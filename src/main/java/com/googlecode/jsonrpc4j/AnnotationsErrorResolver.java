package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Method;
import java.util.List;

/**
 * {@link ErrorResolver} that uses annotations.
 */
public enum AnnotationsErrorResolver implements ErrorResolver {
	INSTANCE;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public JsonError resolveError(Throwable thrownException, Method method, List<JsonNode> arguments) {
		JsonRpcError resolver = getResolverForException(thrownException, method);
		if (notFoundResolver(resolver)) {
			return null;
		}
		
		String message = hasErrorMessage(resolver) ? resolver.message() : thrownException.getMessage();
		Object data = hasErrorData(resolver) ? resolver.data() : new ErrorData(resolver.exception().getName(), message);
		return new JsonError(resolver.code(), message, data);
	}
	
	private JsonRpcError getResolverForException(Throwable thrownException, Method method) {
		JsonRpcErrors errors = ReflectionUtil.getAnnotation(method, JsonRpcErrors.class);
		if (hasAnnotations(errors)) {
			for (JsonRpcError errorDefined : errors.value()) {
				if (isExceptionInstanceOfError(thrownException, errorDefined)) {
					return errorDefined;
				}
			}
		}
		return null;
	}
	
	private boolean notFoundResolver(JsonRpcError resolver) {
		return resolver == null;
	}
	
	private boolean hasErrorMessage(JsonRpcError em) {
		// noinspection ConstantConditions
		return em.message() != null && em.message().trim().length() > 0;
	}
        
        private boolean hasErrorData(JsonRpcError em) {
		// noinspection ConstantConditions
		return em.data() != null && em.data().trim().length() > 0;
	}
	
	private boolean hasAnnotations(JsonRpcErrors errors) {
		return errors != null;
	}
	
	private boolean isExceptionInstanceOfError(Throwable target, JsonRpcError em) {
		return em.exception().isInstance(target);
	}
	
}
