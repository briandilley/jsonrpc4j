package com.googlecode.jsonrpc4j.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcServer;

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
	public static final String nonAsciiCharacters = "PñA&s<>k ;";
	public static final String param2 = "param2";
	public static final String param3 = "param3";
	public static final String param4 = "param4";
	public static final int intParam1 = 1;
	public static final int intParam2 = 2;
	public static final String JSON_ENCODING = StandardCharsets.UTF_8.name();
	public static final ObjectMapper mapper = new ObjectMapper();
	@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
	public static final String DEFAULT_LOCAL_HOSTNAME = "127.0.0.1";
	private static final String invalidJsonRpcRequest = "{\"method\": \"subtract\", \"params\": [], \"id\": 1}";
	private static final String invalidJson = "{\"jsonrpc\": \"2.0,\n" +
			" \"method\": \"testMethod\",\n" +
			" \"params\": {},\n" +
			" \"id\": \n" +
			" }\n" +
			" ";
	
	public static InputStream invalidJsonRpcRequestStream() {
		return new ByteArrayInputStream(invalidJsonRpcRequest.getBytes(StandardCharsets.UTF_8));
	}
	
	public static InputStream invalidJsonStream() {
		return new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));
	}
	
	public static InputStream messageWithListParamsStream(final Object id, final String methodName, final Object... args) throws JsonProcessingException {
		return createStream(messageWithListParams(id, methodName, args));
	}
	
	public static InputStream createStream(Object content) throws JsonProcessingException {
		String data = mapper.writeValueAsString(content);
		return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
	}
	
	public static HashMap<String, Object> messageWithListParams(final Object id, final String methodName, final Object... args) throws JsonProcessingException {
		return messageOfStream(id, methodName, Arrays.asList(args));
	}
	
	public static HashMap<String, Object> messageOfStream(final Object id, final String methodName, final Object params) {
		return makeJsonRpcRequestObject(id, methodName, params);
	}
	
	@SuppressWarnings("serial")
	private static HashMap<String, Object> makeJsonRpcRequestObject(final Object id, final String methodName, final Object params) {
		return new HashMap<String, Object>() {
			{
				if (id != null) put(JsonRpcBasicServer.ID, id);
				put(JsonRpcBasicServer.JSONRPC, JsonRpcServer.VERSION);
				if (methodName != null) put(JsonRpcBasicServer.METHOD, methodName);
				if (params != null) put(JsonRpcBasicServer.PARAMS, params);
			}
		};
	}
	
	public static InputStream multiMessageOfStream(Object... args) throws JsonProcessingException {
		return createStream(args);
	}
	
	public static InputStream messageWithMapParamsStream(final String methodName, final Object... args) throws JsonProcessingException {
		return createStream(messageWithMapParams(methodName, args));
	}
	
	private static HashMap<String, Object> messageWithMapParams(final String methodName, final Object... args) throws JsonProcessingException {
		Map<String, Object> elements = new HashMap<>();
		for (int i = 0; i < args.length; i += 2) {
			final String key = (String) args[i];
			final Object value = args[i + 1];
			elements.put(key, value);
		}
		return messageOfStream(1, methodName, elements);
	}
	
	public static ByteArrayOutputStream toByteArrayOutputStream(byte[] data) throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream(data.length);
		result.write(data);
		return result;
	}
	
	public static ByteArrayOutputStream toByteArrayOutputStream(InputStream inputStream) throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		byte[] buffer = new byte[256];
		int read;
		
		while (-1 != (read = inputStream.read(buffer))) {
			result.write(buffer, 0, read);
		}
		
		return result;
	}
	
	public static JsonNode error(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
		return decodeAnswer(byteArrayOutputStream).get(JsonRpcBasicServer.ERROR);
	}
	
	public static JsonNode decodeAnswer(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
		return mapper.readTree(byteArrayOutputStream.toString(JSON_ENCODING));
	}
	
	public static JsonNode errorCode(JsonNode error) {
		return error.get(JsonRpcBasicServer.ERROR_CODE);
	}
	
	public static JsonNode errorMessage(JsonNode error) {
		return error.get(JsonRpcBasicServer.ERROR_MESSAGE);
	}
	
	public static JsonNode errorData(JsonNode error) {
		return error.get(JsonRpcBasicServer.DATA);
	}
	
	public static JsonNode exceptionType(JsonNode error) {
		return error.get(JsonRpcBasicServer.EXCEPTION_TYPE_NAME);
	}
	
	
	public static JsonNode getFromArrayWithId(final JsonNode node, final int id) {
		for (JsonNode n : node) {
			if (n.get(JsonRpcBasicServer.ID).asInt() == id) {
				return n;
			}
		}
		throw new IllegalStateException("could not find in " + node + " id " + id);
	}
	
	/**
	 * Simple input stream to byte array converter.
	 *
	 * @param inputStream the input stream that will be converted.
	 * @return the content of the input stream in form of a byte array.
	 * @throws IOException thrown if there was an IO error while converting data.
	 */
	public static byte[] convertInputStreamToByteArray(InputStream inputStream) throws IOException {
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		int read;
		byte[] data = new byte[512];
		while ((read = inputStream.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, read);
		}
		
		buffer.flush();
		return buffer.toByteArray();
	}
}
