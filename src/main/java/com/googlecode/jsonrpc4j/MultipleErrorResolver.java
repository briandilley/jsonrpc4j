package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link ErrorResolver} that supports the use
 * of multiple {@link ErrorResolver} used one
 * after another until one is able to resolve
 * the error.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class MultipleErrorResolver implements ErrorResolver {
	
	private final List<ErrorResolver> resolvers;
	
	/**
	 * Creates with the given {@link ErrorResolver}s,
	 * {@link #addErrorResolver(ErrorResolver)} can be called to
	 * add additional {@link ErrorResolver}s.
	 *
	 * @param resolvers the {@link ErrorResolver}s
	 */
	public MultipleErrorResolver(ErrorResolver... resolvers) {
		this.resolvers = new LinkedList<>();
		Collections.addAll(this.resolvers, resolvers);
	}
	
	/**
	 * Adds an {@link ErrorResolver} to the end of the
	 * resolver chain.
	 *
	 * @param errorResolver the {@link ErrorResolver} to add
	 */
	public void addErrorResolver(ErrorResolver errorResolver) {
		this.resolvers.add(errorResolver);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public JsonError resolveError(Throwable t, Method method, List<JsonNode> arguments) {
		
		JsonError resolvedError;
		for (ErrorResolver resolver : resolvers) {
			resolvedError = resolver.resolveError(t, method, arguments);
			if (resolvedError != null) {
				return resolvedError;
			}
		}
		return null;
	}
	
}
