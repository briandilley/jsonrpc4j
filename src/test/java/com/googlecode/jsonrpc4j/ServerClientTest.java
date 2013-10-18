package com.googlecode.jsonrpc4j;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class ServerClientTest {

	private static Mockery mockCtx = new JUnit4Mockery();
	private static Service serviceMock = mockCtx.mock(Service.class);

	private ClassLoader cl;
	private JsonRpcServer jsonRpcServer;
	private JsonRpcClient jsonRpcClient;
	
	private ServerThread serverThread;
	private PipedInputStream clientInputStream;
	private PipedOutputStream clientOutputStream;
	private PipedInputStream serverInputStream;
	private PipedOutputStream serverOutputStream;

	@Before
	public void setUp()
		throws Exception {
		cl = ClassLoader.getSystemClassLoader();

		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.ALL);
		Logger.getLogger("").addHandler(handler);
		Logger.getLogger(JsonRpcServer.class.getName()).setLevel(Level.ALL);
		Logger.getLogger(JsonRpcClient.class.getName()).setLevel(Level.ALL);
		jsonRpcServer = new JsonRpcServer(serviceMock, Service.class);
		jsonRpcClient = new JsonRpcClient();

		// create streams
		clientInputStream = new PipedInputStream();
		serverOutputStream = new PipedOutputStream(clientInputStream);
		serverInputStream = new PipedInputStream();
		clientOutputStream = new PipedOutputStream(serverInputStream);

		// start server
		serverThread = new ServerThread(serverInputStream, serverOutputStream, jsonRpcServer);
		serverThread.start();
		serverThread.waitForStart();
	}

	@After
	public void tearDown()
		throws Exception {
		serverThread.stopServer();
	}

	@Test
	public void testAllMethods()
		throws Throwable {

		// create client service
		Service clientService = ProxyUtil.createClientProxy(
			cl, Service.class, false, jsonRpcClient,
			clientInputStream,
			clientOutputStream);

		mockCtx.checking(new Expectations() {{
			one(serviceMock).noOp();
			one(serviceMock).hello();
			will(returnValue("world"));
			one(serviceMock).hello(with("uranus"));
			will(returnValue("uranus"));
		}});

		// call it
		clientService.noOp();
		assertEquals("world", clientService.hello());
		assertEquals("uranus", clientService.hello("uranus"));

		// call non-rpc methods
		assertNotNull(clientService.toString());
		assertTrue(clientService.equals(clientService));
		assertFalse(clientService.equals(null));
		clientService.hashCode();
	}

	@Test
	public void testAllMethodsViaCompositeProxy()
			throws Throwable {

		// create client service
		Service clientService = ProxyUtil.createClientProxy(
				cl, Service.class, false, jsonRpcClient,
				clientInputStream,
				clientOutputStream);

		Object compositeService = ProxyUtil.createCompositeServiceProxy(
				cl,
				new Object[] { clientService },
				new Class<?>[] { Service.class },
				true);

		clientService = (Service) compositeService;

		mockCtx.checking(new Expectations() {{
			one(serviceMock).noOp();
			one(serviceMock).hello();
			will(returnValue("world"));
			one(serviceMock).hello(with("uranus"));
			will(returnValue("uranus"));
		}});

		// call it
		clientService.noOp();
		assertEquals("world", clientService.hello());
		assertEquals("uranus", clientService.hello("uranus"));

		// call non-rpc methods
		clientService.hashCode();
		assertTrue(clientService.equals(clientService));
		assertFalse(clientService.equals(null));
		assertNotNull(clientService.toString());
	}


	@Test
	public void testException()
		throws Throwable {

		// create client service
		Service clientService = ProxyUtil.createClientProxy(
			cl, Service.class, false, jsonRpcClient,
			clientInputStream,
			clientOutputStream);

		mockCtx.checking(new Expectations() {{
			one(serviceMock).noOp();
			will(throwException(new TestException("testing")));
			one(serviceMock).hello();
			will(throwException(new TestException(null)));
			one(serviceMock).hello(with("uranus"));
			will(throwException(new TestException2()));
		}});

		try {
			clientService.noOp();
			fail("Expecting exception");
		} catch(Throwable t) {
			assertEquals("testing", t.getMessage());
			assertTrue(TestException.class.isInstance(t));
		}

		try {
			clientService.hello();
			fail("Expecting exception");
		} catch(Throwable t) {
			assertNull(t.getMessage());
			assertTrue(TestException.class.isInstance(t));
		}

		try {
			clientService.hello("uranus");
			fail("Expecting exception");
		} catch(Throwable t) {
			assertTrue(TestException2.class.isInstance(t));
		}
	}

	@Test
	public void testUnknownException()
		throws Throwable {

		// create client service
		Service clientService = ProxyUtil.createClientProxy(
			cl, Service.class, false, jsonRpcClient,
			clientInputStream,
			clientOutputStream);

		jsonRpcServer.setErrorResolver(AnnotationsErrorResolver.INSTANCE);

		mockCtx.checking(new Expectations() {{
			one(serviceMock).unresolvedExceptionThrown();
			will(throwException(new IllegalArgumentException("testing")));
			one(serviceMock).undelcaredExceptionThrown();
			will(throwException(new IllegalArgumentException("testing")));
		}});

		try {
			clientService.unresolvedExceptionThrown();
			fail("Expecting exception");
		} catch(Throwable t) {
			assertTrue(JsonRpcClientException.class.isInstance(t));
		}

		try {
			clientService.undelcaredExceptionThrown();
			fail("Expecting exception");
		} catch(Throwable t) {
			assertTrue(JsonRpcClientException.class.isInstance(t));
		}

	}

	public interface Service {
		public void noOp() throws Throwable;
		public String hello() throws Throwable;
		public String hello(String world) throws Throwable;
		public void unresolvedExceptionThrown() throws Throwable;
		public void undelcaredExceptionThrown();
	}

	private static class ServerThread
		extends Thread {
		private Object startLock = new Object();
		private InputStream ips;
		private OutputStream ops;
		private JsonRpcServer jsonRpcServer;
		private AtomicBoolean keepRunning = new AtomicBoolean(false);
		public ServerThread(InputStream ips, OutputStream ops, JsonRpcServer jsonRpcServer) {
			this.ips = ips;
			this.ops = ops;
			this.jsonRpcServer = jsonRpcServer;
		}
		@Override
		public void run() {
			keepRunning.set(true);
			while (keepRunning.get()) {
				try {
					if (ips.available()>0) {
						jsonRpcServer.handle(ips, ops);
					}
				} catch(Exception e) {
					e.printStackTrace();
					return;
				}
				synchronized (startLock) {
					startLock.notify();
				}
			}
		}
		public void stopServer() {
			keepRunning.set(false);
		}
		public void waitForStart()
			throws InterruptedException {
			synchronized (startLock) {
				startLock.wait();
			}
		}
	}

}
