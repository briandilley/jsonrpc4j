package com.googlecode.jsonrpc4j;

/**
 * Request ID generator interface. This allows {@link JsonRpcClient} to use different strategy to
 * generate the ID for the request.
 */
public interface RequestIDGenerator {
	
	/**
	 * Generate the request ID for each json-rpc request.
	 *
	 * @return The request id. This can be of any type. It is used to match the response with the request that it is replying to.
	 */
	String generateID();
}
