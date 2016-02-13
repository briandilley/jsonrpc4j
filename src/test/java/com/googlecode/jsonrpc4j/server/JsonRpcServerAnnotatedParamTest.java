package com.googlecode.jsonrpc4j.server;

import static com.googlecode.jsonrpc4j.util.Util.decodeAnswer;
import static com.googlecode.jsonrpc4j.util.Util.intParam1;
import static com.googlecode.jsonrpc4j.util.Util.intParam2;
import static com.googlecode.jsonrpc4j.util.Util.mapper;
import static com.googlecode.jsonrpc4j.util.Util.messageWithMapParams;
import static com.googlecode.jsonrpc4j.util.Util.param1;
import static com.googlecode.jsonrpc4j.util.Util.param2;
import static com.googlecode.jsonrpc4j.util.Util.param3;
import static com.googlecode.jsonrpc4j.util.Util.param4;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.MockType;

import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.util.Util;

import java.io.ByteArrayOutputStream;

@RunWith(EasyMockRunner.class)
public class JsonRpcServerAnnotatedParamTest {

	@Mock(type = MockType.NICE)
	private ServiceInterfaceWithParamNameAnnotation mockService;
	private ByteArrayOutputStream byteArrayOutputStream;

	private JsonRpcBasicServer jsonRpcServerAnnotatedParam;

	@Before
	public void setup() {
		byteArrayOutputStream = new ByteArrayOutputStream();
		jsonRpcServerAnnotatedParam = new JsonRpcBasicServer(mapper, mockService, ServiceInterfaceWithParamNameAnnotation.class);
	}

	@Test
	public void callMethodWithTooFewParametersNamed() throws Exception {
		EasyMock.expect(mockService.testMethod(EasyMock.anyObject(String.class))).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handle(messageWithMapParams("testMethod"), byteArrayOutputStream);
		assertEquals(-32602, decodeAnswer(byteArrayOutputStream).get("error").get("code").intValue());
	}

