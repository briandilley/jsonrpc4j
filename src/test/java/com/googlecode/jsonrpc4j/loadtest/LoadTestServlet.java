package com.googlecode.jsonrpc4j.loadtest;

import com.googlecode.jsonrpc4j.JsonRpcServer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Eduard Szente
 */
@SuppressWarnings("serial")
public class LoadTestServlet
	extends HttpServlet {

	private JsonRpcServer jsonRpcServer;

	public void init() {
		jsonRpcServer = new JsonRpcServer(new JsonRpcServiceImpl());
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		jsonRpcServer.handle(req, resp);
	}
}
