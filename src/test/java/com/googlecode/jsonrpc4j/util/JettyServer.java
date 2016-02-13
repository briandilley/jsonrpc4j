package com.googlecode.jsonrpc4j.util;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.googlecode.jsonrpc4j.AnnotationsErrorResolver;
import com.googlecode.jsonrpc4j.JsonRpcServer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("WeakerAccess")
public class JettyServer implements AutoCloseable {

	public static final String SERVLET = "someSunnyServlet";
	private static final String LOCAL_SERVER = "127.0.0.1";
	private static final String PROTOCOL = "http";

	private final Class service;
	private Server jetty;
	private int port;

	JettyServer(Class service) {
		this.service = service;
	}

	public String getCustomServerUrlString(final String servletName) {
		return PROTOCOL + "://" + LOCAL_SERVER + ":" + port + "/" + servletName;
	}

	public void startup() throws Exception {
		port = 10000 + new Random().nextInt(30000);
		jetty = new Server(port);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		context.setContextPath("/");
		jetty.setHandler(context);
		ServletHolder servlet = context.addServlet(JsonRpcTestServlet.class, "/" + SERVLET);
		servlet.setInitParameter("class", service.getCanonicalName());
		jetty.start();
	}

	@Override
	public void close() throws Exception {
		this.stop();
	}

	public void stop() throws Exception {
		jetty.stop();
	}

	public static class JsonRpcTestServlet extends HttpServlet {

		private JsonRpcServer jsonRpcServer;

		@Override
		public void init() {
			try {
				// noinspection unchecked
				final Class aClass = Class.forName(getInitParameter("class"));
				// noinspection unchecked
				jsonRpcServer = new JsonRpcServer(aClass.getConstructor().newInstance());
				jsonRpcServer.setErrorResolver(AnnotationsErrorResolver.INSTANCE);
			} catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
				e.printStackTrace();
			}

		}

		@Override
		protected void doPost(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			jsonRpcServer.handle(request, response);
		}
	}

}
