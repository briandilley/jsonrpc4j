package com.googlecode.jsonrpc4j;

/**
 * Implementations of this interface are able to transform the converted parameters before a method invocation in the
 * JSON-RPC service. This allows for mutation of the deserialized arguments before a method invocation or for validation
 * of the actual argument objects.
 * <p>
 * Any exceptions thrown in the {@link ConvertedParameterTransformer#transformConvertedParameters(Object, Object[])}
 * method, will be returned as an error to the JSON-RPC client.
 */
public interface ConvertedParameterTransformer {
	
	/**
	 * Returns the parameters that will be passed to the method on invocation.
	 *
	 * @param target          optional service name used to locate the target object
	 *                        to invoke the Method on.
	 * @param convertedParams the parameters to pass to the method on invocation.
	 * @return the mutated parameters that will be passed to the method on invocation.
	 */
	Object[] transformConvertedParameters(Object target, Object[] convertedParams);
}
