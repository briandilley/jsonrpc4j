package com.googlecode.jsonrpc4j.integration;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.util.BaseRestTest;
import com.googlecode.jsonrpc4j.util.FakeServiceInterface;
import com.googlecode.jsonrpc4j.util.FakeServiceInterfaceImpl;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * HttpClientTest
 */
public class HttpClientTest extends BaseRestTest {
	
	private FakeServiceInterface service;
	
	@Test
	public void testRequestAndResponse() throws MalformedURLException {
		service = ProxyUtil.createClientProxy(this.getClass().getClassLoader(), FakeServiceInterface.class, getHttpClient(false, false));
		int i = service.returnPrimitiveInt(2);
		assertEquals(2, i);
	}

	@Test
	public void testCustomException() throws Exception {
		service = ProxyUtil.createClientProxy(this.getClass().getClassLoader(), FakeServiceInterface.class, getHttpClient(false, false));
		assertThrows(JsonRpcClientException.class, () -> service.throwSomeException("Custom exception"), "Custom exception");
	}

	@Test
	public void testHttpError() throws Exception {
		service = ProxyUtil.createClientProxy(this.getClass().getClassLoader(), FakeServiceInterface.class, getHttpClient("error", false, false));
		assertThrows(Exception.class, () -> service.doSomething());
	}

	@Override
	protected Class service() {
		return FakeServiceInterfaceImpl.class;
	}
}
