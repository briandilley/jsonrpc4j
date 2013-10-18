package com.googlecode.jsonrpc4j.loadtest;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Eduard Szente
 */
public class LoadTest {

	private ServletEngine servletEngine;

	@Before
	public void setup() throws Exception {
		servletEngine = new ServletEngine();
		servletEngine.startup();
	}

	@Test
	public void loadTestJSONRpcHttpClient() throws MalformedURLException {
		JsonRpcHttpClient jsonRpcHttpClient = new JsonRpcHttpClient(new URL(
				"http://127.0.0.1:" + ServletEngine.PORT + "/servlet"));
		JsonRpcService service = ProxyUtil.createClientProxy(
				JsonRpcService.class.getClassLoader(), JsonRpcService.class,
				jsonRpcHttpClient);

		for (int i = 0; i < 700; i++) {
			service.doSomething();
		}
	}

	@After
	public void teardown() throws Exception {
		servletEngine.stop();
	}

}
