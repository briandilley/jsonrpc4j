package com.googlecode.jsonrpc4j.client;

import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.PARAMS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.googlecode.jsonrpc4j.JsonRpcClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JsonRpcClientTest {

	private ByteArrayOutputStream byteArrayOutputStream;
	private JsonRpcClient client;

	@Before
	public void setUp() {
		client = new JsonRpcClient();
		byteArrayOutputStream = new ByteArrayOutputStream();
	}

	@After
	public void tearDown() {
		client = null;
	}

	@Test
	public void testInvokeNoParams()
			throws Throwable {

		client.invoke("test", new Object[0], byteArrayOutputStream);
		JsonNode node = readJSON(byteArrayOutputStream);
		assertFalse(node.has(PARAMS));

		client.invoke("test", null, byteArrayOutputStream);
		node = readJSON(byteArrayOutputStream);
		assertFalse(node.has(PARAMS));
	}

	private JsonNode readJSON(ByteArrayOutputStream byteArrayOutputStream)
			throws IOException {
		return client.getObjectMapper().readTree(byteArrayOutputStream.toString());
	}

	@Test
	public void testInvokeArrayParams()
			throws Throwable {
		client.invoke("test", new Object[] { 1, 2 }, byteArrayOutputStream);
		JsonNode node = readJSON(byteArrayOutputStream);

		assertTrue(node.has(PARAMS));
		assertTrue(node.get(PARAMS).isArray());
		assertEquals(1, node.get(PARAMS).get(0).intValue());
		assertEquals(2, node.get(PARAMS).get(1).intValue());
	}

	@Test
	public void testInvokeHashParams()
			throws Throwable {
		Map<String, Object> params = new HashMap<>();
		params.put("hello", "test");
		params.put("x", 1);
		client.invoke("test", params, byteArrayOutputStream);
		JsonNode node = readJSON(byteArrayOutputStream);

		assertTrue(node.has(PARAMS));
		assertTrue(node.get(PARAMS).isObject());
		assertEquals("test", node.get(PARAMS).get("hello").textValue());
		assertEquals(1, node.get(PARAMS).get("x").intValue());
	}

}
