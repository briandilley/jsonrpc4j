package com.googlecode.jsonrpc4j.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeTimingOutServiceImpl implements FakeTimingOutService {
	private static final Logger logger = LoggerFactory.getLogger(FakeTimingOutServiceImpl.class);

	@Override
	public void doTimeout() {
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			logger.debug("FakeTimingOutServiceImpl doTimeout() thread interrupted. Safe to ignore.");
		}
	}
}
