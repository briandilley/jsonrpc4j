package com.googlecode.jsonrpc4j.spring.rest;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Implementation of {@link org.springframework.http.client.ClientHttpRequestFactory} that creates HTTPS connection
 * with specified settings.
 */
class SslClientHttpRequestFactory
		extends SimpleClientHttpRequestFactory {

	private SSLContext sslContext;
	private HostnameVerifier hostNameVerifier;

	@Override
	protected void prepareConnection(HttpURLConnection connection, String httpMethod)
			throws IOException {

		if (connection instanceof HttpsURLConnection) {
			final HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;

			if (hostNameVerifier != null) {
				httpsConnection.setHostnameVerifier(hostNameVerifier);
			}

			if (sslContext != null) {
				httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
			}
		}

		super.prepareConnection(connection, httpMethod);
	}

	public void setSslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public void setHostNameVerifier(HostnameVerifier hostNameVerifier) {
		this.hostNameVerifier = hostNameVerifier;
	}

}
