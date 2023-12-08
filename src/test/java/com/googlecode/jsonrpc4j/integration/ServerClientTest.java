package com.googlecode.jsonrpc4j.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.RequestInterceptor;
import com.googlecode.jsonrpc4j.util.LocalThreadServer;
import com.googlecode.jsonrpc4j.util.TestThrowable;
import org.easymock.EasyMock;
import org.easymock.EasyMockExtension;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.googlecode.jsonrpc4j.util.Util.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EasyMockExtension.class)
public class ServerClientTest {
	

	@Mock(type = MockType.NICE)
	private Service mockService;
	
	private Service client;
	private LocalThreadServer<Service> server;

	@BeforeEach
	public void setUp() throws Exception {
		server = new LocalThreadServer<>(mockService, Service.class);
		client = server.client(Service.class);
	}

	@AfterEach
	public void tearDown() throws Exception {
		server.close();
	}
	
	@Test
	public void allMethods() throws Throwable {
		testCommon(client);
	}
	
	private void testCommon(Service client) throws Throwable {
		client.noOp();
		EasyMock.expect(mockService.hello()).andReturn(param1);
		EasyMock.expect(mockService.hello(nonAsciiCharacters)).andReturn(nonAsciiCharacters);
		
		EasyMock.replay(mockService);
		client.noOp();
		assertEquals(param1, client.hello());
		assertEquals(nonAsciiCharacters, client.hello(nonAsciiCharacters));
		assertNotNull(client.toString());
		// noinspection ResultOfMethodCallIgnored
		client.hashCode();
		EasyMock.verify(mockService);
	}
	
	@Test
	public void testAllMethodsViaCompositeProxy() throws Throwable {
		Object compositeService = ProxyUtil.createCompositeServiceProxy(ClassLoader.getSystemClassLoader(), new Object[]{client},
				new Class<?>[]{Service.class}, true);
		Service clientService = (Service) compositeService;
		testCommon(clientService);
	}
	
	@Test
	public void testNoArgFuncCallException() throws Throwable {
		final String message = "testing";
		EasyMock.expect(mockService.hello()).andThrow(new TestThrowable(message));
		EasyMock.replay(mockService);
		assertThrows(TestThrowable.class, () -> client.hello(), message);
		EasyMock.verify(mockService);
	}
	
	@Test
	public void testInterceptorDoingNothingCalled() throws Throwable {
		EasyMock.expect(mockService.hello()).andReturn(param1);
		RequestInterceptor interceptorMock = EasyMock.mock(RequestInterceptor.class);
		interceptorMock.interceptRequest(EasyMock.anyObject(JsonNode.class));
		EasyMock.expectLastCall().andVoid();
		server.setRequestInterceptor(interceptorMock);
		EasyMock.replay(interceptorMock, mockService);
		assertEquals(param1, client.hello());
		EasyMock.verify(interceptorMock, mockService);
	}

	@Disabled
	@Test
	public void testInterceptorRaisesException() throws Throwable {
		EasyMock.expect(mockService.hello()).andReturn(param1);
		RequestInterceptor interceptorMock = EasyMock.mock(RequestInterceptor.class);
		interceptorMock.interceptRequest(EasyMock.anyObject(JsonNode.class));
		EasyMock.expectLastCall().andThrow(new TestThrowable(param3));
		server.setRequestInterceptor(interceptorMock);
		EasyMock.replay(interceptorMock, mockService);
		assertThrows(TestThrowable.class, () -> client.hello(), param3);
		EasyMock.verify(interceptorMock, mockService);
	}
	
	@Test
	public void testOneArgFuncCallException() throws Throwable {
		final String name = "uranus";
		final String message = name + " testing";
		EasyMock.expect(mockService.hello(name)).andThrow(new TestThrowable(message));
		EasyMock.replay(mockService);
		assertThrows(TestThrowable.class, () -> client.hello(name), message);
		EasyMock.verify(mockService);
	}
	
	@Test
	public void undeclaredException() {
		final String message = "testing";
		mockService.undeclaredExceptionThrown();
		EasyMock.expectLastCall().andThrow(new IllegalArgumentException(message));
		EasyMock.replay(mockService);
		assertThrows(IllegalArgumentException.class, () -> client.undeclaredExceptionThrown(), message);
	}
	
	@Test
	public void unresolvedException() throws Throwable {
		final String message = "testing";
		mockService.unresolvedExceptionThrown();
		EasyMock.expectLastCall().andThrow(new IllegalArgumentException(message));
		EasyMock.replay(mockService);
		assertThrows(IllegalArgumentException.class, () -> client.unresolvedExceptionThrown(), message);
	}
	
	public interface Service {
		void noOp();
		
		String hello() throws Throwable;
		
		String hello(String world) throws Throwable;
		
		void unresolvedExceptionThrown() throws Throwable;
		
		void undeclaredExceptionThrown();
	}
	
}
