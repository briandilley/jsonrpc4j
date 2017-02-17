package com.googlecode.jsonrpc4j.spring.serviceb;

import com.googlecode.jsonrpc4j.JsonRpcService;

@JsonRpcService(
		"api/temperature" // note the absence of a leading slash
)
public interface Temperature {
	
	@SuppressWarnings("unused")
	Integer currentTemperature();
	
}
