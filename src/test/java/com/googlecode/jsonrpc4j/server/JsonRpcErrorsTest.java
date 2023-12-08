package com.googlecode.jsonrpc4j.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.ErrorResolver;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.util.CustomTestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static com.googlecode.jsonrpc4j.util.Util.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * For testing the @JsonRpcErrors and @JsonRpcError annotations
 */
public class JsonRpcErrorsTest {
	
	private ByteArrayOutputStream byteArrayOutputStream;
	private CustomTestException testException;
	private CustomTestException testExceptionWithMessage;

	@BeforeEach
	public void setup() {
		byteArrayOutputStream = new ByteArrayOutputStream();
		testException = new CustomTestException();
		testExceptionWithMessage = new CustomTestException("exception message");
	}
	
	@Test
	public void exceptionWithoutAnnotatedServiceInterface() throws Exception {
		JsonRpcBasicServer jsonRpcServer = new JsonRpcBasicServer(mapper, new Service(), ServiceInterfaceWithoutAnnotation.class);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "testMethod"), byteArrayOutputStream);
		JsonNode error = error(byteArrayOutputStream);
		assertNotNull(error);
		assertEquals(ErrorResolver.JsonError.ERROR_NOT_HANDLED.code, errorCode(error).intValue());
	}
	
	@Test
	public void exceptionWithAnnotatedServiceInterface() throws Exception {
		JsonRpcBasicServer jsonRpcServer = new JsonRpcBasicServer(mapper, new Service(), ServiceInterfaceWithAnnotation.class);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "testMethod"), byteArrayOutputStream);
		
		JsonNode error = error(byteArrayOutputStream);
		assertNotNull(error);
		assertEquals(1234, errorCode(error).intValue());
		assertEquals(null, errorMessage(error).textValue());
		JsonNode data = errorData(error);
		assertNotNull(data);
		assertEquals(null, errorMessage(data).textValue());
		assertEquals(CustomTestException.class.getName(), exceptionType(data).textValue());
	}
	
	@Test
	public void exceptionWithAnnotatedServiceInterfaceMessageAndData() throws Exception {
		JsonRpcBasicServer jsonRpcServer = new JsonRpcBasicServer(mapper, new Service(), ServiceInterfaceWithAnnotationMessageAndData.class);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "testMethod"), byteArrayOutputStream);
		
		JsonNode error = error(byteArrayOutputStream);
		
		assertNotNull(error);
		assertEquals(-5678, errorCode(error).intValue());
		assertEquals("The message", errorMessage(error).textValue());
	}
	
	@Test
	public void exceptionWithMsgInException() throws Exception {
		JsonRpcBasicServer jsonRpcServer = new JsonRpcBasicServer(mapper, new ServiceWithExceptionMsg(), ServiceInterfaceWithAnnotation.class);
		jsonRpcServer.handleRequest(messageWithListParamsStream(1, "testMethod"), byteArrayOutputStream);
		
		JsonNode error = error(byteArrayOutputStream);
		
		assertNotNull(error);
		assertEquals(1234, errorCode(error).intValue());
		assertEquals(testExceptionWithMessage.getMessage(), errorMessage(error).textValue());
		JsonNode data = errorData(error);
		assertNotNull(data);
		assertEquals(testExceptionWithMessage.getMessage(), errorMessage(data).textValue());
		assertEquals(CustomTestException.class.getName(), exceptionType(data).textValue());
	}
	
	@SuppressWarnings({"unused", "WeakerAccess"})
	public interface ServiceInterfaceWithoutAnnotation {
		Object testMethod();
	}
	
	@SuppressWarnings({"unused", "WeakerAccess"})
	public interface ServiceInterfaceWithAnnotation {
		@JsonRpcErrors({@JsonRpcError(exception = CustomTestException.class, code = 1234)})
		Object testMethod();
	}
	
	@SuppressWarnings({"unused", "WeakerAccess"})
	public interface ServiceInterfaceWithAnnotationMessageAndData {
		@JsonRpcErrors({@JsonRpcError(exception = CustomTestException.class, code = -5678, message = "The message", data = "The data")})
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
