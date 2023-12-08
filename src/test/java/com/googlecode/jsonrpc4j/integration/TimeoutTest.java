package com.googlecode.jsonrpc4j.integration;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.util.BaseRestTest;
import com.googlecode.jsonrpc4j.util.FakeTimingOutService;
import com.googlecode.jsonrpc4j.util.FakeTimingOutServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;

@Disabled
public class TimeoutTest extends BaseRestTest {
	private FakeTimingOutService service;

	@Test
	public void testTimingOutRequests() throws Exception {
		JsonRpcHttpClient client = getHttpClient(true, false);
		client.setReadTimeoutMillis(1);
		service = ProxyUtil.createClientProxy(this.getClass().getClassLoader(), FakeTimingOutService.class, client);
		Assertions.assertThrows(SocketTimeoutException.class, () -> service.doTimeout());
	}

	@Override
	protected Class service() {
		return FakeTimingOutServiceImpl.class;
	}
}
