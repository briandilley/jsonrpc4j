package com.googlecode.jsonrpc4j.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.RequestInterceptor;
import com.googlecode.jsonrpc4j.util.LocalThreadServer;
import com.googlecode.jsonrpc4j.util.TestThrowable;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static com.googlecode.jsonrpc4j.util.Util.nonAsciiCharacters;
import static com.googlecode.jsonrpc4j.util.Util.param1;
import static com.googlecode.jsonrpc4j.util.Util.param3;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(EasyMockRunner.class)
public class ServerClientTest {
	
	@Rule
	public final ExpectedException expectedEx = ExpectedException.none();
	
	@Mock(type = MockType.NICE)
	private Service mockService;
	
	private Service client;
	private LocalThreadServer<Service> server;
	
	@Before
	public void setUp() throws Exception {
		server = new LocalThreadServer<>(mockService, Service.class);
		client = server.client(Service.class);
	}
	
	@After
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
		expectedEx.expectMessage(message);
		expectedEx.expect(TestThrowable.class);
		client.hello();
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
	
	@Test
	public void testInterceptorRaisesException() throws Throwable {
		EasyMock.expect(mockService.hello()).andReturn(param1);
		RequestInterceptor interceptorMock = EasyMock.mock(RequestInterceptor.class);
		interceptorMock.interceptRequest(EasyMock.anyObject(JsonNode.class));
		EasyMock.expectLastCall().andThrow(new TestThrowable(param3));
		server.setRequestInterceptor(interceptorMock);
		expectedEx.expectMessage(param3);
		expectedEx.expect(TestThrowable.class);
		EasyMock.replay(interceptorMock, mockService);
		client.hello();
		EasyMock.verify(interceptorMock, mockService);
	}
	
	@Test
	public void testOneArgFuncCallException() throws Throwable {
		final String name = "uranus";
		final String message = name + " testing";
		EasyMock.expect(mockService.hello(name)).andThrow(new TestThrowable(message));
		EasyMock.replay(mockService);
		expectedEx.expectMessage(message);
		expectedEx.expect(TestThrowable.class);
		client.hello(name);
		EasyMock.verify(mockService);
	}
	
	@Test
	public void undeclaredException() {
		final String message = "testing";
		mockService.undeclaredExceptionThrown();
		EasyMock.expectLastCall().andThrow(new IllegalArgumentException(message));
		EasyMock.replay(mockService);
		expectedEx.expect(IllegalArgumentException.class);
		expectedEx.expectMessage(message);
		client.undeclaredExceptionThrown();
	}
	
	@Test
	public void unresolvedException() throws Throwable {
		final String message = "testing";
		mockService.unresolvedExceptionThrown();
		EasyMock.expectLastCall().andThrow(new IllegalArgumentException(message));
		EasyMock.replay(mockService);
		expectedEx.expect(IllegalArgumentException.class);
		expectedEx.expectMessage(message);
		client.unresolvedExceptionThrown();
	}
	
	public interface Service {
		void noOp();
		
		String hello() throws Throwable;
		
		String hello(String world) throws Throwable;
		
		void unresolvedExceptionThrown() throws Throwable;
		
		void undeclaredExceptionThrown();
	}
	
}
