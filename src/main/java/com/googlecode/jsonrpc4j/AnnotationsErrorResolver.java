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
import com.googlecode.jsonrpc4j.DefaultErrorResolver.ErrorData;

/**
 * {@link ErrorResolver} that uses annotations.
 */
public class AnnotationsErrorResolver
	implements ErrorResolver {

	public static final AnnotationsErrorResolver INSTANCE = new AnnotationsErrorResolver();

	/**
	 * {@inheritDoc}
	 */
	public JsonError resolveError(Throwable t, Method method, List<JsonNode> arguments) {

		// use annotations to map errors
		JsonRpcErrors errors = ReflectionUtil.getAnnotation(method, JsonRpcErrors.class);
		if (errors!=null) {
			for (JsonRpcError em : errors.value()) {
				if (em.exception().isInstance(t)) {
					String message = em.message()!=null && em.message().trim().length() > 0
						? em.message()
						: t.getMessage();
					return new JsonError(em.code(), message,
						new ErrorData(em.exception().getName(), message));
				}
			}
		}

		//  none found
		return null;
	}

}
