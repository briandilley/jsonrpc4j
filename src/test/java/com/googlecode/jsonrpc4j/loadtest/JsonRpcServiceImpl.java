package com.googlecode.jsonrpc4j.loadtest;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * @author Eduard Szente
 */
public class JsonRpcServiceImpl
	implements JsonRpcService {

	private static final Logger LOG = Logger.getLogger(JsonRpcServiceImpl.class.getName());

	@Override
	public void doSomething() {
		LOG.info("doSomething()");
	}

	@Override
	public int returnSomeSimple(int arg) {
		return arg;
	}

	@Override
	public ComplexType returnSomeComplex(int arg1, String arg2) {
		ComplexType complexType = new ComplexType();
		complexType.setInteger(arg1);
		complexType.setString(arg2);
		complexType.setList(new ArrayList<String>());

		complexType.getList().add(String.valueOf(arg1));
		complexType.getList().add(arg2);

		return complexType;
	}

	@Override
	public void throwSomeException(String message)
		throws Exception {

		throw new UnsupportedOperationException(message);
	}
}
