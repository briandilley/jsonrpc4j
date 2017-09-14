package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.ACCEPT_ENCODING;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.CONTENT_ENCODING;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.JSONRPC_CONTENT_TYPE;

/**
 * A JSON-RPC client that uses the HTTP protocol.
 */
@SuppressWarnings("unused")
public class JsonRpcHttpClient extends JsonRpcClient implements IJsonRpcClient {
	
	private static final String GZIP = "gzip";
	
	private final Map<String, String> headers = new HashMap<>();
	private URL serviceUrl;
	private Proxy connectionProxy = Proxy.NO_PROXY;
	private int connectionTimeoutMillis = 60 * 1000;
	private int readTimeoutMillis = 60 * 1000 * 2;
	private SSLContext sslContext = null;
	private HostnameVerifier hostNameVerifier = null;
	private String contentType = JSONRPC_CONTENT_TYPE;
	private boolean gzipRequests = false;
	
	/**
	 * Creates the {@link JsonRpcHttpClient} bound to the given {@code serviceUrl}.
	 * The headers provided in the {@code headers} map are added to every request
	 * made to the {@code serviceUrl}.
	 *
	 * @param serviceUrl the service end-point URL
	 * @param headers    the headers
	 */
	public JsonRpcHttpClient(URL serviceUrl, Map<String, String> headers) {
		this(new ObjectMapper(), serviceUrl, headers);
	}
	
	/**
	 * Creates the {@link JsonRpcHttpClient} bound to the given {@code serviceUrl}.
	 * The headers provided in the {@code headers} map are added to every request
	 * made to the {@code serviceUrl}.
	 *
	 * @param mapper     the {@link ObjectMapper} to use for json&lt;-&gt;java conversion
	 * @param serviceUrl the service end-point URL
	 * @param headers    the headers
	 */
	public JsonRpcHttpClient(ObjectMapper mapper, URL serviceUrl, Map<String, String> headers) {
		this(mapper, serviceUrl, headers, false, false);
	}
	
	/**
	 * Creates the {@link JsonRpcHttpClient} bound to the given {@code serviceUrl}.
	 * The headers provided in the {@code headers} map are added to every request
	 * made to the {@code serviceUrl}.
	 *
	 * @param mapper              the {@link ObjectMapper} to use for json&lt;-&gt;java conversion
	 * @param serviceUrl          the service end-point URL
	 * @param headers             the headers
	 * @param gzipRequests        whether gzip the request
	 * @param acceptGzipResponses whether accept gzip response
	 */
	public JsonRpcHttpClient(ObjectMapper mapper, URL serviceUrl, Map<String, String> headers, boolean gzipRequests, boolean acceptGzipResponses) {
		super(mapper);
		this.serviceUrl = serviceUrl;
		this.headers.putAll(headers);
		this.gzipRequests = gzipRequests;
		if (acceptGzipResponses) {
			this.headers.put(ACCEPT_ENCODING, GZIP);
		}
	}
	
