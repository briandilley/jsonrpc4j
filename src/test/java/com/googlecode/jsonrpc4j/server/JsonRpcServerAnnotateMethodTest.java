package com.googlecode.jsonrpc4j.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import org.easymock.EasyMock;
import org.easymock.EasyMockExtension;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.RESULT;
import static com.googlecode.jsonrpc4j.util.Util.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(EasyMockExtension.class)
public class JsonRpcServerAnnotateMethodTest {
	@Mock(type = MockType.NICE)
	private ServiceInterfaceWithCustomMethodNameAnnotation mockService;
	
	private ByteArrayOutputStream byteArrayOutputStream;
	private JsonRpcBasicServer jsonRpcServerAnnotatedMethod;

	@BeforeEach
	public void setup() {
		byteArrayOutputStream = new ByteArrayOutputStream();
		jsonRpcServerAnnotatedMethod = new JsonRpcBasicServer(mapper, mockService, ServiceInterfaceWithCustomMethodNameAnnotation.class);
	}
	
	@Test
	public void callMethodWithCustomMethodNameTest() throws Exception {
		EasyMock.expect(mockService.customMethod()).andReturn(param1);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedMethod.handleRequest(messageWithMapParamsStream("Test.custom"), byteArrayOutputStream);
		assertEquals(param1, result().textValue());
	}
	
	private JsonNode result() throws IOException {
		return decodeAnswer(byteArrayOutputStream).get(RESULT);
	}
	
	@Test
	public void callMethodWithoutCustomMethodNameTest() throws Exception {
		EasyMock.expect(mockService.customMethod()).andReturn(param1);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedMethod.handleRequest(messageWithListParamsStream(1, "customMethod"), byteArrayOutputStream);
		assertEquals(param1, result().textValue());
	}
	
	@Test
	public void callMethodWithCustomMethodNameAndParamTest() throws Exception {
		EasyMock.expect(mockService.customMethod2(param1)).andReturn(param2);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedMethod.handleRequest(messageWithListParamsStream(1, "Test.custom2", param1), byteArrayOutputStream);
		assertEquals(param2, result().textValue());
	}
	
	@Test
	public void callMethodWithoutCustomMethodNameAndParamTest() throws Exception {
		EasyMock.expect(mockService.customMethod2(param1)).andReturn(param2);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedMethod.handleRequest(messageWithListParamsStream(1, "customMethod2", param1), byteArrayOutputStream);
		assertEquals(param2, result().textValue());
	}
	
	public interface ServiceInterfaceWithCustomMethodNameAnnotation {
		@JsonRpcMethod("Test.custom")
		String customMethod();
		
		@JsonRpcMethod("Test.custom2")
		String customMethod2(String stringParam1);
	}
	
}
