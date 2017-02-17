package com.googlecode.jsonrpc4j.integration;

import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.util.BaseRestTest;
import com.googlecode.jsonrpc4j.util.FakeServiceInterface;
import com.googlecode.jsonrpc4j.util.FakeServiceInterfaceImpl;
import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;

/**
 * HttpClientTest
 */
public class HttpClientTest extends BaseRestTest {
	
	private FakeServiceInterface service;
	
	@Test
	public void testGZIPRequest() throws MalformedURLException {
		service = ProxyUtil.createClientProxy(this.getClass().getClassLoader(), FakeServiceInterface.class, getHttpClient(true, false));
		int i = service.returnPrimitiveInt(2);
		Assert.assertEquals(2, i);
	}
	
	@Test
	public void testGZIPRequestAndResponse() throws MalformedURLException {
		service = ProxyUtil.createClientProxy(this.getClass().getClassLoader(), FakeServiceInterface.class, getHttpClient(true, true));
		int i = service.returnPrimitiveInt(2);
		Assert.assertEquals(2, i);
	}
	
	@Test
	public void testRequestAndResponse() throws MalformedURLException {
		service = ProxyUtil.createClientProxy(this.getClass().getClassLoader(), FakeServiceInterface.class, getHttpClient(false, false));
		int i = service.returnPrimitiveInt(2);
		Assert.assertEquals(2, i);
	}
	
	@Override
	protected Class service() {
		return FakeServiceInterfaceImpl.class;
	}
}
