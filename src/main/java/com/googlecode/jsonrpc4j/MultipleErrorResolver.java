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
