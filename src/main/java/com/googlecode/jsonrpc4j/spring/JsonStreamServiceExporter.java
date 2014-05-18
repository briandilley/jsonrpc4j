/*
The MIT License (MIT)

Copyright (c) 2014 jsonrpc4j

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package com.googlecode.jsonrpc4j.spring;

import java.net.InetAddress;
import java.net.ServerSocket;

import javax.net.ServerSocketFactory;

import org.springframework.beans.factory.DisposableBean;

import com.googlecode.jsonrpc4j.StreamServer;

/**
 * {@link RemoteExporter} that exports services using Json
 * according to the JSON-RPC proposal specified at:
 * <a href="http://groups.google.com/group/json-rpc">
 * http://groups.google.com/group/json-rpc</a>.
 *
 */
public class JsonStreamServiceExporter
	extends AbstractJsonServiceExporter
	implements DisposableBean {

	public static final int DEFAULT_MAX_THREADS			= 50;
	public static final int DEFAULT_PORT				= 10420;
	public static final int DEFAULT_BACKLOG				= 0;
	public static final int DEFAULT_MAX_CLIENT_ERRORS	= 5;
	public static final String DEFAULT_HOSTNAME			= "0.0.0.0";

	private ServerSocketFactory serverSocketFactory;
	private int maxThreads		= DEFAULT_MAX_THREADS;
	private int port			= DEFAULT_PORT;
	private int backlog			= DEFAULT_BACKLOG;
	private int maxClientErrors	= DEFAULT_MAX_CLIENT_ERRORS;
	private String hostName		= DEFAULT_HOSTNAME;
	
	private StreamServer streamServer;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void exportService()
		throws Exception {

		// create a stream server if needed
		if (streamServer==null) {
	
			// make sure we have a factory
			if (serverSocketFactory==null) {
				serverSocketFactory = ServerSocketFactory.getDefault();
			}
	
			// create server socket
			ServerSocket serverSocket = serverSocketFactory
				.createServerSocket(port, backlog, InetAddress.getByName(hostName));
	
			// create the stream server
			streamServer = new StreamServer(getJsonRpcServer(), maxThreads, serverSocket);
			streamServer.setMaxClientErrors(maxClientErrors);
		}

		// start it
		streamServer.start();
	}

	/**
	 * {@inheritDoc}
	 */
	public void destroy()
		throws Exception {
		streamServer.stop();
	}

	/**
	 * @param serverSocketFactory the serverSocketFactory to set
	 */
	public void setServerSocketFactory(ServerSocketFactory serverSocketFactory) {
		this.serverSocketFactory = serverSocketFactory;
	}

	/**
	 * @param maxThreads the maxThreads to set
	 */
	public void setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @param backlog the backlog to set
	 */
	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}

	/**
	 * @param hostName the hostName to set
	 */
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	/**
	 * @param streamServer the streamServer to set
	 */
	public void setStreamServer(StreamServer streamServer) {
		this.streamServer = streamServer;
	}

	/**
	 * @param maxClientErrors the maxClientErrors to set
	 */
	public void setMaxClientErrors(int maxClientErrors) {
		this.maxClientErrors = maxClientErrors;
	}

}
