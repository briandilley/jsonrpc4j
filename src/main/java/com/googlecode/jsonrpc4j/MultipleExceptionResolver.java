package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link ExceptionResolver} that supports the use
 * of multiple {@link ExceptionResolver} used one
 * after another until one is able to resolve
 * the Exception.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class MultipleExceptionResolver implements ExceptionResolver {
	
	private final List<ExceptionResolver> resolvers;
	
	/**
	 * Creates with the given {@link ExceptionResolver}s,
	 * {@link #addExceptionResolver(ExceptionResolver)} can be called to
	 * add additional {@link ExceptionResolver}s.
	 *
	 * @param resolvers the {@link ExceptionResolver}s
	 */
	public MultipleExceptionResolver(ExceptionResolver... resolvers) {
		this.resolvers = new LinkedList<>();
		Collections.addAll(this.resolvers, resolvers);
	}
	
	/**
	 * Adds an {@link ExceptionResolver} to the end of the
	 * resolver chain.
	 *
	 * @param ExceptionResolver the {@link ExceptionResolver} to add
	 */
	public void addExceptionResolver(ExceptionResolver ExceptionResolver) {
		this.resolvers.add(ExceptionResolver);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Throwable resolveException(ObjectNode response) {
		for (ExceptionResolver resolver : resolvers) {
			Throwable resolvedException = resolver.resolveException(response);
			if (resolvedException != null) {
				return resolvedException;
			}
		}
		return null;
	}
	
}
