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
 * An {@link ErrorResolver} that puts type information into the
 * data portion of the error.  This {@link ErrorResolver} always
 * returns a {@link JsonError}.
 *
 */
public class DefaultErrorResolver
	implements ErrorResolver {

	public static final DefaultErrorResolver INSTANCE = new DefaultErrorResolver();

	/**
	 * {@inheritDoc}
	 */
	public JsonError resolveError(
		Throwable t, Method method, List<JsonNode> arguments) {
		return new JsonError(0, t.getMessage(),
			new ErrorData(t.getClass().getName(), t.getMessage()));
	}

	/**
	 * Data that is added to an error.
	 *
	 */
	public static class ErrorData {

		private String exceptionTypeName;
		private String message;

		/**
		 * Creates it.
		 * @param exceptionTypeName the exception type name
		 * @param message the message
		 */
		public ErrorData(String exceptionTypeName, String message) {
			this.exceptionTypeName 	= exceptionTypeName;
			this.message			= message;
		}

		/**
		 * @return the exceptionTypeName
		 */
		public String getExceptionTypeName() {
			return exceptionTypeName;
		}

		/**
		 * @return the message
		 */
		public String getMessage() {
			return message;
		}

	}

}
