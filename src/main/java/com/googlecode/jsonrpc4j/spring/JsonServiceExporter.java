package com.googlecode.jsonrpc4j.spring;

import org.springframework.web.HttpRequestHandler;

import com.googlecode.jsonrpc4j.JsonRpcServer;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link HttpRequestHandler} that exports services using Json
 * according to the JSON-RPC proposal specified at:
 * <a href="http://groups.google.com/group/json-rpc">
 * http://groups.google.com/group/json-rpc</a>.
 *
 */
class JsonServiceExporter extends AbstractJsonServiceExporter implements HttpRequestHandler {

	private JsonRpcServer jsonRpcServer;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void exportService() {
		jsonRpcServer = getJsonRpcServer();
	}

	/**
	 * {@inheritDoc}
	 */
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		jsonRpcServer.handle(request, response);
		response.getOutputStream().flush();
	}

}
