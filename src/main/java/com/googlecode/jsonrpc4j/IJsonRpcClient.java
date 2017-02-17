package com.googlecode.jsonrpc4j;

import java.lang.reflect.Type;
import java.util.Map;

public interface IJsonRpcClient {
	
	/**
	 * Invokes the given method with the given argument.
	 *
	 * @param methodName the name of the method to invoke
	 * @param argument   the argument to the method
	 * @throws Throwable on error
	 */
	void invoke(String methodName, Object argument) throws Throwable;
	
	/**
	 * Invokes the given method with the given arguments and returns
	 * an object of the given type, or null if void.
	 *
	 * @param methodName the name of the method to invoke
	 * @param argument   the argument to the method
	 * @param returnType the return type
	 * @return the return value
	 * @throws Throwable on error
	 */
	Object invoke(String methodName, Object argument, Type returnType) throws Throwable;
	
	/**
	 * Invokes the given method with the given arguments and returns
	 * an object of the given type, or null if void.
	 *
	 * @param methodName   the name of the method to invoke
	 * @param argument     the argument to the method
	 * @param returnType   the return type
	 * @param extraHeaders extra headers to add to the request
	 * @return the return value
	 * @throws Throwable on error
	 */
	Object invoke(String methodName, Object argument, Type returnType, Map<String, String> extraHeaders) throws Throwable;
	
	/**
	 * Invokes the given method with the given arguments and returns
	 * an object of the given type, or null if void.
	 *
	 * @param methodName the name of the method to invoke
	 * @param argument   the argument to the method
	 * @param clazz      the return type
	 * @param <T>        the return type
	 * @return the return value
	 * @throws Throwable on error
	 */
	<T> T invoke(String methodName, Object argument, Class<T> clazz) throws Throwable;
	
	/**
	 * Invokes the given method with the given arguments and returns
	 * an object of the given type, or null if void.
	 *
	 * @param methodName   the name of the method to invoke
	 * @param argument     the argument to the method
	 * @param clazz        the return type
	 * @param extraHeaders extra headers to add to the request
	 * @param <T>          the return type
	 * @return the return value
	 * @throws Throwable on error
	 */
	<T> T invoke(String methodName, Object argument, Class<T> clazz, Map<String, String> extraHeaders) throws Throwable;
	
}
