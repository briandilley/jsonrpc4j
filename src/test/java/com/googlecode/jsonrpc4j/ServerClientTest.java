package com.googlecode.jsonrpc4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.MockType;

import com.googlecode.jsonrpc4j.util.LocalThreadServer;

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
		final String defaultName = "world";
		EasyMock.expect(mockService.hello()).andReturn(defaultName);
		final String name = "uranus";
		EasyMock.expect(mockService.hello(name)).andReturn(name);

		EasyMock.replay(mockService);
		client.noOp();
		assertEquals(defaultName, client.hello());
		assertEquals(name, client.hello(name));
		assertNotNull(client.toString());
		// noinspection ResultOfMethodCallIgnored
		client.hashCode();
		EasyMock.verify(mockService);
	}

	@Test
	public void testAllMethodsViaCompositeProxy() throws Throwable {
		Object compositeService = ProxyUtil.createCompositeServiceProxy(ClassLoader.getSystemClassLoader(), new Object[] { client },
				new Class<?>[] { Service.class }, true);
		Service clientService = (Service) compositeService;
		testCommon(clientService);
	}

	@Test
	public void testNoArgFuncCallException() throws Throwable {
		final String message = "testing";
		EasyMock.expect(mockService.hello()).andThrow(new TestException(message));
		EasyMock.replay(mockService);
		expectedEx.expectMessage(message);
		expectedEx.expect(TestException.class);
		client.hello();
		EasyMock.verify(mockService);
	}

	@Test
	public void testOneArgFuncCallException() throws Throwable {
		final String name = "uranus";
		final String message = name + " testing";
		EasyMock.expect(mockService.hello(name)).andThrow(new TestException(message));
		EasyMock.replay(mockService);
		expectedEx.expectMessage(message);
		expectedEx.expect(TestException.class);
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
