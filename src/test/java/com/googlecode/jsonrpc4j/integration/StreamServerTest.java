package com.googlecode.jsonrpc4j.integration;

import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.StreamServer;
import com.googlecode.jsonrpc4j.StreamServer.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static com.googlecode.jsonrpc4j.util.Util.DEFAULT_LOCAL_HOSTNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StreamServerTest {
	
	private static final Logger logger = LoggerFactory.getLogger(StreamServerTest.class);
	private ServerSocket serverSocket;
	private JsonRpcBasicServer jsonRpcServer;
	private JsonRpcClient jsonRpcClient;
	private ServiceImpl service;

	@BeforeEach
	public void setUp() throws Exception {
		serverSocket = ServerSocketFactory.getDefault().createServerSocket(0, 0, InetAddress.getByName(DEFAULT_LOCAL_HOSTNAME));
		service = new ServiceImpl();
		jsonRpcServer = new JsonRpcBasicServer(service, Service.class);
		jsonRpcClient = new JsonRpcClient();
	}
	
	@Test
	public void testBasicConnection() throws Exception {
		
		StreamServer streamServer = createAndStartServer();
		Socket socket = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());
		Service client = ProxyUtil.createClientProxy(this.getClass().getClassLoader(), Service.class, jsonRpcClient, socket);
		for (int i = 0; i < 100; i++) {
			assertEquals(i, client.inc());
		}
		assertEquals("hello dude", client.hello("dude"));
		socket.close();
		streamServer.stop();
	}
	
	private StreamServer createAndStartServer() {
		StreamServer streamServer = new StreamServer(jsonRpcServer, 5, serverSocket);
		streamServer.start();
		return streamServer;
	}
	
	@Test
	public void testMultipleClients() throws Exception {
		StreamServer streamServer = createAndStartServer();
		CreateClients createClients = new CreateClients().invoke();
		Service[] services = createClients.getServices();
		Socket[] sockets = createClients.getSockets();
		
		for (Service service2 : services) {
			for (int j = 0; j < 100; j++) {
				assertEquals(j, service2.inc());
			}
			service2.reset();
		}
		
		for (Service service1 : services) {
			assertEquals("hello dude", service1.hello("dude"));
		}
		
		stopClients(sockets);
		streamServer.stop();
	}
	
	private void stopClients(Socket[] sockets) throws IOException {
		for (Socket socket : sockets) {
			socket.close();
		}
	}
	
	@Test
	public void testStopWhileClientsWorking() throws Exception {
		
		StreamServer streamServer = createAndStartServer();
		Socket socket = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());
		final Service service1 = ProxyUtil.createClientProxy(this.getClass().getClassLoader(), Service.class, jsonRpcClient, socket);
		
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				// noinspection InfiniteLoopStatement
				while (true) {
					service1.inc();
				}
				
			}
		});
		t.start();
		
		while (service.val < 10) {
			Thread.yield();
		}
		streamServer.stop();
	}
	
	@Test
	public void testClientDisconnectsCausingExceptionOnServer() throws Exception {
		StreamServer streamServer = createAndStartServer();
		final Socket socket = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());
		final Service service1 = ProxyUtil.createClientProxy(this.getClass().getClassLoader(), Service.class, jsonRpcClient, socket);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					if (service1.inc() > 5) {
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
		
		while (streamServer.getNumberOfConnections() > 0) {
			Thread.yield();
		}
		assertEquals(0, server.getNumberOfErrors());
		streamServer.stop();
	}
	
	// @Test
	// Separating invoke() and readResponse() calls #20
	// this just isn't going to work with jackson
	@SuppressWarnings("unused")
	public void testMultipleClientCallsBeforeReadResponse() throws Throwable {
		StreamServer streamServer = createAndStartServer();
		Socket socket = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());
		
		InputStream ips = socket.getInputStream();
		OutputStream ops = socket.getOutputStream();
		
		for (int i = 0; i < 10; i++) {
			jsonRpcClient.invoke("inc", null, ops);
		}
		for (int i = 0; i < 10; i++) {
			Integer value = jsonRpcClient.readResponse(Integer.class, ips);
			assertEquals(i, value.intValue());
		}
		socket.close();
		while (streamServer.getNumberOfConnections() > 0) {
			Thread.yield();
		}
		streamServer.stop();
	}
	
	@SuppressWarnings("WeakerAccess")
	public interface Service {
		String hello(String whatever);
		
		int inc();
		
		void reset();
	}
	
	@SuppressWarnings("WeakerAccess")
	public static class ServiceImpl implements Service {
		
		private int val;
		
		public String hello(String whatever) {
			logger.info("server: hello({})", whatever);
			return "hello " + whatever;
		}
		
		public int inc() {
			logger.info("server: inc():", val);
			return val++;
		}
		
		public void reset() {
			val = 0;
		}
		
	}
	
	private class CreateClients {
		private Service[] services;
		private Socket[] sockets;
		
		Service[] getServices() {
			return services;
		}
		
		Socket[] getSockets() {
			return sockets;
		}
		
		CreateClients invoke() throws IOException {
			services = new Service[5];
			sockets = new Socket[5];
			for (int i = 0; i < services.length; i++) {
				sockets[i] = new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort());
				services[i] = ProxyUtil.createClientProxy(this.getClass().getClassLoader(), Service.class, jsonRpcClient, sockets[i]);
			}
			return this;
		}
	}
}
