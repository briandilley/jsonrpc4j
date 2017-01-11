package com.googlecode.jsonrpc4j.integration;

import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.util.BaseRestTest;
import com.googlecode.jsonrpc4j.util.FakeServiceInterface;
import com.googlecode.jsonrpc4j.util.FakeServiceInterfaceImpl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimpleTest extends BaseRestTest {
	private FakeServiceInterface service;
	
	@Before
	@Override
	public void setup() throws Exception {
		super.setup();
		service = ProxyUtil.createClientProxy(FakeServiceInterface.class, getClient());
	}
	
	@Override
	protected Class service() {
		return FakeServiceInterfaceImpl.class;
	}
	
	@Test
	public void doSomething() {
		service.doSomething();
	}
	
	@Test
	public void returnPrimitiveInt() {
		final int at = 22;
		assertEquals(at, service.returnPrimitiveInt(at));
	}
	
	@Test
	public void returnCustomClass() {
		final int at = 22;
		final String message = "simple";
		FakeServiceInterface.CustomClass result = service.returnCustomClass(at, message);
		assertEquals(at, result.integer);
		assertEquals(message, result.string);
		assertTrue(result.list.contains(message));
		assertTrue(result.list.contains("" + at));
	}
	
}
