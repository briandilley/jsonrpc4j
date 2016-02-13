package com.googlecode.jsonrpc4j;

import static com.googlecode.jsonrpc4j.Util.hasNonNullObjectData;
import static com.googlecode.jsonrpc4j.Util.hasNonNullTextualData;

import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Default implementation of the {@link ExceptionResolver} interface that attempts to re-throw the same exception
 * that was thrown by the server.  This always returns a {@link Throwable}.
 */
@SuppressWarnings("WeakerAccess")
public enum DefaultExceptionResolver implements ExceptionResolver {
	INSTANCE;
	private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

	/**
	 * {@inheritDoc}
	 */
	public Throwable resolveException(ObjectNode response) {
		ObjectNode errorObject = ObjectNode.class.cast(response.get("error"));
		if (!hasNonNullObjectData(errorObject, "data")) return createJsonRpcClientException(errorObject);

		ObjectNode dataObject = ObjectNode.class.cast(errorObject.get("data"));
		if (!hasNonNullTextualData(dataObject, "exceptionTypeName")) return createJsonRpcClientException(errorObject);

		try {
			String exceptionTypeName = dataObject.get("exceptionTypeName").asText();
			String message = hasNonNullTextualData(dataObject, "message") ? dataObject.get("message").asText() : null;
			return createThrowable(exceptionTypeName, message);
		} catch (Exception e) {
			logger.warn("Unable to create throwable", e);
			return createJsonRpcClientException(errorObject);
		}
	}

	/**
	 * Creates a {@link JsonRpcClientException} from the given
	 * {@link ObjectNode}.
	 *
	 * @param errorObject the error object
	 * @return the exception
	 */
	private JsonRpcClientException createJsonRpcClientException(ObjectNode errorObject) {
		int code = errorObject.has("code") ? errorObject.get("code").asInt() : 0;
		return new JsonRpcClientException(code, errorObject.get("message").asText(), errorObject.get("data"));
	}

	/**
	 * Attempts to create an {@link Throwable} of the given type  with the given message.  For this method to create a
	 * {@link Throwable} it must have either a default (no-args) constructor or a  constructor that takes a {@code String}
	 * as the message name.  null is returned if a {@link Throwable} can't be created.
	 *
	 * @param typeName the java type name (class name)
	 * @param message  the message
	 * @return the throwable
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws IllegalArgumentException
	 */
	private Throwable createThrowable(String typeName, String message) throws IllegalAccessException, InvocationTargetException, InstantiationException {
		Class<? extends Throwable> clazz = loadThrowableClass(typeName);
		if (clazz == null) return null;

		Constructor<? extends Throwable> defaultCtr = getDefaultConstructor(clazz);
		Constructor<? extends Throwable> messageCtr = getMessageConstructor(clazz);

		if (message != null && messageCtr != null) {
			return messageCtr.newInstance(message);
		} else if (message != null && defaultCtr != null) {
			logger.warn("Unable to invoke message constructor for {}, fallback to default", clazz.getName());
			return defaultCtr.newInstance();
		} else if (message == null && defaultCtr != null) {
			return defaultCtr.newInstance();
		} else if (message == null && messageCtr != null) {
			logger.warn("Passing null message to message constructor for {}", clazz.getName());
			return messageCtr.newInstance((String) null);
		} else {
			logger.error("Unable to find message or default constructor for {} have {}", clazz.getName(), clazz.getDeclaredConstructors());
			return null;
		}
	}

	private Class<? extends Throwable> loadThrowableClass(String typeName) {
		Class<?> clazz;
		try {
			clazz = Class.forName(typeName);
			if (!Throwable.class.isAssignableFrom(clazz)) {
				logger.warn("Type does not inherit from Throwable {}", clazz.getName());
			} else {
				// noinspection unchecked
				return (Class<? extends Throwable>) clazz;
			}
		} catch (Exception e) {
			logger.warn("Unable to load Throwable class {}", typeName);
		}
		return null;
	}

	private Constructor<? extends Throwable> getDefaultConstructor(Class<? extends Throwable> clazz) {
		Constructor<? extends Throwable> defaultCtr = null;
		try {
			defaultCtr = clazz.getConstructor();
		} catch (NoSuchMethodException t) { /* eat it */ }
		return defaultCtr;
	}

	private Constructor<? extends Throwable> getMessageConstructor(Class<? extends Throwable> clazz) {
		Constructor<? extends Throwable> messageCtr = null;
		try {
			messageCtr = clazz.getConstructor(String.class);
		} catch (NoSuchMethodException t) { /* eat it */ }
		return messageCtr;
	}
}
