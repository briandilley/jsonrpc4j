package com.googlecode.jsonrpc4j.integration;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.spring.rest.JsonRpcRestClient;
import com.googlecode.jsonrpc4j.util.BaseRestTest;
import com.googlecode.jsonrpc4j.util.FakeServiceInterface;
import com.googlecode.jsonrpc4j.util.FakeServiceInterfaceImpl;
import com.googlecode.jsonrpc4j.util.JettyServer;
import org.junit.Test;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;

public class HttpCodeTest extends BaseRestTest {
	
	@Test
	public void http405OnInvalidUrl() throws MalformedURLException {
		expectedEx.expectMessage(anyOf(
				equalTo("405 HTTP method POST is not supported by this URL"),
				equalTo("404 Not Found"),
				equalTo("HTTP method POST is not supported by this URL"),
				startsWith("Server returned HTTP response code: 405 for URL: http://127.0.0.1")));
		expectedEx.expect(Exception.class);
		FakeServiceInterface service = ProxyUtil.createClientProxy(FakeServiceInterface.class, getClient("error"));
		service.doSomething();
	}
	
	@Test
	public void http200() throws MalformedURLException {
		FakeServiceInterface service = ProxyUtil.createClientProxy(FakeServiceInterface.class, getClient());
		service.doSomething();
	}
	
	@Test
	public void httpCustomStatus() throws MalformedURLException {
		expectedEx.expectMessage(equalTo("Server Error"));
		expectedEx.expect(JsonRpcClientException.class);

		RestTemplate restTemplate = new RestTemplate();

		JsonRpcRestClient client = getClient(JettyServer.SERVLET, restTemplate);

		// Overwrite error handler for error check.
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler());

		FakeServiceInterface service = ProxyUtil.createClientProxy(FakeServiceInterface.class, client);
		service.throwSomeException("function error");
	}

	@Override
	protected Class service() {
		return FakeServiceInterfaceImpl.class;
	}
}