	@Test
	public void callMethodExactNumberOfParametersNamed() throws Exception {
		EasyMock.expect(mockService.testMethod(EasyMock.anyObject(String.class))).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handle(messageWithMapParams("testMethod", param1, param2), byteArrayOutputStream);
		assertEquals("success", decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callMethodWithExtraParameterNamed() throws Exception {
		EasyMock.expect(mockService.testMethod(EasyMock.anyObject(String.class))).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handle(messageWithMapParams("testMethod", param1, param2, param3, intParam1), byteArrayOutputStream);
		assertEquals(-32602, decodeAnswer(byteArrayOutputStream).get("error").get("code").intValue());
	}

	@Test
	public void callMethodWithTooFewParametersNamedAllowOn() throws Exception {
		EasyMock.expect(mockService.testMethod(EasyMock.anyObject(String.class))).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.setAllowExtraParams(true);
		jsonRpcServerAnnotatedParam.handle(messageWithMapParams("testMethod"), byteArrayOutputStream);
		assertEquals(-32602, decodeAnswer(byteArrayOutputStream).get("error").get("code").intValue());
	}

	@Test
	public void callMethodExactNumberOfParametersNamedAllowOn() throws Exception {
		EasyMock.expect(mockService.testMethod(EasyMock.anyObject(String.class))).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.setAllowExtraParams(true);
		jsonRpcServerAnnotatedParam.handle(messageWithMapParams("testMethod", param1, param2), byteArrayOutputStream);
		assertEquals("success", decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callMethodWithExtraParameterNamedAllowOn() throws Exception {
		EasyMock.expect(mockService.testMethod(EasyMock.anyObject(String.class))).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.setAllowExtraParams(true);
		jsonRpcServerAnnotatedParam.handle(messageWithMapParams("testMethod", param1, param2, param3, intParam1), byteArrayOutputStream);
		assertEquals("success", decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callOverloadedMethodNoNamedParams() throws Exception {
		EasyMock.expect(mockService.overloadedMethod()).andReturn(param1);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handle(messageWithMapParams("overloadedMethod"), byteArrayOutputStream);
		assertEquals(param1, decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callOverloadedMethodOneNamedStringParam() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(EasyMock.anyString())).andReturn(param3);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handle(messageWithMapParams("overloadedMethod", param1, param2), byteArrayOutputStream);
		assertEquals(param3, decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callOverloadedMethodOneNamedIntParam() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(intParam1)).andReturn(param2);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handle(messageWithMapParams("overloadedMethod", param1, intParam1), byteArrayOutputStream);
		assertEquals(param2, decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callOverloadedMethodTwoNamedStringParams() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(param3, param4)).andReturn(param1 + param2);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handle(messageWithMapParams("overloadedMethod", param1, param3, param2, param4), byteArrayOutputStream);
		assertEquals(param1 + param2, decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callOverloadedMethodTwoNamedIntParams() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(intParam1, intParam2)).andReturn((intParam1 + intParam2) + "");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handle(messageWithMapParams("overloadedMethod", param1, intParam1, param2, intParam2), byteArrayOutputStream);
		assertEquals((intParam1 + intParam2) + "", decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callOverloadedMethodNamedExtraParams() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(intParam1, intParam2)).andReturn((intParam1 + intParam2) + "");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handle(messageWithMapParams("overloadedMethod", param1, intParam1, param2, intParam2, param3, param4), byteArrayOutputStream);
		assertEquals(-32602, decodeAnswer(byteArrayOutputStream).get("error").get("code").intValue());
	}

	@Test
	public void callOverloadedMethodNamedExtraParamsAllowOn() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(intParam1, intParam2)).andReturn((intParam1 + intParam2) + "");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.setAllowExtraParams(true);
		jsonRpcServerAnnotatedParam.handle(messageWithMapParams("overloadedMethod", param1, intParam1, param2, intParam2, param3, param4), byteArrayOutputStream);
		assertEquals((intParam1 + intParam2) + "", decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callMethodWithoutRequiredParam() throws Exception {
		EasyMock.expect(mockService.methodWithoutRequiredParam(param2, param4)).andReturn(param2 + param4);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handle(messageWithMapParams("methodWithoutRequiredParam", param1, param3), byteArrayOutputStream);
		assertEquals(-32602, decodeAnswer(byteArrayOutputStream).get("error").get("code").intValue());
	}

	@Test
	public void callMethodWithoutRequiredParamAllowOn() throws Exception {
		EasyMock.expect(mockService.methodWithoutRequiredParam(EasyMock.eq(param3), EasyMock.anyString())).andReturn(param3);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.setAllowLessParams(true);
		jsonRpcServerAnnotatedParam.handle(messageWithMapParams("methodWithoutRequiredParam", param1, param3), byteArrayOutputStream);
		assertEquals(param3, decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callParseErrorJson() throws Exception {
		jsonRpcServerAnnotatedParam.handle(Util.invalidJsonStream(), byteArrayOutputStream);
		assertEquals(-32700, decodeAnswer(byteArrayOutputStream).get("error").get("code").asInt());
	}

	public interface ServiceInterfaceWithParamNameAnnotation {
		String testMethod(@JsonRpcParam("param1") String param1);

		String overloadedMethod();

		String overloadedMethod(@JsonRpcParam("param1") String stringParam1);

		String overloadedMethod(@JsonRpcParam("param1") String stringParam1, @JsonRpcParam("param2") String stringParam2);

		String overloadedMethod(@JsonRpcParam("param1") int intParam1);

		String overloadedMethod(@JsonRpcParam("param1") int intParam1, @JsonRpcParam("param2") int intParam2);

		String methodWithoutRequiredParam(@JsonRpcParam("param1") String stringParam1, @JsonRpcParam(value = "param2") String stringParam2);
	}
}
