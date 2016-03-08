package com.googlecode.jsonrpc4j.server;

import static com.googlecode.jsonrpc4j.util.Util.decodeAnswer;
import static com.googlecode.jsonrpc4j.util.Util.mapper;
import static com.googlecode.jsonrpc4j.util.Util.messageWithListParamsStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.googlecode.jsonrpc4j.ErrorResolver;
import org.junit.Before;
import org.junit.Test;

import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.util.CustomTestException;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.ByteArrayOutputStream;

/**
 * For testing the @JsonRpcErrors and @JsonRpcError annotations
 */
public class JsonRpcErrorsTest {

	private ByteArrayOutputStream byteArrayOutputStream;
	private CustomTestException testException;
	private CustomTestException testExceptionWithMessage;

	@Before
	public void setup() {
		byteArrayOutputStream = new ByteArrayOutputStream();
		testException = new CustomTestException();
		testExceptionWithMessage = new CustomTestException("exception message");
	}

	@Test
	public void exceptionWithoutAnnotatedServiceInterface() throws Exception {
		JsonRpcBasicServer jsonRpcServer = new JsonRpcBasicServer(mapper, new Service(), ServiceInterfaceWithoutAnnotation.class);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "testMethod"), byteArrayOutputStream);
		JsonNode error = decodeAnswer(byteArrayOutputStream).get("error");
		assertNotNull(error);
		assertEquals(ErrorResolver.JsonError.ERROR_NOT_HANDLED.code, error.get("code").intValue());
	}

	@Test
	public void exceptionWithAnnotatedServiceInterface() throws Exception {
		JsonRpcBasicServer jsonRpcServer = new JsonRpcBasicServer(mapper, new Service(), ServiceInterfaceWithAnnotation.class);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "testMethod"), byteArrayOutputStream);

		JsonNode error = decodeAnswer(byteArrayOutputStream).get("error");

		assertNotNull(error);
		assertEquals(1234, error.get("code").intValue());
		assertEquals(null, error.get("message").textValue());
		assertNotNull(error.get("data"));
		JsonNode data = error.get("data");
		assertEquals(null, data.get("message").textValue());
		assertEquals(CustomTestException.class.getName(), data.get("exceptionTypeName").textValue());
	}

	@Test
	public void exceptionWithAnnotatedServiceInterfaceMessageAndData() throws Exception {
		JsonRpcBasicServer jsonRpcServer = new JsonRpcBasicServer(mapper, new Service(), ServiceInterfaceWithAnnotationMessageAndData.class);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "testMethod"), byteArrayOutputStream);

		JsonNode error = decodeAnswer(byteArrayOutputStream).get("error");

		assertNotNull(error);
		assertEquals(-5678, error.get("code").intValue());
		assertEquals("The message", error.get("message").textValue());
		assertNotNull(error.get("data"));
		JsonNode data = error.get("data");
		assertEquals("The message", data.get("message").textValue());
		assertEquals(CustomTestException.class.getName(), data.get("exceptionTypeName").textValue());
	}

	@Test
	public void exceptionWithMsgInException() throws Exception {
		JsonRpcBasicServer jsonRpcServer = new JsonRpcBasicServer(mapper, new ServiceWithExceptionMsg(), ServiceInterfaceWithAnnotation.class);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "testMethod"), byteArrayOutputStream);

		JsonNode error = decodeAnswer(byteArrayOutputStream).get("error");

		assertNotNull(error);
		assertEquals(1234, error.get("code").intValue());
		assertEquals(testExceptionWithMessage.getMessage(), error.get("message").textValue());
		assertNotNull(error.get("data"));
		JsonNode data = error.get("data");
		assertEquals(testExceptionWithMessage.getMessage(), data.get("message").textValue());
		assertEquals(CustomTestException.class.getName(), data.get("exceptionTypeName").textValue());
	}

	@SuppressWarnings({ "unused", "WeakerAccess" })
	public interface ServiceInterfaceWithoutAnnotation {
		Object testMethod();
	}

	@SuppressWarnings({ "unused", "WeakerAccess" })
	public interface ServiceInterfaceWithAnnotation {
		@JsonRpcErrors({ @JsonRpcError(exception = CustomTestException.class, code = 1234) })
		Object testMethod();
	}

	@SuppressWarnings({ "unused", "WeakerAccess" })
	public interface ServiceInterfaceWithAnnotationMessageAndData {
		@JsonRpcErrors({ @JsonRpcError(exception = CustomTestException.class, code = -5678, message = "The message", data = "The data") })
		Object testMethod();
	}

	private class Service implements ServiceInterfaceWithoutAnnotation, ServiceInterfaceWithAnnotation, ServiceInterfaceWithAnnotationMessageAndData {
		public Object testMethod() {
			throw testException;
		}
	}

	private class ServiceWithExceptionMsg implements ServiceInterfaceWithoutAnnotation, ServiceInterfaceWithAnnotation, ServiceInterfaceWithAnnotationMessageAndData {
		public Object testMethod() {
			throw testExceptionWithMessage;
		}
	}

}
