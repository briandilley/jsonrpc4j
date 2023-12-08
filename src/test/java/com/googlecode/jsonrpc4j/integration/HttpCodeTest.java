package com.googlecode.jsonrpc4j.integration;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.spring.rest.JsonRpcRestClient;
import com.googlecode.jsonrpc4j.util.BaseRestTest;
import com.googlecode.jsonrpc4j.util.FakeServiceInterface;
import com.googlecode.jsonrpc4j.util.FakeServiceInterfaceImpl;
import com.googlecode.jsonrpc4j.util.JettyServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class HttpCodeTest extends BaseRestTest {

	@Test
	public void http405OnInvalidUrl() throws MalformedURLException {
		FakeServiceInterface service = ProxyUtil.createClientProxy(FakeServiceInterface.class, getClient("error"));
		assertThrows(Exception.class, () -> service.doSomething());
	}

	@Test
	public void http200() throws MalformedURLException {
		FakeServiceInterface service = ProxyUtil.createClientProxy(FakeServiceInterface.class, getClient());
		service.doSomething();
	}

	@Test
	public void httpCustomStatus() throws MalformedURLException {
		RestTemplate restTemplate = new RestTemplate();

		JsonRpcRestClient client = getClient(JettyServer.SERVLET, restTemplate);

		// Overwrite error handler for error check.
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler());

		FakeServiceInterface service = ProxyUtil.createClientProxy(FakeServiceInterface.class, client);
		assertThrows(JsonRpcClientException.class, () -> service.throwSomeException("function error"), "Server Error");
	}

	@Override
	protected Class service() {
		return FakeServiceInterfaceImpl.class;
	}
}
