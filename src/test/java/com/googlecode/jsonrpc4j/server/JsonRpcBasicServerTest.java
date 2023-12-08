package com.googlecode.jsonrpc4j.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.ConvertedParameterTransformer;
import com.googlecode.jsonrpc4j.ErrorResolver.JsonError;
import com.googlecode.jsonrpc4j.InvocationListener;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcInterceptor;
import com.googlecode.jsonrpc4j.util.CustomTestException;
import com.googlecode.jsonrpc4j.util.Util;
import org.easymock.EasyMock;
import org.easymock.EasyMockExtension;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.METHOD_PARAMS_INVALID;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.ID;
import static com.googlecode.jsonrpc4j.util.Util.*;
import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EasyMockExtension.class)
public class JsonRpcBasicServerTest {
	
	@Mock(type = MockType.NICE)
	private ServiceInterface mockService;
	@Mock(type = MockType.NICE)
	private JsonRpcInterceptor mockInterceptor;
	private ByteArrayOutputStream byteArrayOutputStream;
	private JsonRpcBasicServer jsonRpcServer;

	@BeforeEach
	public void setup() {
		byteArrayOutputStream = new ByteArrayOutputStream();
		jsonRpcServer = new JsonRpcBasicServer(Util.mapper, mockService, ServiceInterface.class);
        jsonRpcServer.setInterceptorList(new ArrayList<JsonRpcInterceptor>() {{
            add(mockInterceptor);
        }});
    }
	
