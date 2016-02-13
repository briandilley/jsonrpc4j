package com.googlecode.jsonrpc4j.server;

import static com.googlecode.jsonrpc4j.util.Util.decodeAnswer;
import static com.googlecode.jsonrpc4j.util.Util.mapper;
import static com.googlecode.jsonrpc4j.util.Util.messageWithListParams;
import static com.googlecode.jsonrpc4j.util.Util.messageWithMapParams;
import static com.googlecode.jsonrpc4j.util.Util.param1;
import static com.googlecode.jsonrpc4j.util.Util.param2;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.MockType;

import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcMethod;

import java.io.ByteArrayOutputStream;

@RunWith(EasyMockRunner.class)
public class JsonRpcServerAnnotateMethodTest {
	@Mock(type = MockType.NICE)
	private ServiceInterfaceWithCustomMethodNameAnnotation mockService;

	private ByteArrayOutputStream byteArrayOutputStream;
	private JsonRpcBasicServer jsonRpcServerAnnotatedMethod;

	@Before
	public void setup() {
		byteArrayOutputStream = new ByteArrayOutputStream();
		jsonRpcServerAnnotatedMethod = new JsonRpcBasicServer(mapper, mockService, ServiceInterfaceWithCustomMethodNameAnnotation.class);
	}

	@Test
	public void callMethodWithCustomMethodNameTest() throws Exception {
		EasyMock.expect(mockService.customMethod()).andReturn(param1);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedMethod.handle(messageWithMapParams("Test.custom"), byteArrayOutputStream);
		assertEquals(param1, decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callMethodWithoutCustomMethodNameTest() throws Exception {
		EasyMock.expect(mockService.customMethod()).andReturn(param1);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedMethod.handle(messageWithListParams(1, "customMethod"), byteArrayOutputStream);
		assertEquals(param1, decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callMethodWithCustomMethodNameAndParamTest() throws Exception {
		EasyMock.expect(mockService.customMethod2(param1)).andReturn(param2);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedMethod.handle(messageWithListParams(1, "Test.custom2", param1), byteArrayOutputStream);
		assertEquals(param2, decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callMethodWithoutCustomMethodNameAndParamTest() throws Exception {
		EasyMock.expect(mockService.customMethod2(param1)).andReturn(param2);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedMethod.handle(messageWithListParams(1, "customMethod2", param1), byteArrayOutputStream);
		assertEquals(param2, decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	public interface ServiceInterfaceWithCustomMethodNameAnnotation {
		@JsonRpcMethod("Test.custom")
		String customMethod();

		@JsonRpcMethod("Test.custom2")
		String customMethod2(String stringParam1);
	}

}
