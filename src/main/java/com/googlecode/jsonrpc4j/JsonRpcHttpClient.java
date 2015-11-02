/*
The MIT License (MIT)

Copyright (c) 2014 jsonrpc4j

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package com.googlecode.jsonrpc4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
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
	extends JsonRpcClient
	implements IJsonRpcClient {

	public static final String JSONRPC_CONTENT_TYPE="application/json-rpc";

	private URL serviceUrl;

	private Proxy connectionProxy 				= Proxy.NO_PROXY;
	private int connectionTimeoutMillis			= 60 * 1000;
	private int readTimeoutMillis				= 60 * 1000 * 2;
	private SSLContext sslContext				= null;
	private HostnameVerifier hostNameVerifier	= null;
	private final Map<String, String> headers	= new HashMap<String, String>();

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
	 * {@inheritDoc}
	 */
	@Override
	public void invoke(String methodName, Object argument)
		throws Throwable {
		invoke(methodName, argument, null, new HashMap<String, String>());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object invoke(
		String methodName, Object argument, Type returnType)
		throws Throwable {
		return invoke(methodName, argument, returnType, new HashMap<String, String>());
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T invoke(
		String methodName, Object argument, Class<T> clazz)
		throws Throwable {
		return (T)invoke(methodName, argument, Type.class.cast(clazz));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
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
		try {
			InputStream ips = con.getInputStream();
			try {
				// in case of http error try to read response body and return it in exception
				return super.readResponse(returnType, ips);
			} finally {
				ips.close();
			}
		} catch (IOException e) {
			throw new HttpException(readString(con.getErrorStream()), e);
		}
	}

	private static String readString(InputStream stream) {
		if (stream == null) return "null";
		try {
			StringBuilder buf = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
			for (int ch = reader.read(); ch >= 0; ch = reader.read()) {
				buf.append((char) ch);
			}
			return buf.toString();
		} catch (UnsupportedEncodingException e) {
			return e.getMessage();
		} catch (IOException e) {
			return e.getMessage();
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
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
		con.setRequestProperty("Content-Type", JSONRPC_CONTENT_TYPE);
		for (Entry<String, String> entry : headers.entrySet()) {
			con.setRequestProperty(entry.getKey(), entry.getValue());
		}
		for (Entry<String, String> entry : extraHeaders.entrySet()) {
			con.setRequestProperty(entry.getKey(), entry.getValue());
		}

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