package com.googlecode.jsonrpc4j.spring.serviceb;

/**
 * <p>This implementation should not be picked up by the
 * {@link com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImplExporter}
 * bean because it does not have the necessary annotation.</p>
 */

public class NoopTemperatureImpl implements Temperature {
	
	@Override
	public Integer currentTemperature() {
		return 0;
	}
}
