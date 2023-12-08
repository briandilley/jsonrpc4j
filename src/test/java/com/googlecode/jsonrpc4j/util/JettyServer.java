package com.googlecode.jsonrpc4j.util;

import com.googlecode.jsonrpc4j.AnnotationsErrorResolver;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import static com.googlecode.jsonrpc4j.util.Util.DEFAULT_LOCAL_HOSTNAME;

@SuppressWarnings("WeakerAccess")
public class JettyServer implements AutoCloseable {

	public static final String SERVLET = "someSunnyServlet";
	private static final String PROTOCOL = "http";

	private final Class<?> service;
	private Server jetty;
	private int port;

	JettyServer(Class<?> service) {
		this.service = service;
	}

	public String getCustomServerUrlString(final String servletName) {
		return PROTOCOL + "://" + DEFAULT_LOCAL_HOSTNAME + ":" + port + "/" + servletName;
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

		static final long serialVersionUID = 1L;
		private transient JsonRpcServer jsonRpcServer;

		@Override
		public void init() {
			try {
				final Class<?> aClass = Class.forName(getInitParameter("class"));
				final Object instance = aClass.getConstructor().newInstance();
				jsonRpcServer = new JsonRpcServer(instance);
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
