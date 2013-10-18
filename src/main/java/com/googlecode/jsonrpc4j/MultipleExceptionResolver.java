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

	private List<ExceptionResolver> resolvers;

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
