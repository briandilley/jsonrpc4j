package com.googlecode.jsonrpc4j.loadtest;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 * @author Eduard Szente
 */
public class ServletEngine {

	public static final int PORT = 54321;

	Server server;

	public void startup() throws Exception {
		server = new Server(PORT);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);
		context.addServlet(LoadTestServlet.class, "/servlet");
		server.start();
	}

	public void stop() throws Exception {
		server.stop();
	}

}