	/**
	 * Creates the {@link JsonRpcHttpClient} bound to the given {@code serviceUrl}.
	 * The headers provided in the {@code headers} map are added to every request
	 * made to the {@code serviceUrl}.
	 *
	 * @param serviceUrl the service end-point URL
	 */
	public JsonRpcHttpClient(URL serviceUrl) {
		this(new ObjectMapper(), serviceUrl, new HashMap<String, String>());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void invoke(String methodName, Object argument) throws Throwable {
		invoke(methodName, argument, null, new HashMap<String, String>());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object invoke(String methodName, Object argument, Type returnType) throws Throwable {
		return invoke(methodName, argument, returnType, new HashMap<String, String>());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object invoke(String methodName, Object argument, Type returnType, Map<String, String> extraHeaders) throws Throwable {
		HttpURLConnection connection = prepareConnection(extraHeaders);
		try {
			if (this.gzipRequests) {
				connection.setRequestProperty(CONTENT_ENCODING, GZIP);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
					super.invoke(methodName, argument, gos);
				}
				connection.setFixedLengthStreamingMode(baos.size());
				connection.connect();
				connection.getOutputStream().write(baos.toByteArray());
			} else {
				connection.connect();
				try (OutputStream send = connection.getOutputStream()) {
					super.invoke(methodName, argument, send);
				}
			}
			
			final boolean useGzip = useGzip(connection);
			// read and return value
			try {
				try (InputStream answer = getStream(connection.getInputStream(), useGzip)) {
					return super.readResponse(returnType, answer);
				}
			} catch (JsonMappingException e) {
				// JsonMappingException inherits from IOException
				throw e;
			} catch (IOException e) {
				if (connection.getErrorStream() == null) {
					throw new HttpException("Caught error with no response body.", e);
				}

				try (InputStream answer = getStream(connection.getErrorStream(), useGzip)) {
					return super.readResponse(returnType, answer);
				} catch (IOException ef) {
					throw new HttpException(readErrorString(connection), ef);
				}
			}
		} finally {
			connection.disconnect();
		}
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T invoke(String methodName, Object argument, Class<T> clazz) throws Throwable {
		return (T) invoke(methodName, argument, Type.class.cast(clazz));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T invoke(String methodName, Object argument, Class<T> clazz, Map<String, String> extraHeaders) throws Throwable {
		return (T) invoke(methodName, argument, Type.class.cast(clazz), extraHeaders);
	}
	
	/**
	 * Prepares a connection to the server.
	 *
	 * @param extraHeaders extra headers to add to the request
	 * @return the unopened connection
	 * @throws IOException
	 */
	private HttpURLConnection prepareConnection(Map<String, String> extraHeaders) throws IOException {
		
		// create URLConnection
		HttpURLConnection connection = (HttpURLConnection) serviceUrl.openConnection(connectionProxy);
		connection.setConnectTimeout(connectionTimeoutMillis);
		connection.setReadTimeout(readTimeoutMillis);
		connection.setAllowUserInteraction(false);
		connection.setDefaultUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setUseCaches(false);
		connection.setInstanceFollowRedirects(true);
		connection.setRequestMethod("POST");
		
		setupSsl(connection);
		addHeaders(extraHeaders, connection);
		
		return connection;
	}
	
	private boolean useGzip(final HttpURLConnection connection) {
		String contentEncoding = connection.getHeaderField(CONTENT_ENCODING);
		return contentEncoding != null && contentEncoding.equalsIgnoreCase(GZIP);
	}
	
	private InputStream getStream(final InputStream inputStream, final boolean useGzip) throws IOException {
		return useGzip ? new GZIPInputStream(inputStream) : inputStream;
	}
	
	private static String readErrorString(final HttpURLConnection connection) {
		try (InputStream stream = connection.getErrorStream()) {
			StringBuilder buffer = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
				for (int ch = reader.read(); ch >= 0; ch = reader.read()) {
					buffer.append((char) ch);
				}
			}
			return buffer.toString();
		} catch (IOException e) {
			return e.getMessage();
		}
	}
	
	private void setupSsl(HttpURLConnection connection) {
		if (HttpsURLConnection.class.isInstance(connection)) {
			HttpsURLConnection https = HttpsURLConnection.class.cast(connection);
			if (hostNameVerifier != null) {
				https.setHostnameVerifier(hostNameVerifier);
			}
			if (sslContext != null) {
				https.setSSLSocketFactory(sslContext.getSocketFactory());
			}
		}
	}
	
	private void addHeaders(Map<String, String> extraHeaders, HttpURLConnection connection) {
		connection.setRequestProperty("Content-Type", contentType);
		for (Entry<String, String> entry : headers.entrySet()) {
			connection.setRequestProperty(entry.getKey(), entry.getValue());
		}
		for (Entry<String, String> entry : extraHeaders.entrySet()) {
			connection.setRequestProperty(entry.getKey(), entry.getValue());
		}
	}
	
	/**
	 * @return the serviceUrl
	 */
	public URL getServiceUrl() {
		return serviceUrl;
	}
	
	/**
	 * @param serviceUrl the serviceUrl to set
	 */
	public void setServiceUrl(URL serviceUrl) {
		this.serviceUrl = serviceUrl;
	}
	
	/**
	 * @return the connectionProxy
	 */
	public Proxy getConnectionProxy() {
		return connectionProxy;
	}
	
	/**
	 * @param connectionProxy the connectionProxy to set
	 */
	public void setConnectionProxy(Proxy connectionProxy) {
		this.connectionProxy = connectionProxy;
	}
	
	/**
	 * @return the connectionTimeoutMillis
	 */
	public int getConnectionTimeoutMillis() {
		return connectionTimeoutMillis;
	}
	
	/**
	 * @param connectionTimeoutMillis the connectionTimeoutMillis to set
	 */
	public void setConnectionTimeoutMillis(int connectionTimeoutMillis) {
		this.connectionTimeoutMillis = connectionTimeoutMillis;
	}
	
	/**
	 * @return the readTimeoutMillis
	 */
	public int getReadTimeoutMillis() {
		return readTimeoutMillis;
	}
	
	/**
	 * @param readTimeoutMillis the readTimeoutMillis to set
	 */
	public void setReadTimeoutMillis(int readTimeoutMillis) {
		this.readTimeoutMillis = readTimeoutMillis;
	}
	
	/**
	 * @return the headers
	 */
	public Map<String, String> getHeaders() {
		return Collections.unmodifiableMap(headers);
	}
	
	/**
	 * @param headers the headers to set
	 */
	public void setHeaders(Map<String, String> headers) {
		this.headers.clear();
		this.headers.putAll(headers);
	}
	
	/**
	 * @param sslContext the sslContext to set
	 */
	public void setSslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}
	
	/**
	 * @param hostNameVerifier the hostNameVerifier to set
	 */
	public void setHostNameVerifier(HostnameVerifier hostNameVerifier) {
		this.hostNameVerifier = hostNameVerifier;
	}
	
	/**
	 * @param contentType the contentType to set
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
}