	@Test
	public void receiveJsonRpcNotification() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn(param1);
		EasyMock.replay(mockService);
		jsonRpcServer.handleRequest(messageWithListParamsStream(null, "testMethod", param1), byteArrayOutputStream);
		assertEquals(0, byteArrayOutputStream.size());
	}
	
	@Test
	public void callMethodWithTooFewParameters() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn(param1);
		EasyMock.replay(mockService);
		jsonRpcServer.handleRequest(createStream(messageOfStream(1, "testMethod", null)), byteArrayOutputStream);
		assertEquals(METHOD_PARAMS_INVALID.code, decodeAnswer(byteArrayOutputStream).get(JsonRpcBasicServer.ERROR).get(JsonRpcBasicServer.ERROR_CODE).intValue());
	}
	
	@Test
	public void callMethodExactNumberOfParameters() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn(param1);
		EasyMock.replay(mockService);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "testMethod", param1), byteArrayOutputStream);
		assertEquals(param1, decodeAnswer(byteArrayOutputStream).get(JsonRpcBasicServer.RESULT).textValue());
	}
	
	@Test
	public void callMethodWithExtraParameter() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn(param1);
		EasyMock.replay(mockService);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "testMethod", param1, param2), byteArrayOutputStream);
		assertEquals(METHOD_PARAMS_INVALID.code, decodeAnswer(byteArrayOutputStream).get(JsonRpcBasicServer.ERROR).get(JsonRpcBasicServer.ERROR_CODE).intValue());
	}
	
	@Test
	public void callOverloadedMethodExtraParamsAllowOn() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(param1, param2)).andReturn(param1 + param2);
		EasyMock.replay(mockService);
		jsonRpcServer.setAllowExtraParams(true);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "overloadedMethod", param1, param2, param3), byteArrayOutputStream);
		assertEquals(param1 + param2, decodeAnswer(byteArrayOutputStream).get(JsonRpcBasicServer.RESULT).textValue());
	}
	
	@Test
	public void callMethodWithTooFewParametersAllowOn() throws Exception {
		EasyMock.expect(mockService.testMethod(anyString())).andReturn(param1);
		EasyMock.replay(mockService);
		jsonRpcServer.setAllowLessParams(true);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "testMethod"), byteArrayOutputStream);
		assertEquals(param1, decodeAnswer(byteArrayOutputStream).get(JsonRpcBasicServer.RESULT).textValue());
	}
	
	@Test
	public void callMethodExactNumberOfParametersAllowOn() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn(param1);
		EasyMock.replay(mockService);
		jsonRpcServer.setAllowExtraParams(true);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "testMethod", param1), byteArrayOutputStream);
		assertEquals(param1, decodeAnswer(byteArrayOutputStream).get(JsonRpcBasicServer.RESULT).textValue());
	}
	
	@Test
	public void callMethodWithExtraParameterAllowOn() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn(param1);
		EasyMock.replay(mockService);
		jsonRpcServer.setAllowExtraParams(true);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "testMethod", param1, param2), byteArrayOutputStream);
		assertEquals(param1, decodeAnswer(byteArrayOutputStream).get(JsonRpcBasicServer.RESULT).textValue());
	}
	
	@Test
	public void callOverloadedMethodNoParams() throws Exception {
		final String noParam = "noParam";
		EasyMock.expect(mockService.overloadedMethod()).andReturn(noParam);
		EasyMock.replay(mockService);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "overloadedMethod"), byteArrayOutputStream);
		assertEquals(noParam, decodeAnswer(byteArrayOutputStream).get(JsonRpcBasicServer.RESULT).textValue());
	}
	
	@Test
	public void callOverloadedMethodOneStringParam() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(param2)).andReturn(param2);
		EasyMock.replay(mockService);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "overloadedMethod", param2), byteArrayOutputStream);
		assertEquals(param2, decodeAnswer(byteArrayOutputStream).get(JsonRpcBasicServer.RESULT).textValue());
	}
	
	@Test
	public void callOverloadedMethodOneIntParam() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(intParam1)).andReturn(param1 + intParam1);
		EasyMock.replay(mockService);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "overloadedMethod", intParam1), byteArrayOutputStream);
		assertEquals(param1 + intParam1, decodeAnswer(byteArrayOutputStream).get(JsonRpcBasicServer.RESULT).textValue());
	}
	
	@Test
	public void callOverloadedMethodTwoStringParams() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(param1, param2)).andReturn(param1 + param2);
		EasyMock.replay(mockService);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "overloadedMethod", param1, param2), byteArrayOutputStream);
		assertEquals(param1 + param2, decodeAnswer(byteArrayOutputStream).get(JsonRpcBasicServer.RESULT).textValue());
	}
	
	@Test
	public void callOverloadedMethodTwoIntParams() throws Exception {
		final String result = (intParam1 + intParam2) + "";
		EasyMock.expect(mockService.overloadedMethod(intParam1, intParam2)).andReturn(result);
		EasyMock.replay(mockService);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "overloadedMethod", intParam1, intParam2), byteArrayOutputStream);
		assertEquals(result, decodeAnswer(byteArrayOutputStream).get(JsonRpcBasicServer.RESULT).textValue());
	}
	
	@Test
	public void callOverloadedMethodExtraParams() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(param1, param2)).andReturn(param1 + param2);
		EasyMock.replay(mockService);
		jsonRpcServer.setAllowExtraParams(true);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "overloadedMethod", param1, param2, param3), byteArrayOutputStream);
		assertEquals(param1 + param2, decodeAnswer(byteArrayOutputStream).get(JsonRpcBasicServer.RESULT).textValue());
	}

	@Test
	public void callVoidMethod() throws Exception {
		mockService.voidMethod(intParam1);
		EasyMock.replay(mockService);
		jsonRpcServer.handleRequest(
			messageWithListParamsStream(1, "voidMethod", intParam1),
			byteArrayOutputStream
		);
		assertNull(decodeAnswer(byteArrayOutputStream).get(JsonRpcBasicServer.RESULT).textValue());
		EasyMock.verify(mockService);
	}
	
	@Test
	public void idIntegerType() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn(param1);
		EasyMock.replay(mockService);
		jsonRpcServer.handleRequest(messageWithListParamsStream(intParam1, "testMethod", param1), byteArrayOutputStream);
		assertTrue(decodeAnswer(byteArrayOutputStream).get(ID).isIntegralNumber());
	}

	@Test
	public void idLongType() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn(param1);
		EasyMock.replay(mockService);
		jsonRpcServer.handleRequest(messageWithListParamsStream(longParam, "testMethod", param1), byteArrayOutputStream);
		assertTrue(decodeAnswer(byteArrayOutputStream).get(ID).isLong());
	}

	@Test
	public void idStringType() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn(param1);
		EasyMock.replay(mockService);
		jsonRpcServer.handleRequest(messageWithListParamsStream(param1, "testMethod", param1), byteArrayOutputStream);
		assertTrue(decodeAnswer(byteArrayOutputStream).get(ID).isTextual());
	}
	
	@Test
	public void noId() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn(param1);
		EasyMock.replay(mockService);
		int ok_code = jsonRpcServer.handleRequest(messageWithListParamsStream(null, "testMethod", param1), byteArrayOutputStream);
		assertTrue(byteArrayOutputStream.toString(Util.JSON_ENCODING).isEmpty());
		assertEquals(ok_code, JsonRpcBasicServer.CODE_OK);
	}

	@Test
	public void receiveInvalidJson() throws IOException {
		jsonRpcServer.handleRequest(invalidJsonStream(), byteArrayOutputStream);
		assertEquals(JsonError.PARSE_ERROR.code, decodeAnswer(byteArrayOutputStream).get(JsonRpcBasicServer.ERROR).get(JsonRpcBasicServer.ERROR_CODE).intValue());
	}

	@Test
	public void receiveEmptyLine() throws IOException {
		jsonRpcServer.handleRequest(emptyLineStream(), byteArrayOutputStream);
		assertEquals(JsonError.PARSE_ERROR.code, decodeAnswer(byteArrayOutputStream).get(JsonRpcBasicServer.ERROR).get(JsonRpcBasicServer.ERROR_CODE).intValue());
	}

	/**
	 * The {@link com.googlecode.jsonrpc4j.JsonRpcBasicServer} is able to have an instance of
	 * {@link com.googlecode.jsonrpc4j.InvocationListener} configured for it.  Prior to a
	 * method being invoked, the lister is notified and after the method is invoked, the
	 * listener is notified.  This test checks that these two events are hit correctly in
	 * the case that an exception is raised when the method is invoked.
	 */
	
	@Test
	@SuppressWarnings("unchecked")
	public void callMethodThrowingWithInvocationListener() throws Exception {
		final InvocationListener invocationListener = EasyMock.niceMock(InvocationListener.class);
		Method m = ServiceInterface.class.getMethod("throwsMethod", String.class);
		invocationListener.willInvoke(eq(m), anyObject(List.class));
		invocationListener.didInvoke(eq(m), anyObject(List.class), EasyMock.isNull(), EasyMock.<Throwable>notNull(), EasyMock.geq(0L));
		
		jsonRpcServer.setInvocationListener(invocationListener);
		
		EasyMock.expect(mockService.throwsMethod(param1)).andThrow(new CustomTestException(param1));
		EasyMock.replay(mockService, invocationListener);
		
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "throwsMethod", param1), byteArrayOutputStream);
		
		EasyMock.verify(invocationListener, mockService);
		
		JsonNode json = decodeAnswer(byteArrayOutputStream);
		assertNull(json.get(JsonRpcBasicServer.RESULT));
		assertNotNull(json.get(JsonRpcBasicServer.ERROR));
	}
	
	/**
	 * The {@link com.googlecode.jsonrpc4j.JsonRpcBasicServer} is able to have an instance of
	 * {@link com.googlecode.jsonrpc4j.InvocationListener} configured for it.  Prior to a
	 * method being invoked, the lister is notified and after the method is invoked, the
	 * listener is notified.  This test checks that these two events are hit correctly
	 * when a method is invoked.
	 */
	
	@SuppressWarnings("unchecked")
	@Test
	public void callMethodWithInvocationListener() throws Exception {
		final InvocationListener invocationListener = EasyMock.niceMock(InvocationListener.class);
		Method m = ServiceInterface.class.getMethod("throwsMethod", String.class);
		invocationListener.willInvoke(eq(m), anyObject(List.class));
		invocationListener.didInvoke(eq(m), anyObject(List.class), EasyMock.notNull(), EasyMock.<Throwable>isNull(), EasyMock.geq(0L));
		
		jsonRpcServer.setInvocationListener(invocationListener);
		
		EasyMock.expect(mockService.throwsMethod(param1)).andReturn(param1);
		EasyMock.replay(mockService, invocationListener);
		
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "throwsMethod", param1), byteArrayOutputStream);
		
		EasyMock.verify(invocationListener, mockService);
		
		JsonNode json = decodeAnswer(byteArrayOutputStream);
		assertEquals(param1, json.get(JsonRpcBasicServer.RESULT).textValue());
		assertNull(json.get(JsonRpcBasicServer.ERROR));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void callConvertedParameterTransformerShouldBeCalledIfSet() throws Exception {
		final ConvertedParameterTransformer convertedParameterTransformer = EasyMock.niceMock(ConvertedParameterTransformer.class);
		
		EasyMock.expect(mockService.testMethod(param1)).andReturn(param1);
		jsonRpcServer.setConvertedParameterTransformer(convertedParameterTransformer);
		
		EasyMock.expect(convertedParameterTransformer.transformConvertedParameters(anyObject(), anyObject(Object[].class))).andReturn(new Object[]{param1});
		EasyMock.replay(convertedParameterTransformer);
		
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "testMethod", param1), byteArrayOutputStream);
		
		EasyMock.verify(convertedParameterTransformer);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void callConvertedParameterTransformerShouldTransformTheParameters() throws Exception {
		final ConvertedParameterTransformer convertedParameterTransformer = EasyMock.niceMock(ConvertedParameterTransformer.class);
		
		String[] parameters = {param1, param2};
		String[] expectedConvertedParameters = {param2, param1};
		
		EasyMock.expect(mockService.overloadedMethod(param2, param1)).andReturn("converted");
		jsonRpcServer.setConvertedParameterTransformer(convertedParameterTransformer);
		
		EasyMock.expect(convertedParameterTransformer.transformConvertedParameters(anyObject(), anyObject(Object[].class))).andReturn(expectedConvertedParameters);
		EasyMock.replay(mockService, convertedParameterTransformer);
		
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "overloadedMethod", (Object[]) parameters), byteArrayOutputStream);
		
		JsonNode json = decodeAnswer(byteArrayOutputStream);
		assertEquals("converted", json.get(JsonRpcBasicServer.RESULT).textValue());
		assertNull(json.get(JsonRpcBasicServer.ERROR));
	}
	
	@Test
	public void multiMessageSimple() throws IOException {
		EasyMock.expect(mockService.testMethod(param1)).andReturn(param2);
		EasyMock.expect(mockService.overloadedMethod(intParam1)).andReturn(param1);
		EasyMock.replay(mockService);
		
		InputStream input = multiMessageOfStream(messageWithListParams(1, "testMethod", param1),
				messageWithListParams(2, "overloadedMethod", intParam1));
		jsonRpcServer.handleRequest(input, byteArrayOutputStream);
		JsonNode json = decodeAnswer(byteArrayOutputStream);
		assertTrue(json.isArray());
		assertEquals(getFromArrayWithId(json, 1).get(JsonRpcBasicServer.RESULT).asText(), param2);
		assertEquals(getFromArrayWithId(json, 2).get(JsonRpcBasicServer.RESULT).asText(), param1);
		EasyMock.verify(mockService);
	}
	
	@Test
	public void multiMessageOneOkOneError() throws IOException {
		EasyMock.expect(mockService.overloadedMethod(param1)).andReturn(param2);
		EasyMock.expect(mockService.throwsMethod(param3)).andThrow(new CustomTestException(param4));
		EasyMock.replay(mockService);
		
		InputStream input = multiMessageOfStream(messageWithListParams(1, "overloadedMethod", param1),
				messageWithListParams(2, "throwsMethod", param3),
				messageWithListParams(3, "testMethod"));
		jsonRpcServer.handleRequest(input, byteArrayOutputStream);
		JsonNode json = decodeAnswer(byteArrayOutputStream);
		assertTrue(json.isArray());
		assertEquals(getFromArrayWithId(json, 1).get(JsonRpcBasicServer.RESULT).asText(), param2);
		assertEquals(getFromArrayWithId(json, 2).get(JsonRpcBasicServer.RESULT), null);
		assertFalse(getFromArrayWithId(json, 2).get(JsonRpcBasicServer.ERROR).isNull());
		assertEquals(getFromArrayWithId(json, 2).get(JsonRpcBasicServer.ERROR).get(JsonRpcBasicServer.DATA).get(JsonRpcBasicServer.ERROR_MESSAGE).asText(), param4);
		EasyMock.verify(mockService);
	}

	@Test
    public void interceptorsNotJsonRpcTest() throws IOException {
        String requestNotRpc = "{\"test\": 1}";
		String responseError = "{\"jsonrpc\":\"2.0\",\"id\":\"null\",\"error\":{\"code\":-32601,\"message\":\"method not found\"}}";


		//bad call
        mockInterceptor.preHandleJson(mapper.readTree(requestNotRpc));
        expectLastCall().times(1);
		mockInterceptor.postHandleJson(anyObject(JsonNode.class));
//		mockInterceptor.postHandleJson(Util.mapper.readTree(responseError)); this place cause problem
		// json nodes are same but EasyMock don't understand it
		expectLastCall().times(1);

		replay(mockInterceptor);
        jsonRpcServer.handleRequest(new ByteArrayInputStream(requestNotRpc.getBytes(StandardCharsets.UTF_8)), byteArrayOutputStream);

		verify(mockInterceptor);
	}



	@Test
	public void interceptorsGoodTest() throws IOException {
		final String requestGood = "{\n" +
				"  \"id\": 0,\n" +
				"  \"jsonrpc\": \"2.0\",\n" +
				"  \"method\": \"overloadedMethod\",\n" +
				"  \"params\": [\"test.cool\",\"test.ru\"]\n" +
				"  }\n" +
				"}";
		final String responseGood = "{\n" +
                "  \"jsonrpc\": \"2.0\",\n" +
                "  \"id\": 0,\n" +
				"  \"result\": \"test.ru\"}\n" +
				"}";
        Iterator<JsonNode> paramsIterator = mapper.readTree(requestGood).at("/params").iterator();
        List<JsonNode> paramsNodes = new ArrayList<>();
        while (paramsIterator.hasNext()) {
            paramsNodes.add(paramsIterator.next());
        }

		// good call
        mockInterceptor.preHandleJson(mapper.readTree(requestGood));
        expectLastCall().times(1);
		mockInterceptor.preHandle(
				anyObject(),
				anyObject(Method.class),
                eq(paramsNodes)
        );
		expectLastCall().times(1);
        expect(mockService.overloadedMethod("test.cool", "test.ru")).andReturn("test.ru");
        mockInterceptor.postHandle(
				anyObject(),
				anyObject(Method.class),
                eq(paramsNodes),
                eq(mapper.readTree(responseGood).at("/result"))
        );
		expectLastCall().times(1);
        mockInterceptor.postHandleJson(mapper.readTree(responseGood));
        expectLastCall().times(1);

		replay(mockService, mockInterceptor);
        jsonRpcServer.handleRequest(new ByteArrayInputStream(requestGood.getBytes(StandardCharsets.UTF_8)), byteArrayOutputStream);

        verify(mockService, mockInterceptor);
    }
	
	// Service and service interfaces used in test
	
	public interface ServiceInterface {
		String testMethod(String param1);
		
		String overloadedMethod();
		
		String overloadedMethod(String stringParam1);
		
		String overloadedMethod(String stringParam1, String stringParam2);

		String overloadedMethod(int intParam1);
		
		String overloadedMethod(int intParam1, int intParam2);
		
		String throwsMethod(String param1) throws CustomTestException;

		void voidMethod(int intParam1);
	}
	
}
