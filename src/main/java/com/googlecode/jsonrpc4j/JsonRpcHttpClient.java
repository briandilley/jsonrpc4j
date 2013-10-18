package com.googlecode.jsonrpc4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A JSON-RPC client that uses the HTTP protocol.
 *
 */
public class JsonRpcHttpClient
	extends JsonRpcClient {

	private URL serviceUrl;

	private Proxy connectionProxy 				= Proxy.NO_PROXY;
	private int connectionTimeoutMillis		= 60 * 1000;
	private int readTimeoutMillis				= 60 * 1000 * 2;
	private SSLContext sslContext 				= null;
	private HostnameVerifier hostNameVerifier 	= null;
	private Map<String, String> headers			= new HashMap<String, String>();

	/**
	 * Creates the {@link JsonRpcHttpClient} bound to the given {@code serviceUrl}.
	 * The headers provided in the {@code headers} map are added to every request
	 * made to the {@code serviceUrl}.
	 * 
	 * @param mapper the {@link ObjectMapper} to use for json<->java conversion
	 * @param serviceUrl the service end-point URL
	 * @param headers the headers
	 */
	public JsonRpcHttpClient(ObjectMapper mapper, URL serviceUrl, Map<String, String> headers) {
		super(mapper);
		this.serviceUrl = serviceUrl;
		this.headers.putAll(headers);
	}

	/**
	 * Creates the {@link JsonRpcHttpClient} bound to the given {@code serviceUrl}.
	 * The headers provided in the {@code headers} map are added to every request
	 * made to the {@code serviceUrl}.
	 * 
	 * @param serviceUrl the service end-point URL
	 * @param headers the headers
	 */
	public JsonRpcHttpClient(URL serviceUrl, Map<String, String> headers) {
		this(new ObjectMapper(), serviceUrl, headers);
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
	 * Invokes the given method with the given argument.
	 * 
	 * @see JsonRpcClient#writeRequest(String, Object, java.io.OutputStream, String)
	 * @param methodName the name of the method to invoke
	 * @param arguments the arguments to the method
	 * @throws Throwable on error
	 */
	public void invoke(String methodName, Object argument)
		throws Throwable {
		invoke(methodName, argument, null, new HashMap<String, String>());
	}

	/**
	 * Invokes the given method with the given arguments and returns
	 * an object of the given type, or null if void.
	 * 
	 * @see JsonRpcClient#writeRequest(String, Object, java.io.OutputStream, String)
	 * @param methodName the name of the method to invoke
	 * @param argument the arguments to the method
	 * @param returnType the return type
	 * @return the return value
	 * @throws Throwable on error
	 */
	public Object invoke(
		String methodName, Object argument, Type returnType)
		throws Throwable {
		return invoke(methodName, argument, returnType, new HashMap<String, String>());
	}

	/**
	 * Invokes the given method with the given arguments and returns
	 * an object of the given type, or null if void.
	 * 
	 * @see JsonRpcClient#writeRequest(String, Object, java.io.OutputStream, String)
	 * @param methodName the name of the method to invoke
	 * @param argument the arguments to the method
	 * @param returnType the return type
	 * @return the return value
	 * @throws Throwable on error
	 */
	@SuppressWarnings("unchecked")
	public <T> T invoke(
		String methodName, Object argument, Class<T> clazz)
		throws Throwable {
		return (T)invoke(methodName, argument, Type.class.cast(clazz));
	}

	/**
	 * Invokes the given method with the given arguments and returns
	 * an object of the given type, or null if void.
	 * 
	 * @see JsonRpcClient#writeRequest(String, Object, java.io.OutputStream, String)
	 * @param methodName the name of the method to invoke
	 * @param arguments the arguments to the method
	 * @param returnType the return type
	 * @param extraHeaders extra headers to add to the request
	 * @return the return value
	 * @throws Throwable on error
	 */
	public Object invoke(
		String methodName, Object argument, Type returnType,
		Map<String, String> extraHeaders)
		throws Throwable {

		// create URLConnection
		HttpURLConnection con = prepareConnection(extraHeaders);
		con.connect();

		// invoke it
		OutputStream ops = con.getOutputStream();
		try {
			super.invoke(methodName, argument, ops);
		} finally {
			ops.close();
		}

		// read and return value
		InputStream ips = con.getInputStream();
		try {
			return super.readResponse(returnType, ips);
		} finally {
			ips.close();
		}
	}

	/**
	 * Invokes the given method with the given arguments and returns
	 * an object of the given type, or null if void.
	 * 
	 * @see JsonRpcClient#writeRequest(String, Object, java.io.OutputStream, String)
	 * @param methodName the name of the method to invoke
	 * @param arguments the arguments to the method
	 * @param returnType the return type
	 * @param extraHeaders extra headers to add to the request
	 * @return the return value
	 * @throws Throwable on error
	 */
	@SuppressWarnings("unchecked")
	public <T> T invoke(
		String methodName, Object argument, Class<T> clazz,
		Map<String, String> extraHeaders)
		throws Throwable {
		return (T)invoke(methodName, argument, Type.class.cast(clazz), extraHeaders);
	}

	/**
	 * Prepares a connection to the server.
	 * @param extraHeaders extra headers to add to the request
	 * @return the unopened connection
	 * @throws IOException 
	 */
	protected HttpURLConnection prepareConnection(Map<String, String> extraHeaders)
		throws IOException {
		
		// create URLConnection
		HttpURLConnection con = (HttpURLConnection)serviceUrl.openConnection(connectionProxy);
		con.setConnectTimeout(connectionTimeoutMillis);
		con.setReadTimeout(readTimeoutMillis);
		con.setAllowUserInteraction(false);
		con.setDefaultUseCaches(false);
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setUseCaches(false);
		con.setInstanceFollowRedirects(true);
		con.setRequestMethod("POST");

		// do stuff for ssl
		if (HttpsURLConnection.class.isInstance(con)) {
			HttpsURLConnection https = HttpsURLConnection.class.cast(con);
			if (hostNameVerifier != null) {
				https.setHostnameVerifier(hostNameVerifier);
			}
			if (sslContext != null) {
				https.setSSLSocketFactory(sslContext.getSocketFactory());
			}
		}

		// add headers
		for (Entry<String, String> entry : headers.entrySet()) {
			con.setRequestProperty(entry.getKey(), entry.getValue());
		}
		for (Entry<String, String> entry : extraHeaders.entrySet()) {
			con.setRequestProperty(entry.getKey(), entry.getValue());
		}
		con.setRequestProperty("Content-Type", "application/json-rpc");

		// return it
		return con;
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

}
