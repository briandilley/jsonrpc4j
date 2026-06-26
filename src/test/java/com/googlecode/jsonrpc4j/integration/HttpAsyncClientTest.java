package com.googlecode.jsonrpc4j.integration;

import com.googlecode.jsonrpc4j.JsonRpcHttpAsyncClient;
import com.googlecode.jsonrpc4j.util.FakeServiceInterface;
import com.googlecode.jsonrpc4j.util.FakeServiceInterfaceImpl;
import com.googlecode.jsonrpc4j.util.JettyServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HttpAsyncClientTest {

	private JettyServer jettyServer;
	private JsonRpcHttpAsyncClient client;

	@Before
	public void setup() throws Exception {
		jettyServer = new JettyServer(FakeServiceInterfaceImpl.class);
		jettyServer.startup();
		String url = jettyServer.getCustomServerUrlString(JettyServer.SERVLET);
		client = new JsonRpcHttpAsyncClient(new URL(url));
	}

	@After
	public void teardown() throws Exception {
		jettyServer.stop();
	}

	@Test
	public void testInvokeWithFuture() throws Exception {
		Future<Object> future = client.invoke("returnPrimitiveInt", new Object[]{5});
		Object result = future.get(5, TimeUnit.SECONDS);
		assertNotNull(result);
		assertEquals(5, ((Number) result).intValue());
	}

	@Test
	public void testInvokeWithReturnType() throws Exception {
		Future<Integer> future = client.invoke("returnPrimitiveInt", new Object[]{42}, Integer.class);
		Integer result = future.get(5, TimeUnit.SECONDS);
		assertEquals(Integer.valueOf(42), result);
	}

	@Test
	public void testInvokeReturnsCustomObject() throws Exception {
		Future<FakeServiceInterface.CustomClass> future = client.invoke("returnCustomClass", new Object[]{7, "hello"}, FakeServiceInterface.CustomClass.class);
		FakeServiceInterface.CustomClass result = future.get(5, TimeUnit.SECONDS);
		assertNotNull(result);
		assertEquals(7, result.integer);
		assertEquals("hello", result.string);
	}

	@Test
	public void testInvokeVoidMethod() throws Exception {
		Future<Object> future = client.invoke("doSomething", new Object[]{});
		Object result = future.get(5, TimeUnit.SECONDS);
		assertNull(result);
	}

	@Test
	public void testInvokeWithServerError() throws Exception {
		Future<Object> future = client.invoke("throwSomeException", new Object[]{"test error"});
		try {
			future.get(5, TimeUnit.SECONDS);
			fail("Expected ExecutionException");
		} catch (ExecutionException e) {
			assertNotNull("Cause should not be null", e.getCause());
		}
	}
}
