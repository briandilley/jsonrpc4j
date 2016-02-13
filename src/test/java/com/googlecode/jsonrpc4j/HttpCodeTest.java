package com.googlecode.jsonrpc4j;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.Test;

import org.springframework.web.client.HttpClientErrorException;

import com.googlecode.jsonrpc4j.util.BaseRestTest;
import com.googlecode.jsonrpc4j.util.FakeServiceInterface;
import com.googlecode.jsonrpc4j.util.FakeServiceInterfaceImpl;

import java.net.MalformedURLException;

public class HttpCodeTest extends BaseRestTest {

	@Test
	public void http405OnInvalidUrl() throws MalformedURLException {
		expectedEx.expectMessage(anyOf(equalTo("405 HTTP method POST is not supported by this URL"), equalTo("404 Not Found")));
		expectedEx.expect(HttpClientErrorException.class);
		FakeServiceInterface service = ProxyUtil.createClientProxy(FakeServiceInterface.class, getClient("error"));
		service.doSomething();
	}

	@Test
	public void http200() throws MalformedURLException {
		FakeServiceInterface service = ProxyUtil.createClientProxy(FakeServiceInterface.class, getClient());
		service.doSomething();
	}

	@Override
	protected Class service() {
		return FakeServiceInterfaceImpl.class;
	}
}
