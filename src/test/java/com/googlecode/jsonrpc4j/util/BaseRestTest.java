package com.googlecode.jsonrpc4j.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.spring.rest.JsonRpcRestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseRestTest {
	
	private JettyServer jettyServer;

	@BeforeEach
	public void setup() throws Exception {
		this.jettyServer = createServer();
	}
	
	private JettyServer createServer() throws Exception {
		Class service = service();
		if (service == null) return null;
		JettyServer jettyServer = new JettyServer(service);
		jettyServer.startup();
		return jettyServer;
	}
	
	protected abstract Class service();
	
	protected JsonRpcRestClient getClient() throws MalformedURLException {
		return getClient(JettyServer.SERVLET);
	}
	
	protected JsonRpcRestClient getClient(final String servlet) throws MalformedURLException {
		return new JsonRpcRestClient(new URL(jettyServer.getCustomServerUrlString(servlet)));
	}
	
	protected JsonRpcRestClient getClient(final String servlet, RestTemplate restTemplate) throws MalformedURLException {
		return new JsonRpcRestClient(new URL(jettyServer.getCustomServerUrlString(servlet)), restTemplate);
	}

	protected JsonRpcHttpClient getHttpClient(boolean gzipRequests, boolean acceptGzipResponses) throws MalformedURLException {
		Map<String, String> header = new HashMap<>();
		return new JsonRpcHttpClient(new ObjectMapper(), new URL(jettyServer.getCustomServerUrlString(JettyServer.SERVLET)), header, gzipRequests, acceptGzipResponses);
	}

	protected JsonRpcHttpClient getHttpClient(final String servlet, boolean gzipRequests, boolean acceptGzipResponses) throws MalformedURLException {
		Map<String, String> header = new HashMap<>();
		return new JsonRpcHttpClient(new ObjectMapper(), new URL(jettyServer.getCustomServerUrlString(servlet)), header, gzipRequests, acceptGzipResponses);
	}

	@AfterEach
	public void teardown() throws Exception {
		jettyServer.stop();
	}
	
}
