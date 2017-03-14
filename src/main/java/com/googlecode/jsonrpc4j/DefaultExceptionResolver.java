package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.googlecode.jsonrpc4j.Util.hasNonNullObjectData;
import static com.googlecode.jsonrpc4j.Util.hasNonNullTextualData;

/**
 * Default implementation of the {@link ExceptionResolver} interface that attempts to re-throw the same exception
 * that was thrown by the server. This always returns a {@link Throwable}.
 * The exception class must be present on the classpath.
 */
public class DefaultExceptionResolver implements ExceptionResolver {
	private static final Logger logger = LoggerFactory.getLogger(DefaultExceptionResolver.class);
	public static final DefaultExceptionResolver INSTANCE = new DefaultExceptionResolver();

	/**
	 * {@inheritDoc}
	 */
	public Throwable resolveException(ObjectNode response) {
		ObjectNode errorObject = ObjectNode.class.cast(response.get(JsonRpcBasicServer.ERROR));
		if (!hasNonNullObjectData(errorObject, JsonRpcBasicServer.DATA))
			return createJsonRpcClientException(errorObject);
		
		ObjectNode dataObject = ObjectNode.class.cast(errorObject.get(JsonRpcBasicServer.DATA));
		if (!hasNonNullTextualData(dataObject, JsonRpcBasicServer.EXCEPTION_TYPE_NAME))
			return createJsonRpcClientException(errorObject);
		
		try {
			String exceptionTypeName = dataObject.get(JsonRpcBasicServer.EXCEPTION_TYPE_NAME).asText();
			String message = hasNonNullTextualData(dataObject, JsonRpcBasicServer.ERROR_MESSAGE) ? dataObject.get(JsonRpcBasicServer.ERROR_MESSAGE).asText() : null;
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
		int code = errorObject.has(JsonRpcBasicServer.ERROR_CODE) ? errorObject.get(JsonRpcBasicServer.ERROR_CODE).asInt() : 0;
		return new JsonRpcClientException(code, errorObject.get(JsonRpcBasicServer.ERROR_MESSAGE).asText(), errorObject.get(JsonRpcBasicServer.DATA));
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
	private Throwable createThrowable(String typeName, String message) throws IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException {
		Class<? extends Throwable> clazz = resolveThrowableClass(typeName);

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

	/**
	 * Resolves original exception type name into an actual {@link Class}.
	 * Override this, if you want custom behaviour for handling exceptions,
	 * i.e.: Default to RuntimeException.
	 *
	 * @param typeName Original exception type name thrown on the server.
     * @return the resolved throwable
	 * @throws ClassNotFoundException - if throwable class has not been found
	 */
	protected Class<? extends Throwable> resolveThrowableClass(String typeName) throws ClassNotFoundException {
		Class<?> clazz;
		try {
			clazz = Class.forName(typeName);
			if (!Throwable.class.isAssignableFrom(clazz)) {
				logger.warn("Type does not inherit from Throwable {}", clazz.getName());
			} else {
				return clazz.asSubclass(Throwable.class);
			}
		} catch(ClassNotFoundException e) {
			logger.warn("Unable to load Throwable class {}", typeName);
			throw e;
		} catch(Exception e) {
			logger.warn("Unable to load Throwable class {}", typeName);
		}
		return null;
	}
	
	private Constructor<? extends Throwable> getDefaultConstructor(Class<? extends Throwable> clazz) {
		Constructor<? extends Throwable> defaultCtr = null;
		try {
			defaultCtr = clazz.getConstructor();
		} catch (NoSuchMethodException e) {
			handleException(e);
		}
		return defaultCtr;
	}
	
	private Constructor<? extends Throwable> getMessageConstructor(Class<? extends Throwable> clazz) {
		Constructor<? extends Throwable> messageCtr = null;
		try {
			messageCtr = clazz.getConstructor(String.class);
		} catch (NoSuchMethodException e) {
			handleException(e);
		}
		return messageCtr;
	}
	
	@SuppressWarnings("UnusedParameters")
	private void handleException(Exception e) {
	      /* do nothing */
	}
}
