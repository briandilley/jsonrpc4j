package com.googlecode.jsonrpc4j;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLException;

/**
 * A multi-threaded streaming server that uses JSON-RPC
 * over sockets.
 *
 */
public class StreamServer {

	private static final Logger LOGGER = Logger.getLogger(StreamServer.class.getName());

	private static final long SERVER_SOCKET_SO_TIMEOUT	= 5000;

	private ThreadPoolExecutor executor;
	private ServerSocket serverSocket;
	private JsonRpcServer jsonRpcServer;
	private int maxClientErrors = 5;

	private AtomicBoolean isStarted 	= new AtomicBoolean(false);
	private AtomicBoolean keepRunning 	= new AtomicBoolean(false);
	private Set<Server> servers			= new HashSet<Server>();

	/**
	 * Creates a {@code StreamServer} with the given max number
	 * of threads using the given {@link ServerSocket} to listen
	 * for client connections.
	 * 
	 * @param jsonRpcServer the {@link JsonRpcServer} that will handle requests
	 * @param maxThreads the mac number of threads the server will spawn
	 * @param serverSocket the {@link ServerSocket} used for accepting client connections
	 */
	public StreamServer(
		JsonRpcServer jsonRpcServer, int maxThreads, ServerSocket serverSocket) {

		// initialize values
		this.jsonRpcServer		= jsonRpcServer;
		this.serverSocket		= serverSocket;

		// create the executor server
		executor = new ThreadPoolExecutor(
			maxThreads+1, maxThreads+1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

		// we can't allow the server to re-throw exceptions
		jsonRpcServer.setRethrowExceptions(false);
	}

	/**
	 * Returns the current servers.
	 * @return the servers
	 */
	public Set<Server> getServers() {
		return Collections.unmodifiableSet(servers);
	}

	/**
	 * Creates a {@code StreamServer} with the given max number
	 * of threads.  A {@link ServerSocket} is created using the
	 * default {@link ServerSocketFactory} that listes on the
	 * given {@code port} and {@link InetAddress}.
	 * 
	 * @param jsonRpcServer the {@link JsonRpcServer} that will handle requests
	 * @param maxThreads the mac number of threads the server will spawn
	 * @param port the port to listen on
	 * @param backlog the {@link ServerSocket} backlog
	 * @param bindAddress the address to listen on
	 * @throws IOException on error
	 */
	public StreamServer(
		JsonRpcServer jsonRpcServer, int maxThreads,
		int port, int backlog, InetAddress bindAddress)
		throws IOException {
		this(jsonRpcServer, maxThreads,
			ServerSocketFactory.getDefault().createServerSocket(port, backlog, bindAddress));
	}

	/**
	 * Starts the server.
	 */
	public void start() {

		// make sure we're not already started
		if (!isStarted.compareAndSet(false, true)) {
			throw new IllegalStateException(
				"The StreamServer is already started");
		}

		// we're starting
		LOGGER.log(Level.INFO,
			"StreamServer starting "
			+serverSocket.getInetAddress()
			+":"+serverSocket.getLocalPort());

		// start the server
		keepRunning.set(true);
		executor.submit(new Server());
	}

	/**
	 * Stops the server thread.
	 * @throws InterruptedException if a graceful shutdown didn't happen
	 */
	public void stop()
		throws InterruptedException {

		// make sure we're started
		if (!isStarted.get()) {
			throw new IllegalStateException(
				"The StreamServer is not started");
		}

		// stop the server
		keepRunning.set(false);

		// wait for the clients to stop
		executor.shutdownNow();

		try {
			serverSocket.close();
		} catch (IOException e) { /* no-op */ }

		try {

			// wait for it to finish
			if (!executor.isTerminated()) {
				executor.awaitTermination(
					2000 + SERVER_SOCKET_SO_TIMEOUT, TimeUnit.MILLISECONDS);
			}

			// set the flags
			isStarted.set(false);
			keepRunning.set(false);
			
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, "InterruptedException while waiting for termination", e);
			throw e;
		}
	}

