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
	 * @param t         the {@link Throwable}
	 * @param method    the {@link Method} that threw the {@link Throwable}
	 * @param arguments the {@code arguments} that were passed to the {@link Method}
	 * @return the {@link JsonError} or null if it couldn't be resolved
	 */
	JsonError resolveError(Throwable t, Method method, List<JsonNode> arguments);

	/**
	 * A JSON error.
	 */
	@SuppressWarnings({"WeakerAccess", "unused"})
	class JsonError {

		public static final JsonError OK = new JsonError(0, "ok", null, true);
		public static final JsonError PARSE_ERROR = new JsonError(-32700, "JSON parse error", null, true);
		public static final JsonError INVALID_REQUEST = new JsonError(-32600, "invalid request", null, true);
		public static final JsonError METHOD_NOT_FOUND = new JsonError(-32601, "method not found", null, true);
		public static final JsonError METHOD_PARAMS_INVALID = new JsonError(-32602, "method parameters invalid", null, true);
		public static final JsonError INTERNAL_ERROR = new JsonError(-32603, "internal error", null, true);
		public static final JsonError ERROR_NOT_HANDLED = new JsonError(-32001, "error not handled", null, true);
		public static final JsonError BULK_ERROR = new JsonError(-32002, "bulk error", null, true);

		public static final int CUSTOM_SERVER_ERROR_UPPER = -32000;
		public static final int CUSTOM_SERVER_ERROR_LOWER = -32099;

		public final int code;
		public final String message;
		public final Object data;
		public final boolean loggingEnabled;

		/**
		 * Creates the error.
		 *
		 * @param code    the code
		 * @param message the message
		 * @param data    the data
		 */
		public JsonError(int code, String message, Object data, boolean loggingEnabled) {
			this.code = code;
			this.message = message;
			this.data = data;
			this.loggingEnabled = loggingEnabled;
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

		/**
		 * @return the loggingEnabled
		 */
		boolean isLoggingEnabled() {
			return loggingEnabled;
		}

		@Override
		public String toString() {
			return "JsonError{" + "code=" + code +
					", message='" + message + '\'' +
					", data=" + data +
					", loggingEnabled=" + loggingEnabled +
					'}';
		}
	}
}
