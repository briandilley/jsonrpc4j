package com.googlecode.jsonrpc4j.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.ErrorResolver;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.util.Util;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.ID;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.RESULT;
import static com.googlecode.jsonrpc4j.util.Util.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(EasyMockRunner.class)
public class JsonRpcServerTest {

	@Mock(type = MockType.NICE)
	private ServiceInterface mockService;
	private JsonRpcServer jsonRpcServer;

	@Before
	public void setup() {
		jsonRpcServer = new JsonRpcServer(Util.mapper, mockService, ServiceInterface.class);
	}

	@Test
	public void testGetMethod_badRequest_corruptParams() throws Exception {
		EasyMock.expect(mockService.testMethod("Whirinaki")).andReturn("Forest");
		EasyMock.replay(mockService);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test-get");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.addParameter("id", Integer.toString(123));
		request.addParameter("method", "testMethod");
		request.addParameter("params", "{BROKEN}");

		jsonRpcServer.handle(request, response);

		assertTrue(MockHttpServletResponse.SC_BAD_REQUEST == response.getStatus());

		JsonNode errorNode = error(toByteArrayOutputStream(response.getContentAsByteArray()));

		assertNotNull(errorNode);
		assertEquals(errorCode(errorNode).asLong(), (long) ErrorResolver.JsonError.PARSE_ERROR.code);
	}

	@Test
	public void testGetMethod_badRequest_noMethod() throws Exception {
		EasyMock.expect(mockService.testMethod("Whirinaki")).andReturn("Forest");
		EasyMock.replay(mockService);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test-get");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.addParameter("id", Integer.toString(123));
		// no method!
		request.addParameter("params", net.iharder.Base64.encodeBytes("[\"Whirinaki\"]".getBytes(StandardCharsets.UTF_8)));

		jsonRpcServer.handle(request, response);

		assertTrue(MockHttpServletResponse.SC_NOT_FOUND == response.getStatus());

		JsonNode errorNode = error(toByteArrayOutputStream(response.getContentAsByteArray()));

		assertNotNull(errorNode);
		assertEquals(errorCode(errorNode).asLong(), (long) ErrorResolver.JsonError.METHOD_NOT_FOUND.code);
	}

	private void checkSuccessfulResponse(MockHttpServletResponse response) throws IOException {
		assertTrue(HttpServletResponse.SC_OK == response.getStatus());

		JsonNode responseEnvelope = decodeAnswer(toByteArrayOutputStream(response.getContentAsByteArray()));
		assertTrue(responseEnvelope.get(ID).isIntegralNumber());
		assertEquals(responseEnvelope.get(ID).asLong(), 123L);
		assertTrue(responseEnvelope.get(RESULT).isTextual());
		assertEquals(responseEnvelope.get(RESULT).asText(), "For?est");
	}

	@Test
	public void testGetMethod_base64Params() throws Exception {
		EasyMock.expect(mockService.testMethod("Whir?inaki")).andReturn("For?est");
		EasyMock.replay(mockService);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test-get");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.addParameter("id", Integer.toString(123));
		request.addParameter("method", "testMethod");
		request.addParameter("params", net.iharder.Base64.encodeBytes("[\"Whir?inaki\"]".getBytes(StandardCharsets.UTF_8)));

		jsonRpcServer.handle(request, response);

		checkSuccessfulResponse(response);
	}

	@Test
	public void testGetMethod_unencodedParams() throws Exception {
		EasyMock.expect(mockService.testMethod("Whir?inaki")).andReturn("For?est");
		EasyMock.replay(mockService);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test-get");
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.addParameter("id", Integer.toString(123));
		request.addParameter("method", "testMethod");
		request.addParameter("params", "[\"Whir?inaki\"]");

		jsonRpcServer.handle(request, response);

		checkSuccessfulResponse(response);
	}

	// Service and service interfaces used in test

	public interface ServiceInterface {
		String testMethod(String param1);
	}

}
