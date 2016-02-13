package com.googlecode.jsonrpc4j.util;

import org.apache.logging.log4j.LogManager;

import java.util.Collections;

@SuppressWarnings("WeakerAccess")
public class FakeServiceInterfaceImpl implements FakeServiceInterface {

	private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

	@Override
	public void doSomething() {
		logger.debug("doing something");
	}

	@Override
	public int returnPrimitiveInt(int arg) {
		return arg;
	}

	@Override
	public CustomClass returnCustomClass(int primitiveArg, String stringArg) {
		CustomClass result = new CustomClass(primitiveArg, stringArg);
		Collections.addAll(result.list, "" + primitiveArg, stringArg);
		return result;
	}

	@Override
	public void throwSomeException(String message) {
		throw new UnsupportedOperationException(message);
	}
}
