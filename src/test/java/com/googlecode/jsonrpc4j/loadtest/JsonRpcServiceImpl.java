package com.googlecode.jsonrpc4j.loadtest;

import java.util.logging.Logger;

/**
 * @author Eduard Szente
 */
public class JsonRpcServiceImpl implements JsonRpcService {
	public void doSomething() {
		Logger.getLogger(JsonRpcServiceImpl.class.getName()).info("doSomething()");
	}
}
