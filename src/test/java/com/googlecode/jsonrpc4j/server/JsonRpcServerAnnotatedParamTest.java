package com.googlecode.jsonrpc4j.server;

import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.METHOD_PARAMS_INVALID;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.PARSE_ERROR;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.RESULT;
import static com.googlecode.jsonrpc4j.util.Util.decodeAnswer;
import static com.googlecode.jsonrpc4j.util.Util.error;
import static com.googlecode.jsonrpc4j.util.Util.errorCode;
import static com.googlecode.jsonrpc4j.util.Util.intParam1;
import static com.googlecode.jsonrpc4j.util.Util.intParam2;
import static com.googlecode.jsonrpc4j.util.Util.mapper;
import static com.googlecode.jsonrpc4j.util.Util.messageWithMapParamsStream;
import static com.googlecode.jsonrpc4j.util.Util.param1;
import static com.googlecode.jsonrpc4j.util.Util.param2;
import static com.googlecode.jsonrpc4j.util.Util.param3;
import static com.googlecode.jsonrpc4j.util.Util.param4;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.util.Util;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

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
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("testMethod"), byteArrayOutputStream);
		assertEquals(METHOD_PARAMS_INVALID.code, errorCode(error(byteArrayOutputStream)).intValue());
	}
	
	@Test
	public void callMethodExactNumberOfParametersNamed() throws Exception {
		EasyMock.expect(mockService.testMethod(EasyMock.anyObject(String.class))).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("testMethod", param1, param2), byteArrayOutputStream);
		assertEquals("success", result().textValue());
	}
	
	private JsonNode result() throws IOException {
		return decodeAnswer(byteArrayOutputStream).get(RESULT);
	}
	
	@Test
	public void callMethodWithExtraParameterNamed() throws Exception {
		EasyMock.expect(mockService.testMethod(EasyMock.anyObject(String.class))).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("testMethod", param1, param2, param3, intParam1), byteArrayOutputStream);
		assertEquals(METHOD_PARAMS_INVALID.code, errorCode(error(byteArrayOutputStream)).intValue());
	}
	
	@Test
	public void callMethodWithTooFewParametersNamedAllowOn() throws Exception {
		EasyMock.expect(mockService.testMethod(EasyMock.anyObject(String.class))).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.setAllowExtraParams(true);
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("testMethod"), byteArrayOutputStream);
		assertEquals(METHOD_PARAMS_INVALID.code, errorCode(error(byteArrayOutputStream)).intValue());
	}
	
	@Test
	public void callMethodExactNumberOfParametersNamedAllowOn() throws Exception {
		EasyMock.expect(mockService.testMethod(EasyMock.anyObject(String.class))).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.setAllowExtraParams(true);
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("testMethod", param1, param2), byteArrayOutputStream);
		assertEquals("success", result().textValue());
	}
	
	@Test
	public void callMethodWithExtraParameterNamedAllowOn() throws Exception {
		EasyMock.expect(mockService.testMethod(EasyMock.anyObject(String.class))).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.setAllowExtraParams(true);
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("testMethod", param1, param2, param3, intParam1), byteArrayOutputStream);
		assertEquals("success", result().textValue());
	}
	
	@Test
	public void callOverloadedMethodNoNamedParams() throws Exception {
		EasyMock.expect(mockService.overloadedMethod()).andReturn(param1);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("overloadedMethod"), byteArrayOutputStream);
		assertEquals(param1, result().textValue());
	}
	
	@Test
	public void callOverloadedMethodOneNamedStringParam() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(EasyMock.anyString())).andReturn(param3);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("overloadedMethod", param1, param2), byteArrayOutputStream);
		assertEquals(param3, result().textValue());
	}
	
	@Test
	public void callOverloadedMethodOneNamedIntParam() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(intParam1)).andReturn(param2);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("overloadedMethod", param1, intParam1), byteArrayOutputStream);
		assertEquals(param2, result().textValue());
	}
	
	@Test
	public void callOverloadedMethodTwoNamedStringParams() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(param3, param4)).andReturn(param1 + param2);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("overloadedMethod", param1, param3, param2, param4), byteArrayOutputStream);
		assertEquals(param1 + param2, result().textValue());
	}
	
	@Test
	public void callOverloadedMethodTwoNamedIntParams() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(intParam1, intParam2)).andReturn((intParam1 + intParam2) + "");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("overloadedMethod", param1, intParam1, param2, intParam2), byteArrayOutputStream);
		assertEquals((intParam1 + intParam2) + "", result().textValue());
	}
	
	@Test
	public void callOverloadedMethodNamedExtraParams() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(intParam1, intParam2)).andReturn((intParam1 + intParam2) + "");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("overloadedMethod", param1, intParam1, param2, intParam2, param3, param4), byteArrayOutputStream);
		assertEquals(METHOD_PARAMS_INVALID.code, errorCode(error(byteArrayOutputStream)).intValue());
	}
	
	@Test
	public void callOverloadedMethodNamedExtraParamsAllowOn() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(intParam1, intParam2)).andReturn((intParam1 + intParam2) + "");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.setAllowExtraParams(true);
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("overloadedMethod", param1, intParam1, param2, intParam2, param3, param4), byteArrayOutputStream);
		assertEquals((intParam1 + intParam2) + "", result().textValue());
	}
	
	@Test
	public void callMethodWithoutRequiredParam() throws Exception {
		EasyMock.expect(mockService.methodWithoutRequiredParam(param2, param4)).andReturn(param2 + param4);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("methodWithoutRequiredParam", param1, param3), byteArrayOutputStream);
		assertEquals(METHOD_PARAMS_INVALID.code, errorCode(error(byteArrayOutputStream)).intValue());
	}
	
	@Test
	public void callMethodWithoutRequiredParamAllowOn() throws Exception {
		EasyMock.expect(mockService.methodWithoutRequiredParam(EasyMock.eq(param3), EasyMock.anyString())).andReturn(param3);
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.setAllowLessParams(true);
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("methodWithoutRequiredParam", param1, param3), byteArrayOutputStream);
		assertEquals(param3, result().textValue());
	}
	
	@Test
	public void callParseErrorJson() throws Exception {
		jsonRpcServerAnnotatedParam.handleRequest(Util.invalidJsonStream(), byteArrayOutputStream);
		assertEquals(PARSE_ERROR.code, errorCode(error(byteArrayOutputStream)).asInt());
	}

	@Test
	public void callMethodWithAllRequiredParametersInObjectAsParam() throws Exception {
		EasyMock.expect(mockService.testMethodWithObjParam(EasyMock.anyObject(String.class),EasyMock.anyObject(TestRequestObj.class))).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("testMethodWithObjParam", param1, param2,"obj",new TestRequestObj("1","2","3")), byteArrayOutputStream);
		assertEquals("success", result().textValue());
	}

	@Test
	public void callMethodWithNullInRequiredParametersInObjectAsParam() throws Exception {
		EasyMock.expect(mockService.testMethodWithObjParam(EasyMock.anyObject(String.class),EasyMock.anyObject(TestRequestObj.class))).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("testMethodWithObjParam", param1, param2,"obj",new TestRequestObj(null,"2","3")), byteArrayOutputStream);
		assertEquals(METHOD_PARAMS_INVALID.code, errorCode(error(byteArrayOutputStream)).intValue());
	}

	@Test
	public void callMethodWithNullInNonRequiredParametersInObjectAsParam() throws Exception {
		EasyMock.expect(mockService.testMethodWithObjParam(EasyMock.anyObject(String.class),EasyMock.anyObject(TestRequestObj.class))).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServerAnnotatedParam.handleRequest(messageWithMapParamsStream("testMethodWithObjParam", param1, param2,"obj",new TestRequestObj("1","2",null)), byteArrayOutputStream);
		assertEquals("success", result().textValue());
	}
	
	public interface ServiceInterfaceWithParamNameAnnotation {
		String testMethod(@JsonRpcParam("param1") String param1);

		String testMethodWithObjParam(@JsonRpcParam("param1") String param1,@JsonRpcParam("obj") TestRequestObj obj);
		
		String overloadedMethod();
		
		String overloadedMethod(@JsonRpcParam("param1") String stringParam1);
		
		String overloadedMethod(@JsonRpcParam("param1") String stringParam1, @JsonRpcParam("param2") String stringParam2);
		
		String overloadedMethod(@JsonRpcParam("param1") int intParam1);
		
		String overloadedMethod(@JsonRpcParam("param1") int intParam1, @JsonRpcParam("param2") int intParam2);
		
		String methodWithoutRequiredParam(@JsonRpcParam("param1") String stringParam1, @JsonRpcParam(value = "param2") String stringParam2);
	}

	@ApiModel
	public static class TestRequestObj {

		public TestRequestObj(String requiredValue, String anotherRequiredValue,
				String nonRequiredValue) {

			this.requiredValue = requiredValue;
			this.anotherRequiredValue = anotherRequiredValue;
			this.nonRequiredValue = nonRequiredValue;
		}

		// for serialization
		public TestRequestObj() {

		}

		@ApiModelProperty(required = true)
		public String requiredValue;

		@ApiModelProperty(required = true)
		public String anotherRequiredValue;

		@ApiModelProperty(required = false)
		public String nonRequiredValue;

		public String getRequiredValue() {

			return requiredValue;
		}

		public void setRequiredValue(String requiredValue) {

			this.requiredValue = requiredValue;
		}

		public String getAnotherRequiredValue() {

			return anotherRequiredValue;
		}

		public void setAnotherRequiredValue(String anotherRequiredValue) {

			this.anotherRequiredValue = anotherRequiredValue;
		}

		public String getNonRequiredValue() {

			return nonRequiredValue;
		}

		public void setNonRequiredValue(String nonRequiredValue) {

			this.nonRequiredValue = nonRequiredValue;
		}
	}
}
