package com.googlecode.jsonrpc4j;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;

import org.junit.Before;
import org.junit.Test;

public class StreamServerTest {

	private ServerSocket serverSocket;
	private JsonRpcServer jsonRpcServer;
	private JsonRpcClient jsonRpcClient;

	@Before
	public void setUp()
		throws Exception {
		serverSocket = ServerSocketFactory.getDefault().createServerSocket(0, 0, InetAddress.getByName("127.0.0.1"));
		jsonRpcServer = new JsonRpcServer(new ServiceImpl(), Service.class);
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

		// for the client to invoke something
		while (service1.inc()<10) {
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
