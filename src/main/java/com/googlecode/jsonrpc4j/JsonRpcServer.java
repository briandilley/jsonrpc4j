package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A JSON-RPC request server reads JSON-RPC requests from an input stream and writes responses to an output stream.
 * Supports handler and servlet requests.
 */
@SuppressWarnings("unused")
public class JsonRpcServer extends JsonRpcBasicServer {
	private static final Logger logger = LoggerFactory.getLogger(JsonRpcServer.class);
	
	private static final String GZIP = "gzip";
	private final boolean gzipResponses;
	private String contentType = JSONRPC_CONTENT_TYPE;
	
	/**
	 * Creates the server with the given {@link ObjectMapper} delegating
	 * all calls to the given {@code handler} {@link Object} but only
	 * methods available on the {@code remoteInterface}.
	 *
	 * @param mapper          the {@link ObjectMapper}
	 * @param handler         the {@code handler}
	 * @param remoteInterface the interface
	 * @param gzipResponses   whether gzip the response that is sent to the client.
	 */
	public JsonRpcServer(ObjectMapper mapper, Object handler, Class<?> remoteInterface, boolean gzipResponses) {
		super(mapper, handler, remoteInterface);
		this.gzipResponses = gzipResponses;
	}
	
	/**
	 * Creates the server with the given {@link ObjectMapper} delegating
	 * all calls to the given {@code handler} {@link Object} but only
	 * methods available on the {@code remoteInterface}.
	 *
	 * @param mapper          the {@link ObjectMapper}
	 * @param handler         the {@code handler}
	 * @param remoteInterface the interface
	 */
	public JsonRpcServer(ObjectMapper mapper, Object handler, Class<?> remoteInterface) {
		super(mapper, handler, remoteInterface);
		this.gzipResponses = false;
	}
	
	/**
	 * Creates the server with the given {@link ObjectMapper} delegating
	 * all calls to the given {@code handler}.
	 *
	 * @param mapper  the {@link ObjectMapper}
	 * @param handler the {@code handler}
	 */
	public JsonRpcServer(ObjectMapper mapper, Object handler) {
		super(mapper, handler, null);
		this.gzipResponses = false;
	}
	
	/**
	 * Creates the server with a default {@link ObjectMapper} delegating
	 * all calls to the given {@code handler} {@link Object} but only
	 * methods available on the {@code remoteInterface}.
	 *
	 * @param handler         the {@code handler}
	 * @param remoteInterface the interface
	 */
	private JsonRpcServer(Object handler, Class<?> remoteInterface) {
		super(new ObjectMapper(), handler, remoteInterface);
		this.gzipResponses = false;
	}
	
	/**
	 * Creates the server with a default {@link ObjectMapper} delegating
	 * all calls to the given {@code handler}.
	 *
	 * @param handler the {@code handler}
	 */
	public JsonRpcServer(Object handler) {
		super(new ObjectMapper(), handler, null);
		this.gzipResponses = false;
	}
	
	/**
	 * Handles a portlet request.
	 *
	 * @param request  the {@link ResourceRequest}
	 * @param response the {@link ResourceResponse}
	 * @throws IOException on error
	 */
	public void handle(ResourceRequest request, ResourceResponse response) throws IOException {
		logger.debug("Handing ResourceRequest {}", request.getMethod());
		response.setContentType(contentType);
		InputStream input = getRequestStream(request);
		OutputStream output = response.getPortletOutputStream();
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
	 * @param request  the {@link HttpServletRequest}
	 * @param response the {@link HttpServletResponse}
	 * @throws IOException on error
	 */
	public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.debug("Handling HttpServletRequest {}", request);
		response.setContentType(contentType);
		OutputStream output = response.getOutputStream();
		InputStream input = getRequestStream(request);
		int result = ErrorResolver.JsonError.PARSE_ERROR.code;
		int contentLength = 0;
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		try {
			String acceptEncoding = request.getHeader(ACCEPT_ENCODING);
			result = handleRequest0(input, output, acceptEncoding, response, byteOutput);

			contentLength = byteOutput.size();
		} catch (Throwable t) {
			if (StreamEndedException.class.isInstance(t)) {
				logger.debug("Bad request: empty contents!");
			} else {
				logger.error(t.getMessage(), t);
			}
		}
		int httpStatusCode = httpStatusCodeProvider == null ? DefaultHttpStatusCodeProvider.INSTANCE.getHttpStatusCode(result)
				: httpStatusCodeProvider.getHttpStatusCode(result);
		response.setStatus(httpStatusCode);
		response.setContentLength(contentLength);
		byteOutput.writeTo(output);
		output.flush();
	}
	
	private InputStream getRequestStream(HttpServletRequest request) throws IOException {
		InputStream input;
		if (request.getMethod().equals("POST")) {
			input = createInputStream(request.getInputStream(), request.getHeader(CONTENT_ENCODING));
		} else if (request.getMethod().equals("GET")) {
			input = createInputStream(request);
		} else {
			throw new IOException("Invalid request method, only POST and GET is supported");
		}
		return input;
	}
	
	private int handleRequest0(InputStream input, OutputStream output, String contentEncoding, HttpServletResponse response, ByteArrayOutputStream byteOutput) throws IOException {
		int result;

		boolean canGzipResponse = contentEncoding != null && contentEncoding.contains(GZIP);
		// Use gzip if client's accept-encoding is set to gzip and gzipResponses is enabled.
		if (gzipResponses && canGzipResponse) {
			response.addHeader(CONTENT_ENCODING, GZIP);
			try (GZIPOutputStream gos = new GZIPOutputStream(byteOutput)) {
				result = handleRequest(input, gos);
			}
		} else {
			result = handleRequest(input, byteOutput);
		}

		return result;
	}
	
	private static InputStream createInputStream(InputStream inputStream, String contentEncoding) throws IOException {
		InputStream input;
		if (contentEncoding != null && GZIP.equalsIgnoreCase(contentEncoding)) {
			input = new GZIPInputStream(inputStream);
		} else {
			input = inputStream;
		}
		
		return input;
	}
	
	private static InputStream createInputStream(HttpServletRequest request) throws IOException {
		String method = request.getParameter(METHOD);
		String id = request.getParameter(ID);
		String params = request.getParameter(PARAMS);
		if (method == null && id == null && params == null) {
			return new ByteArrayInputStream(new byte[]{});
		} else {
			return createInputStream(method, id, params);
		}
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
}
