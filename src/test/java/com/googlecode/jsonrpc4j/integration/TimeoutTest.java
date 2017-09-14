package com.googlecode.jsonrpc4j.integration;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.util.BaseRestTest;
import com.googlecode.jsonrpc4j.util.FakeTimingOutService;
import com.googlecode.jsonrpc4j.util.FakeTimingOutServiceImpl;
import org.junit.Test;

import java.net.SocketTimeoutException;

import static org.hamcrest.CoreMatchers.isA;

public class TimeoutTest extends BaseRestTest {
	private FakeTimingOutService service;

	@Test
	public void testTimingOutRequests() throws Exception {
		JsonRpcHttpClient client = getHttpClient(true, false);
		client.setReadTimeoutMillis(1);
		expectedEx.expectCause(isA(SocketTimeoutException.class));
		service = ProxyUtil.createClientProxy(this.getClass().getClassLoader(), FakeTimingOutService.class, client);
		service.doTimeout();
	}

	@Override
	protected Class service() {
		return FakeTimingOutServiceImpl.class;
	}
}
