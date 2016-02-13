package com.googlecode.jsonrpc4j.server;

import static com.googlecode.jsonrpc4j.util.Util.decodeAnswer;
import static com.googlecode.jsonrpc4j.util.Util.intParam1;
import static com.googlecode.jsonrpc4j.util.Util.intParam2;
import static com.googlecode.jsonrpc4j.util.Util.messageOf;
import static com.googlecode.jsonrpc4j.util.Util.messageWithListParams;
import static com.googlecode.jsonrpc4j.util.Util.param1;
import static com.googlecode.jsonrpc4j.util.Util.param2;
import static com.googlecode.jsonrpc4j.util.Util.param3;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.MockType;

import com.googlecode.jsonrpc4j.InvocationListener;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.util.CustomTestException;
import com.googlecode.jsonrpc4j.util.Util;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.List;

@RunWith(EasyMockRunner.class)
public class JsonRpcServerTest {

	@Mock(type = MockType.NICE)
	private ServiceInterface mockService;
	private ByteArrayOutputStream byteArrayOutputStream;
	private JsonRpcBasicServer jsonRpcServer;

	@Before
	public void setup() {
		byteArrayOutputStream = new ByteArrayOutputStream();
		jsonRpcServer = new JsonRpcBasicServer(Util.mapper, mockService, ServiceInterface.class);
	}

