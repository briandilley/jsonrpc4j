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

package com.googlecode.jsonrpc4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A JSON-RPC request server reads JSON-RPC requests from an
 * input stream and writes responses to an output stream.
 * Supports handlet and servlet requests.
 */
public class JsonRpcServer extends JsonRpcBasicServer
{
	private static final Logger LOGGER = Logger.getLogger(JsonRpcServer.class.getName());
    
	/**
	 * Creates the server with the given {@link ObjectMapper} delegating
	 * all calls to the given {@code handler} {@link Object} but only
	 * methods available on the {@code remoteInterface}.
	 *
	 * @param mapper the {@link ObjectMapper}
	 * @param handler the {@code handler}
	 * @param remoteInterface the interface
	 */
	public JsonRpcServer(
		ObjectMapper mapper, Object handler, Class<?> remoteInterface) {
		super(mapper, handler, remoteInterface);
	}

	/**
	 * Creates the server with the given {@link ObjectMapper} delegating
	 * all calls to the given {@code handler}.
	 *
	 * @param mapper the {@link ObjectMapper}
	 * @param handler the {@code handler}
	 */
	public JsonRpcServer(ObjectMapper mapper, Object handler) {
		super(mapper, handler, null);
	}

	/**
	 * Creates the server with a default {@link ObjectMapper} delegating
	 * all calls to the given {@code handler} {@link Object} but only
	 * methods available on the {@code remoteInterface}.
	 *
	 * @param handler the {@code handler}
	 * @param remoteInterface the interface
	 */
	public JsonRpcServer(Object handler, Class<?> remoteInterface) {
		super(new ObjectMapper(), handler, remoteInterface);
	}

	/**
	 * Creates the server with a default {@link ObjectMapper} delegating
	 * all calls to the given {@code handler}.
	 *
	 * @param handler the {@code handler}
	 */
	public JsonRpcServer(Object handler) {
		super(new ObjectMapper(), handler, null);
	}
        
	/**
	 * Handles a portlet request.
	 *
	 * @param request the {@link ResourceRequest}
	 * @param response the {@link ResourceResponse}
	 * @throws IOException on error
	 */
	public void handle(ResourceRequest request, ResourceResponse response)
		throws IOException {
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.log(Level.FINE, "Handing ResourceRequest "+request.getMethod());
		}

		// set response type
		response.setContentType(JSONRPC_RESPONSE_CONTENT_TYPE);

		// setup streams
		InputStream input 	= null;
		OutputStream output	= response.getPortletOutputStream();

		// POST
		if (request.getMethod().equals("POST")) {
			input = request.getPortletInputStream();

		// GET
		} else if (request.getMethod().equals("GET")) {
			input = createInputStream(
				request.getParameter("method"),
				request.getParameter("id"),
				request.getParameter("params"));

		// invalid request
		} else {
			throw new IOException(
				"Invalid request method, only POST and GET is supported");
		}

		// service the request
		handle(input, output);
	}

	/**
	 * Handles a servlet request.
	 *
	 * @param request the {@link HttpServletRequest}
	 * @param response the {@link HttpServletResponse}
	 * @throws IOException on error
	 */
	public void handle(HttpServletRequest request, HttpServletResponse response)
		throws IOException {
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.log(Level.FINE, "Handing HttpServletRequest "+request.getMethod());
		}

		// set response type
		response.setContentType(JSONRPC_RESPONSE_CONTENT_TYPE);

		// setup streams
		InputStream input 	= null;
		OutputStream output	= response.getOutputStream();

		// POST
		if (request.getMethod().equals("POST")) {
			input = request.getInputStream();

		// GET
		} else if (request.getMethod().equals("GET")) {
			input = createInputStream(
				request.getParameter("method"),
				request.getParameter("id"),
				request.getParameter("params"));

		// invalid request
		} else {
			throw new IOException(
				"Invalid request method, only POST and GET is supported");
		}

		// service the request
		handle(input, output);
	}
}
