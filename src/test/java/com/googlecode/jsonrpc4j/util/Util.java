package com.googlecode.jsonrpc4j.util;

import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcServer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Util {
	public static final String param1 = "param1";
	public static final String param2 = "param2";
	public static final String param3 = "param3";
	public static final String param4 = "param4";
	public static final int intParam1 = 1;
	public static final int intParam2 = 2;
	public static final String JSON_ENCODING = StandardCharsets.UTF_8.name();
	public static final ObjectMapper mapper = new ObjectMapper();
	private static final String invalidJson = "{\"jsonrpc\": \"2.0,\n" +
			" \"method\": \"testMethod\",\n" +
			" \"params\": {},\n" +
			" \"id\": \n" +
			" }\n" +
			" ";

	public static InputStream invalidJsonStream() {
		return new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));
	}

	public static InputStream messageWithListParams(final Object id, final String methodName, final Object... args) throws JsonProcessingException {
		return messageOf(id, methodName, Arrays.asList(args));
	}

	public static InputStream messageOf(final Object id, final String methodName, final Object params) throws JsonProcessingException {
		String data = mapper.writeValueAsString(new HashMap<String, Object>() {
			{
				if (id != null) put(JsonRpcBasicServer.ID, id);
				put(JsonRpcBasicServer.JSONRPC, JsonRpcServer.VERSION);
				if (methodName != null) put(JsonRpcBasicServer.METHOD, methodName);
				if (params != null) put(JsonRpcBasicServer.PARAMS, params);
			}
		});
		return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
	}

	public static InputStream messageWithMapParams(final String methodName, final Object... args) throws JsonProcessingException {
		Map<String, Object> elements = new HashMap<>();
		for (int i = 0; i < args.length; i += 2) {
			final String key = (String) args[i];
			final Object value = args[i + 1];
			elements.put(key, value);

		}
		return messageOf(1, methodName, elements);
	}

	public static JsonNode decodeAnswer(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
		return mapper.readTree(byteArrayOutputStream.toString(JSON_ENCODING));
	}
}
