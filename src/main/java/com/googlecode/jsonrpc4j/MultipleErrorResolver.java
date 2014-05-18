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
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@link ErrorResolver} that supports the use
 * of multiple {@link ErrorResolver} used one
 * after another until one is able to resolve
 * the error.
 *
 */
public class MultipleErrorResolver
	implements ErrorResolver {

	private List<ErrorResolver> resolvers;

	/**
	 * Creates with the given {@link ErrorResolver}s,
	 * {@link #addErrorResolver(ErrorResolver)} can be called to
	 * add additional {@link ErrorResolver}s.
	 * @param resolvers the {@link ErrorResolver}s
	 */
	public MultipleErrorResolver(ErrorResolver... resolvers) {
		this.resolvers = new LinkedList<ErrorResolver>();
		for (ErrorResolver resolver : resolvers) {
			this.resolvers.add(resolver);
		}
	}

	/**
	 * Adds an {@link ErrorResolver} to the end of the
	 * resolver chain.
	 * @param errorResolver the {@link ErrorResolver} to add
	 */
	public void addErrorResolver(ErrorResolver errorResolver) {
		this.resolvers.add(errorResolver);
	}

	/**
	 * {@inheritDoc}
	 */
	public JsonError resolveError(
		Throwable t, Method method, List<JsonNode> arguments) {

		// delegate to internal resolvers
		JsonError resolvedError = null;
		for (ErrorResolver resolver : resolvers) {
			resolvedError = resolver.resolveError(t, method, arguments);
			if (resolvedError!=null) {
				return resolvedError;
			}
		}

		// unable to resolve
		return null;
	}

}
