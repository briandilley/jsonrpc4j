package com.googlecode.jsonrpc4j;

/**
 * This interface is used by the JsonRpcHttpAsyncClient for receiving
 * RPC responses.  When an invocation is made, one of {@code onComplete()}
 * or {@code onError()} is guaranteed to be called.
 *
 * @param <T> the return type of the JSON-RPC call
 * @author Brett Wooldridge
 */
public interface JsonRpcCallback<T> {
	
	/**
	 * Called if the remote invocation was successful.
	 *
	 * @param result the result object of the call (possibly null)
	 */
	void onComplete(T result);
	
	/**
	 * Called if there was an error in the remove invocation.
	 *
	 * @param t the {@code Throwable} (possibly wrapping) the invocation error
	 */
	void onError(Throwable t);
}
