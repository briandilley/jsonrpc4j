package com.googlecode.jsonrpc4j;

import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.googlecode.jsonrpc4j.loadtest.ServletEngine;
import com.googlecode.jsonrpc4j.loadtest.JsonRpcService;

/**
 * @author Alexander Makarov
 */
public class HttpCodeTest {

	private ServletEngine servletEngine;

	@Before
	public void setup() throws Exception {
		servletEngine = new ServletEngine();
		servletEngine.startup();
	}

	@Test
	public void http404() throws MalformedURLException {
		JsonRpcHttpClient jsonRpcHttpClient = new JsonRpcHttpClient(new URL(
				"http://127.0.0.1:" + ServletEngine.PORT + "/error"));
		JsonRpcService service = ProxyUtil.createClientProxy(
				JsonRpcService.class.getClassLoader(), JsonRpcService.class,
				jsonRpcHttpClient);

		try {
			service.doSomething();
			Assert.fail();
		} catch (HttpException e) {
			Assert.assertTrue(e.getMessage().contains("404 Not Found"));
		}
	}

	@After
	public void teardown() throws Exception {
		servletEngine.stop();
	}

}
