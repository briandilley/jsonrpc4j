package com.googlecode.jsonrpc4j.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import com.googlecode.jsonrpc4j.spring.rest.JsonRpcRestClient;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class BaseRestTest {

	@Rule
	final public ExpectedException expectedEx = ExpectedException.none();
	private JettyServer jettyServer;

	@Before
	public void setup() throws Exception {
		this.jettyServer = createServer();
	}

	private JettyServer createServer() throws Exception {
		Class service = service();
		if (service == null) return null;
		JettyServer jettyServer = new JettyServer(service);
		jettyServer.startup();
		return jettyServer;
	}

	protected abstract Class service();

	protected JsonRpcRestClient getClient() throws MalformedURLException {
		return getClient(JettyServer.SERVLET);
	}

	protected JsonRpcRestClient getClient(final String servlet) throws MalformedURLException {
		return new JsonRpcRestClient(new URL(jettyServer.getCustomServerUrlString(servlet)));
	}

	@After
	public void teardown() throws Exception {
		jettyServer.stop();
	}

}
