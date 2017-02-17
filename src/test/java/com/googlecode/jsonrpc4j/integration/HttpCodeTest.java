package com.googlecode.jsonrpc4j.integration;

import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.util.BaseRestTest;
import com.googlecode.jsonrpc4j.util.FakeServiceInterface;
import com.googlecode.jsonrpc4j.util.FakeServiceInterfaceImpl;
import org.junit.Test;

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
	
	@Override
	protected Class service() {
		return FakeServiceInterfaceImpl.class;
	}
}
