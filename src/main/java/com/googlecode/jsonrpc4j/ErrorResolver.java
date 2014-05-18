/*
The MIT License (MIT)

Copyright (c) 2014 jsonrpc4j

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package com.googlecode.jsonrpc4j;

import java.lang.reflect.Method;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Resolves {@link Throwable}s thrown by JSON-RPC services.
 */
public interface ErrorResolver {

	/**
	 * Resolves the error using the {@link Throwable} that
	 * was thrown by the given {@link Method} when passed
	 * the given {@code arguments}.  If the error can not
	 * be resolved then null is returned.
	 *
	 * @param t the {@link Throwable}
	 * @param method the {@link Method} that threw the {@link Throwable}
	 * @param arguments the {@code arguments} that were passed to the {@link Method}
	 * @return the {@link JsonError} or null if it couldn't be resolved
	 */
	JsonError resolveError(Throwable t, Method method, List<JsonNode> arguments);

	/**
	 * A JSON error.
	 */
	public static class JsonError {

		private int code;
		private String message;
		private Object data;

		/**
		 * Creates the error.
		 * @param code the code
		 * @param message the message
		 * @param data the data
		 */
		public JsonError(int code, String message, Object data) {
			this.code 		= code;
			this.message	= message;
			this.data		= data;
		}

		/**
		 * @return the code
		 */
		protected int getCode() {
			return code;
		}

		/**
		 * @return the message
		 */
		protected String getMessage() {
			return message;
		}

		/**
		 * @return the data
		 */
		protected Object getData() {
			return data;
		}

	}
}
