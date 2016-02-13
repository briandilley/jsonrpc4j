package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Resolves {@link Throwable}s thrown by JSON-RPC services.
 */
public interface ErrorResolver {

	/**
	 * Resolves the error using the {@link Throwable} that
	 * was thrown by the given {@link Method} when passed
	 * the given {@code arguments}.  If the error can not
	 * be resolved then null is returned.
	 *
	 * @param t the {@link Throwable}
	 * @param method the {@link Method} that threw the {@link Throwable}
	 * @param arguments the {@code arguments} that were passed to the {@link Method}
	 * @return the {@link JsonError} or null if it couldn't be resolved
	 */
	JsonError resolveError(Throwable t, Method method, List<JsonNode> arguments);

	/**
	 * A JSON error.
	 */
	@SuppressWarnings("WeakerAccess")
	class JsonError {

		private final int code;
		private final String message;
		private final Object data;

		/**
		 * Creates the error.
		 * @param code the code
		 * @param message the message
		 * @param data the data
		 */
		public JsonError(int code, String message, Object data) {
			this.code = code;
			this.message = message;
			this.data = data;
		}

		/**
		 * @return the code
		 */
		int getCode() {
			return code;
		}

		/**
		 * @return the message
		 */
		String getMessage() {
			return message;
		}

		/**
		 * @return the data
		 */
		Object getData() {
			return data;
		}

		@Override
		public String toString() {
			return "JsonError{" + "code=" + code +
					", message='" + message + '\'' +
					", data=" + data +
					'}';
		}
	}
}
