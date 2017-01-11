package com.googlecode.jsonrpc4j;

/**
 * <p>
 * A status code provider maps an HTTP status code to a JSON-RPC result code (e.g. -32000 -&gt; 500).
 * </p>
 * <p>
 * From version 2.0 on the JSON-RPC specification is not explicitly documenting the mapping of result/error codes, so
 * this provider can be used to configure application specific HTTP status codes for a given JSON-RPC error code.
 * </p>
 * <p>
 * The default implementation {@link DefaultHttpStatusCodeProvider} follows the rules defined in the
 * <a href="http://www.jsonrpc.org/historical/json-rpc-over-http.html">JSON-RPC over HTTP</a> document.
 * </p>
 */
public interface HttpStatusCodeProvider {
	
	/**
	 * Returns an HTTP status code for the given response and result code.
	 *
	 * @param resultCode the result code of the current JSON-RPC method call. This is used to look up the HTTP status
	 *                   code.
	 * @return the int representation of the HTTP status code that should be used by the JSON-RPC response.
	 */
	int getHttpStatusCode(int resultCode);
	
	/**
	 * Returns result code for the given HTTP status code
	 *
	 * @param httpStatusCode the int representation of the HTTP status code that should be used by the JSON-RPC response.
	 * @return resultCode the result code of the current JSON-RPC method call. This is used to look up the HTTP status
	 */
	Integer getJsonRpcCode(int httpStatusCode);
}