	@Test
	public void receiveJsonRpcNotification() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServer.handle(messageWithListParams(null, "testMethod", param1), byteArrayOutputStream);
		assertEquals(0, byteArrayOutputStream.size());
	}

	@Test
	public void callMethodWithTooFewParameters() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServer.handle(messageOf(1, "testMethod", null), byteArrayOutputStream);
		assertEquals(-32602, decodeAnswer(byteArrayOutputStream).get("error").get("code").intValue());
	}

	@Test
	public void callMethodExactNumberOfParameters() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServer.handle(messageWithListParams(1, "testMethod", param1), byteArrayOutputStream);
		assertEquals("success", decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callMethodWithExtraParameter() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServer.handle(messageWithListParams(1, "testMethod", param1, param2), byteArrayOutputStream);
		assertEquals(-32602, decodeAnswer(byteArrayOutputStream).get("error").get("code").intValue());
	}

	@Test
	public void callOverloadedMethodExtraParamsAllowOn() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(param1, param2)).andReturn(param1 + param2);
		EasyMock.replay(mockService);
		jsonRpcServer.setAllowExtraParams(true);
		jsonRpcServer.handle(messageWithListParams(1, "overloadedMethod", param1, param2, param3), byteArrayOutputStream);
		assertEquals(param1 + param2, decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callMethodWithTooFewParametersAllowOn() throws Exception {
		EasyMock.expect(mockService.testMethod(anyObject())).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServer.setAllowLessParams(true);
		jsonRpcServer.handle(messageWithListParams(1, "testMethod"), byteArrayOutputStream);
		assertEquals("success", decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callMethodExactNumberOfParametersAllowOn() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServer.setAllowExtraParams(true);
		jsonRpcServer.handle(messageWithListParams(1, "testMethod", param1), byteArrayOutputStream);
		assertEquals("success", decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callMethodWithExtraParameterAllowOn() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServer.setAllowExtraParams(true);
		jsonRpcServer.handle(messageWithListParams(1, "testMethod", param1, param2), byteArrayOutputStream);
		assertEquals("success", decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callOverloadedMethodNoParams() throws Exception {
		final String noParam = "noParam";
		EasyMock.expect(mockService.overloadedMethod()).andReturn(noParam);
		EasyMock.replay(mockService);
		jsonRpcServer.handle(messageWithListParams(1, "overloadedMethod"), byteArrayOutputStream);
		assertEquals(noParam, decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callOverloadedMethodOneStringParam() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(param2)).andReturn(param2);
		EasyMock.replay(mockService);
		jsonRpcServer.handle(messageWithListParams(1, "overloadedMethod", param2), byteArrayOutputStream);
		assertEquals(param2, decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callOverloadedMethodOneIntParam() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(intParam1)).andReturn(param1 + intParam1);
		EasyMock.replay(mockService);
		jsonRpcServer.handle(messageWithListParams(1, "overloadedMethod", intParam1), byteArrayOutputStream);
		assertEquals(param1 + intParam1, decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callOverloadedMethodTwoStringParams() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(param1, param2)).andReturn(param1 + param2);
		EasyMock.replay(mockService);
		jsonRpcServer.handle(messageWithListParams(1, "overloadedMethod", param1, param2), byteArrayOutputStream);
		assertEquals(param1 + param2, decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callOverloadedMethodTwoIntParams() throws Exception {
		final String result = (intParam1 + intParam2) + "";
		EasyMock.expect(mockService.overloadedMethod(intParam1, intParam2)).andReturn(result);
		EasyMock.replay(mockService);
		jsonRpcServer.handle(messageWithListParams(1, "overloadedMethod", intParam1, intParam2), byteArrayOutputStream);
		assertEquals(result, decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void callOverloadedMethodExtraParams() throws Exception {
		EasyMock.expect(mockService.overloadedMethod(param1, param2)).andReturn(param1 + param2);
		EasyMock.replay(mockService);
		jsonRpcServer.setAllowExtraParams(true);
		jsonRpcServer.handle(messageWithListParams(1, "overloadedMethod", param1, param2, param3), byteArrayOutputStream);
		assertEquals(param1 + param2, decodeAnswer(byteArrayOutputStream).get("result").textValue());
	}

	@Test
	public void idIntegerType() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServer.handle(messageWithListParams(intParam1, "testMethod", param1), byteArrayOutputStream);
		assertTrue(decodeAnswer(byteArrayOutputStream).get("id").isIntegralNumber());
	}

	@Test
	public void idStringType() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn("success");
		EasyMock.replay(mockService);
		jsonRpcServer.handle(messageWithListParams(param1, "testMethod", param1), byteArrayOutputStream);
		assertTrue(decodeAnswer(byteArrayOutputStream).get("id").isTextual());
	}

	@Test
	public void noId() throws Exception {
		EasyMock.expect(mockService.testMethod(param1)).andReturn("success");
		EasyMock.replay(mockService);
		int ok_code = jsonRpcServer.handle(messageWithListParams(null, "testMethod", param1), byteArrayOutputStream);
		assertTrue(byteArrayOutputStream.toString(Util.JSON_ENCODING).isEmpty());
		assertEquals(ok_code, JsonRpcBasicServer.CODE_OK);
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
		invocationListener.didInvoke(eq(m), anyObject(List.class), EasyMock.isNull(), EasyMock.notNull(), EasyMock.geq(0L));

		jsonRpcServer.setInvocationListener(invocationListener);

		EasyMock.expect(mockService.throwsMethod(param1)).andThrow(new CustomTestException(param1));
		EasyMock.replay(mockService, invocationListener);

		jsonRpcServer.handle(messageWithListParams(1, "throwsMethod", param1), byteArrayOutputStream);

		EasyMock.verify(invocationListener, mockService);

		JsonNode json = decodeAnswer(byteArrayOutputStream);
		assertNull(json.get("result"));
		assertNotNull(json.get("error"));
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
		invocationListener.didInvoke(eq(m), anyObject(List.class), EasyMock.notNull(), EasyMock.isNull(), EasyMock.geq(0L));

		jsonRpcServer.setInvocationListener(invocationListener);

		EasyMock.expect(mockService.throwsMethod(param1)).andReturn(param1);
		EasyMock.replay(mockService, invocationListener);

		jsonRpcServer.handle(messageWithListParams(1, "throwsMethod", param1), byteArrayOutputStream);

		EasyMock.verify(invocationListener, mockService);

		JsonNode json = decodeAnswer(byteArrayOutputStream);
		assertEquals(param1, json.get("result").textValue());
		assertNull(json.get("error"));
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
	}

}