	/**
	 * Server thread.
	 */
	public class Server
		implements Runnable {

		private int errors;
		private Throwable lastException;

		public int getNumberOfErrors() {
			return errors;
		}

		public Throwable getLastException() {
			return lastException;
		}

		/**
		 * {@inheritDoc}
		 */
		public void run() {

			// get the server socket
			ServerSocket serverSocket = StreamServer.this.serverSocket;

			// start the listening loop
			Socket clientSocket = null;
			while (StreamServer.this.keepRunning.get()) {
				try {
					// wait for a connection
					serverSocket.setSoTimeout((int)SERVER_SOCKET_SO_TIMEOUT);
					clientSocket = serverSocket.accept();

					// log the connection
					LOGGER.log(Level.INFO, "Client connected: "
							+clientSocket.getInetAddress().getHostAddress()
							+":"+clientSocket.getPort());

					// spawn a new Server for the next connection
					// and break out of the server loop
					executor.submit(new Server());
					break;

				} catch (SocketTimeoutException e) {
					// this is expected because of so_timeout

				} catch(SSLException ssle) {
					LOGGER.log(Level.SEVERE, "SSLException while listening for clients, terminating", ssle);
					break;
					
				} catch(IOException ioe) {
					// this could be because the ServerSocket was closed
					if (SocketException.class.isInstance(ioe) && !keepRunning.get()) {
						break;
					}
					LOGGER.log(Level.SEVERE, "Exception while listening for clients", ioe);
				}
			}

			// handle the request
			// get the streams
			InputStream input;
			OutputStream output;
			try {
				input = clientSocket.getInputStream();
				output = clientSocket.getOutputStream();
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Client socket failed", e);
				return;
			}

			// keep handling requests
			servers.add(this);
			try {
				while (StreamServer.this.keepRunning.get()) {
	
					// handle it
					try {
						jsonRpcServer.handle(input, output);
					} catch (Throwable t) {
						// client disconnected, don't count this error
						if (clientSocket.isClosed()
								|| availableQuietly(input)==0) {
							LOGGER.log(Level.INFO, "Client disconnected: "
									+clientSocket.getInetAddress().getHostAddress()
									+":"+clientSocket.getPort());
							break;
						}
						errors++;
						lastException = t;
						if (errors<maxClientErrors) {
							LOGGER.log(Level.SEVERE, "Exception while handling request", t);
						} else {
							LOGGER.log(Level.SEVERE, "Closing client connection due to repeated errors", t);
							break;
						}
					}
				}
			} finally {
				servers.remove(this);
				closeQuietly(clientSocket);
				closeQuietly(input);
				closeQuietly(output);
			}
		}
	}

	/**
	 * Returns the number of available bytes quietly.
	 * @param ips
	 * @return
	 */
	private int availableQuietly(InputStream ips) {
		int avail = 0;
		if (ips!=null) {
			try {
				avail = ips.available();
			} catch(Throwable t) {
				LOGGER.log(Level.FINE, "Input threw exception", t);
			}
		}
		return avail;
	}

	/**
	 * Closes something quietly.
	 * @param c
	 */
	private void closeQuietly(Closeable c) {
		if (c!=null) {
			try {
				c.close();
			} catch(Throwable t) {
				LOGGER.log(Level.WARNING, "Error closing, ignoring", t);
			}
		}
	}

	/**
	 * @return the number of connected clients
	 */
	public int getNumberOfConnections() {
		return servers.size();
	}

	/**
	 * @return the maxClientErrors
	 */
	public int getMaxClientErrors() {
		return maxClientErrors;
	}

	/**
	 * @param maxClientErrors the maxClientErrors to set
	 */
	public void setMaxClientErrors(int maxClientErrors) {
		this.maxClientErrors = maxClientErrors;
	}

	/**
	 * @return the isStarted
	 */
	public boolean isStarted() {
		return isStarted.get();
	}

}
