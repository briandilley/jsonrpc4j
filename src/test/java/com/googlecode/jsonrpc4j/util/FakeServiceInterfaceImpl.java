package com.googlecode.jsonrpc4j.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

@SuppressWarnings("WeakerAccess")
public class FakeServiceInterfaceImpl implements FakeServiceInterface {
	
	private static final Logger logger = LoggerFactory.getLogger(FakeServiceInterfaceImpl.class);
	
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
