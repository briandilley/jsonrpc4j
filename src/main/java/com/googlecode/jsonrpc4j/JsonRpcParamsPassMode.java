package com.googlecode.jsonrpc4j;

/**
 * The JSON-RPC specification allows either passing parameters as an Array, for by-position arguments, or as an Object,
 * for by-name arguments.
 *
 */
public enum JsonRpcParamsPassMode {
	AUTO,
	ARRAY,
	OBJECT
}
