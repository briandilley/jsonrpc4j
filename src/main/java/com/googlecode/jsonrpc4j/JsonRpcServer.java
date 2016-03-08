package com.googlecode.jsonrpc4j;

import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.BULK_ERROR;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.CUSTOM_SERVER_ERROR_LOWER;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.CUSTOM_SERVER_ERROR_UPPER;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.ERROR_NOT_HANDLED;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.INTERNAL_ERROR;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.INVALID_REQUEST;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.METHOD_NOT_FOUND;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.METHOD_PARAMS_INVALID;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.PARSE_ERROR;

import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

/**
 * A JSON-RPC request server reads JSON-RPC requests from an input stream and writes responses to an output stream.
 * Supports handler and servlet requests.
 */
@SuppressWarnings("unused")
public class JsonRpcServer extends JsonRpcBasicServer {
	private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

	/**
	 * Creates the server with the given {@link ObjectMapper} delegating
	 * all calls to the given {@code handler} {@link Object} but only
	 * methods available on the {@code remoteInterface}.
	 *
	 * @param mapper the {@link ObjectMapper}
	 * @param handler the {@code handler}
	 * @param remoteInterface the interface
	 */
	public JsonRpcServer(ObjectMapper mapper, Object handler, Class<?> remoteInterface) {
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
	private JsonRpcServer(Object handler, Class<?> remoteInterface) {
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
	public void handle(ResourceRequest request, ResourceResponse response) throws IOException {
		logger.debug("Handing ResourceRequest {}", request.getMethod());
		response.setContentType(JSONRPC_CONTENT_TYPE);
		OutputStream output = response.getPortletOutputStream();
		InputStream input = getRequestStream(request);
		handleRequest(input, output);
		// fix to not flush within handleRequest() but outside so http status code can be set
		output.flush();
	}

	private InputStream getRequestStream(ResourceRequest request) throws IOException {
		if (request.getMethod().equals("POST")) {
			return request.getPortletInputStream();
		} else if (request.getMethod().equals("GET")) {
			return createInputStream(request);
		} else {
			throw new IOException("Invalid request method, only POST and GET is supported");
		}
	}

	private static InputStream createInputStream(ResourceRequest request) throws IOException {
		return createInputStream(request.getParameter(METHOD), request.getParameter(ID), request.getParameter(PARAMS));
	}

	/**
	 * Handles a servlet request.
	 *
	 * @param request the {@link HttpServletRequest}
	 * @param response the {@link HttpServletResponse}
	 * @throws IOException on error
	 */
	public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.debug("Handling HttpServletRequest {}", request);
		response.setContentType(JSONRPC_CONTENT_TYPE);
		OutputStream output = response.getOutputStream();
		InputStream input = getRequestStream(request);
		int result = handleRequest(input, output);
		response.setStatus(getHttpStatusCode(response, result));
		output.flush();
	}

	private InputStream getRequestStream(HttpServletRequest request) throws IOException {
		InputStream input;
		if (request.getMethod().equals("POST")) {
			input = request.getInputStream();
		} else if (request.getMethod().equals("GET")) {
			input = createInputStream(request);
		} else {
			throw new IOException("Invalid request method, only POST and GET is supported");
		}
		return input;
	}

	private int getHttpStatusCode(HttpServletResponse response, int result) {
		if (result == 0) return HttpServletResponse.SC_OK;

		if (isErrorCode(result)) {
			return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		} else if (result == INVALID_REQUEST.code || result == PARSE_ERROR.code) {
			return HttpServletResponse.SC_BAD_REQUEST;
		} else if (result == METHOD_NOT_FOUND.code) { return HttpServletResponse.SC_NOT_FOUND; }

		return HttpServletResponse.SC_OK;
	}

	private static InputStream createInputStream(HttpServletRequest request) throws IOException {
		return createInputStream(request.getParameter(METHOD), request.getParameter(ID), request.getParameter(PARAMS));
	}

	private boolean isErrorCode(int result) {
		for (ErrorResolver.JsonError error : Arrays.asList(INTERNAL_ERROR, METHOD_PARAMS_INVALID, ERROR_NOT_HANDLED, BULK_ERROR)) {
			if (error.code == result) return true;
		}
		return CUSTOM_SERVER_ERROR_UPPER >= result && result >= CUSTOM_SERVER_ERROR_LOWER;
	}

}
