package com.googlecode.jsonrpc4j.spring.serviceb;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;

/**
 * <p>This implementation should be picked up by the
 * {@link com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImplExporter}
 * class.</p>
 */

@AutoJsonRpcServiceImpl(additionalPaths = {
		"/api-web/temperature" // note the leading slash
})
public class TemperatureImpl implements Temperature {

	@Override
	public Integer currentTemperature() {
		return 25;
	}

}
