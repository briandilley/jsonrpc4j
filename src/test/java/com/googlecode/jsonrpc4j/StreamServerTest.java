package com.googlecode.jsonrpc4j;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;

import org.junit.Before;
import org.junit.Test;

import com.googlecode.jsonrpc4j.StreamServer.Server;

public class StreamServerTest {

	private ServerSocket serverSocket;
	private JsonRpcBasicServer jsonRpcServer;
	private JsonRpcClient jsonRpcClient;
	private ServiceImpl service;

	@Before
	public void setUp()
		throws Exception {
		serverSocket = ServerSocketFactory.getDefault().createServerSocket(0, 0, InetAddress.getByName("127.0.0.1"));
		service = new ServiceImpl();
		jsonRpcServer = new JsonRpcBasicServer(service, Service.class);
		jsonRpcClient = new JsonRpcClient();
	}

	@Test
	public void testBasicConnection()
		throws Exception {

		// create and start the server
		StreamServer streamServer = new StreamServer(jsonRpcServer, 5, serverSocket);
		streamServer.start();

		// create socket
		Socket socket = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());

		// create and connect with a client
		Service service1 = ProxyUtil.createClientProxy(
			this.getClass().getClassLoader(), Service.class,
			jsonRpcClient, socket);

		// invoke
		for (int i=0; i<100; i++) {
			assertEquals(i, service1.inc());
		}

		assertEquals("hello dude", service1.hello("dude"));

		// disconnect
		socket.close();

		// stop it
		streamServer.stop();
	}

	@Test
	public void testMultipleClients()
		throws Exception {

		// create and start the server
		StreamServer streamServer = new StreamServer(jsonRpcServer, 5, serverSocket);
		streamServer.start();

		// create clients
		Service[] services = new Service[5];
		Socket[] sockets = new Socket[5];
		for (int i=0; i<services.length; i++) {
			sockets[i] = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());
			services[i] = ProxyUtil.createClientProxy(
				this.getClass().getClassLoader(), Service.class,
				jsonRpcClient, sockets[i]);
		}

		// invoke
		for (int i=0; i<services.length; i++) {
			for (int j=0; j<100; j++) {
				assertEquals(j, services[i].inc());
			}
			services[i].reset();
		}

		// invoke
		for (int i=0; i<services.length; i++) {
			assertEquals("hello dude", services[i].hello("dude"));
		}

		// disconnect clients
		for (int i=0; i<sockets.length; i++) {
			sockets[i].close();
		}

		// stop it
		streamServer.stop();
	}

	@Test
	public void testStopWhileClientsWorking()
		throws Exception {

		// create and start the server
		StreamServer streamServer = new StreamServer(jsonRpcServer, 5, serverSocket);
		streamServer.start();

		// create clients
		Socket socket = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());

		// create and connect with a client
		final Service service1 = ProxyUtil.createClientProxy(
			this.getClass().getClassLoader(), Service.class,
			jsonRpcClient, socket);

		// start a client
		Thread t = new Thread(new Runnable() {
			public void run() {
				while (true) {
					service1.inc();
				}
			}
		});
		t.start();

		while (service.val<10) {
			Thread.yield();
		}

		// stop it
		streamServer.stop();
	}

	@Test
	public void testClientDisconnectsCausingExceptionOnServer()
		throws Exception {

		// create and start the server
		StreamServer streamServer = new StreamServer(jsonRpcServer, 5, serverSocket);
		streamServer.start();

		// create clients
		final Socket socket = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());

		// create and connect with a client
		final Service service1 = ProxyUtil.createClientProxy(
			this.getClass().getClassLoader(), Service.class,
			jsonRpcClient, socket);

		// start a client
		Thread t = new Thread(new Runnable() {
			public void run() {
				while (true) {
					if (service1.inc()>5) {
						try {
							socket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						return;
					}
				}
			}
		});

		Thread.sleep(1000);
		assertEquals(1, streamServer.getNumberOfConnections());
		Server server = streamServer.getServers().iterator().next();
		assertNotNull(server);
		t.start();

		// for the client to invoke something
		while (streamServer.getNumberOfConnections()>0) {
			Thread.yield();
		}

		// make sure no exception was logged
		assertEquals(0, server.getNumberOfErrors());

		// stop it
		streamServer.stop();
	}

	// @Test
	// Separating invoke() and readResponse() calls #20
	// this just isn't going to work with jackson
	public void testMultipleClientCallsBeforeReadResponse()
		throws Throwable {

		// create and start the server
		StreamServer streamServer = new StreamServer(jsonRpcServer, 5, serverSocket);
		streamServer.start();

		// create socket
		Socket socket = new Socket(
				serverSocket.getInetAddress(),
				serverSocket.getLocalPort());

		InputStream ips = socket.getInputStream();
		OutputStream ops = socket.getOutputStream();

		for (int i=0; i<10; i++) {
			jsonRpcClient.invoke("inc", null, ops);
		}
		for (int i=0; i<10; i++) {
			Integer value = jsonRpcClient.readResponse(Integer.class, ips);
			assertEquals(i, value.intValue());
		}

		// for the client to invoke something
		socket.close();
		while (streamServer.getNumberOfConnections()>0) {
			Thread.yield();
		}

		// stop it
		streamServer.stop();
	}

	private static interface Service {
		String hello(String whatever);
		int inc();
		void reset();
	}

	private class ServiceImpl implements Service {
		private final Logger LOGGER = Logger.getLogger(ServiceImpl.class.getName());
		private int val;

		public String hello(String whatever) {
			LOGGER.log(Level.INFO, "server: hello("+whatever+")");
			return "hello "+whatever;
		}

		public int inc() {
			LOGGER.log(Level.INFO, "server: inc():"+val);
			return val++;
		}

		public void reset() {
			val = 0;
		}
		
	}

}
