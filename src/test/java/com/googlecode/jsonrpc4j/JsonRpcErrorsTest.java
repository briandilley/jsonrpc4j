package com.googlecode.jsonrpc4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * For testing the @JsonRpcErrors and @JsonRpcError annotations
 * 
 * @author Hans J??rgen Hoel (hansjorgen.hoel@nhst.no)
 *
 */
public class JsonRpcErrorsTest {

	private static final String JSON_ENCODING = "UTF-8";
	private static final String JSON_FILE = "jsonRpcErrorTest.json";

	private ObjectMapper mapper;
	private ByteArrayOutputStream baos;
	private TestException testException;
	private TestException testExceptionWithMessage;

	@Before
	public void setup() {
		mapper = new ObjectMapper();
		baos = new ByteArrayOutputStream();
		testException = new TestException();
		testExceptionWithMessage = new TestException("exception message");
	}

	@Test
	public void exceptionWithoutAnnotatedServiceInterface() throws Exception {
		JsonRpcServer jsonRpcServer = new JsonRpcServer(mapper, new Service(), ServiceInterfaceWithoutAnnotation.class);
		jsonRpcServer.handle(new ClassPathResource(JSON_FILE).getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);
		JsonNode error = json.get("error");

		assertNotNull(error);
		assertEquals(0, error.get("code").intValue());
	}

	@Test
	public void exceptionWithAnnotatedServiceInterface() throws Exception {
		JsonRpcServer jsonRpcServer = new JsonRpcServer(mapper, new Service(), ServiceInterfaceWithAnnotation.class);
		jsonRpcServer.handle(new ClassPathResource(JSON_FILE).getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);
		JsonNode error = json.get("error");

		assertNotNull(error);
		assertEquals(1234, error.get("code").intValue());
		assertEquals(null, error.get("message").textValue());
		assertNotNull(error.get("data"));
		JsonNode data = error.get("data");
		assertEquals(null, data.get("message").textValue());
		assertEquals(TestException.class.getName(), data.get("exceptionTypeName").textValue());
	}

	@Test
	public void exceptionWithAnnotatedServiceInterfaceMessageAndData() throws Exception {
		JsonRpcServer jsonRpcServer = new JsonRpcServer(mapper, new Service(), ServiceInterfaceWithAnnotationMessageAndData.class);
		jsonRpcServer.handle(new ClassPathResource(JSON_FILE).getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);
		JsonNode error = json.get("error");

		assertNotNull(error);
		assertEquals(-5678, error.get("code").intValue());
		assertEquals("The message", error.get("message").textValue());
		assertNotNull(error.get("data"));
		JsonNode data = error.get("data");
		assertEquals("The message", data.get("message").textValue());
		assertEquals(TestException.class.getName(), data.get("exceptionTypeName").textValue());
	}
	
	@Test
	public void exceptionWithMsgInException() throws Exception {
		JsonRpcServer jsonRpcServer = new JsonRpcServer(mapper, new ServiceWithExceptionMsg(), ServiceInterfaceWithAnnotation.class);
		jsonRpcServer.handle(new ClassPathResource(JSON_FILE).getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);
		JsonNode error = json.get("error");

		assertNotNull(error);
		assertEquals(1234, error.get("code").intValue());
		assertEquals(testExceptionWithMessage.getMessage(), error.get("message").textValue());
		assertNotNull(error.get("data"));
		JsonNode data = error.get("data");
		assertEquals(testExceptionWithMessage.getMessage(), data.get("message").textValue());
		assertEquals(TestException.class.getName(), data.get("exceptionTypeName").textValue());
	}

	private interface ServiceInterfaceWithoutAnnotation {
		public Object testMethod();
	}

	private interface ServiceInterfaceWithAnnotation {
		@JsonRpcErrors({@JsonRpcError(exception=TestException.class, code=1234) })
		public Object testMethod();
	}

	private interface ServiceInterfaceWithAnnotationMessageAndData {
		@JsonRpcErrors({@JsonRpcError(exception=TestException.class, code=-5678,
				message="The message", data="The data") })
				public Object testMethod();
	}

	private class Service implements ServiceInterfaceWithoutAnnotation,
	ServiceInterfaceWithAnnotation, ServiceInterfaceWithAnnotationMessageAndData {
		public Object testMethod() {
			throw testException;    
		}
	}
	
	private class ServiceWithExceptionMsg implements ServiceInterfaceWithoutAnnotation,
	ServiceInterfaceWithAnnotation, ServiceInterfaceWithAnnotationMessageAndData {
		public Object testMethod() {
			throw testExceptionWithMessage;    
		}
	}

	public class TestException extends RuntimeException {
				
		private static final long serialVersionUID = 1L;
		
		public TestException() {}
		
		public TestException(String msg) {
			super(msg);
		}
	}
}
