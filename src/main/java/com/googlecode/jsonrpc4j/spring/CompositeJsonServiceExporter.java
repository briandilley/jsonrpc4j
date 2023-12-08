package com.googlecode.jsonrpc4j.spring;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.HttpRequestHandler;

import java.io.IOException;

/**
 * A Composite service exporter for spring that exposes
 * multiple services via JSON-RPC over HTTP.
 */
@SuppressWarnings("unused")
public class CompositeJsonServiceExporter extends AbstractCompositeJsonServiceExporter implements HttpRequestHandler {

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
