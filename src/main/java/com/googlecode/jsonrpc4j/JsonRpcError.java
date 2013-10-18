package com.googlecode.jsonrpc4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for mapping exceptions in service methods to
 * custom JsonRpc errors.
 *
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonRpcError {

	/**
	 * Exception handled by the annotation.
	 */
	Class<? extends Throwable> exception();

	/**
	 * The JsonRpc error code.
	 */
	int code();

	/**
	 * The JsonRpc error message.
	 */
	String message() default "";

	/**
	 * The JsonRpc error data. If data is not specified,
	 * message from exception will be used. If the message
	 * is null, the data element be omitted in the error.
	 */
	String data() default "";
}
