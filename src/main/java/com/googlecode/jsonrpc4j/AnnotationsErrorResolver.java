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
