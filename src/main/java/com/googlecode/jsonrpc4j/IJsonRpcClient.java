/*
 * The MIT License
 *
 * Copyright (c) 2014 jsonrpc4j
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jsonrpc4j;

import java.lang.reflect.Type;
import java.util.Map;

/**
 *
 * @author toha
 */
public interface IJsonRpcClient {
    
	/**
	 * Invokes the given method with the given argument.
	 *
	 * @see JsonRpcClient#writeRequest(String, Object, java.io.OutputStream, String)
	 * @param methodName the name of the method to invoke
	 * @param arguments the arguments to the method
	 * @throws Throwable on error
	 */
	public void invoke(String methodName, Object argument)
		throws Throwable;

	/**
	 * Invokes the given method with the given arguments and returns
	 * an object of the given type, or null if void.
	 *
	 * @see JsonRpcClient#writeRequest(String, Object, java.io.OutputStream, String)
	 * @param methodName the name of the method to invoke
	 * @param argument the arguments to the method
	 * @param returnType the return type
	 * @return the return value
	 * @throws Throwable on error
	 */
	public Object invoke(String methodName, Object argument, Type returnType)
		throws Throwable;

	/**
	 * Invokes the given method with the given arguments and returns
	 * an object of the given type, or null if void.
	 *
	 * @see JsonRpcClient#writeRequest(String, Object, java.io.OutputStream, String)
	 * @param methodName the name of the method to invoke
	 * @param arguments the arguments to the method
	 * @param returnType the return type
	 * @param extraHeaders extra headers to add to the request
	 * @return the return value
	 * @throws Throwable on error
	 */
	public Object invoke(String methodName, Object argument, Type returnType, Map<String, String> extraHeaders)
		throws Throwable;
    
	/**
	 * Invokes the given method with the given arguments and returns
	 * an object of the given type, or null if void.
	 *
	 * @see JsonRpcClient#writeRequest(String, Object, java.io.OutputStream, String)
	 * @param methodName the name of the method to invoke
	 * @param argument the arguments to the method
	 * @param returnType the return type
	 * @return the return value
	 * @throws Throwable on error
	 */
	public <T> T invoke(String methodName, Object argument, Class<T> clazz)
		throws Throwable;

	/**
	 * Invokes the given method with the given arguments and returns
	 * an object of the given type, or null if void.
	 *
	 * @see JsonRpcClient#writeRequest(String, Object, java.io.OutputStream, String)
	 * @param methodName the name of the method to invoke
	 * @param arguments the arguments to the method
	 * @param returnType the return type
	 * @param extraHeaders extra headers to add to the request
	 * @return the return value
	 * @throws Throwable on error
	 */
	public <T> T invoke(String methodName, Object argument, Class<T> clazz, Map<String, String> extraHeaders)
		throws Throwable;

}
