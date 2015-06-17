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

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * {@link ExceptionResolver} that supports the use
 * of multiple {@link ExceptionResolver} used one
 * after another until one is able to resolve
 * the Exception.
 *
 */
public class MultipleExceptionResolver
	implements ExceptionResolver {

	private final List<ExceptionResolver> resolvers;

	/**
	 * Creates with the given {@link ExceptionResolver}s,
	 * {@link #addExceptionResolver(ExceptionResolver)} can be called to
	 * add additional {@link ExceptionResolver}s.
	 * @param resolvers the {@link ExceptionResolver}s
	 */
	public MultipleExceptionResolver(ExceptionResolver... resolvers) {
		this.resolvers = new LinkedList<ExceptionResolver>();
		for (ExceptionResolver resolver : resolvers) {
			this.resolvers.add(resolver);
		}
	}

	/**
	 * Adds an {@link ExceptionResolver} to the end of the
	 * resolver chain.
	 * @param ExceptionResolver the {@link ExceptionResolver} to add
	 */
	public void addExceptionResolver(ExceptionResolver ExceptionResolver) {
		this.resolvers.add(ExceptionResolver);
	}

	/**
	 * {@inheritDoc}
	 */
	public Throwable resolveException(ObjectNode response) {

		// delegate to internal resolvers
		for (ExceptionResolver resolver : resolvers) {
			Throwable resolvedException = resolver.resolveException(response);
			if (resolvedException!=null) {
				return resolvedException;
			}
		}

		// unable to resolve
		return null;
	}

}
