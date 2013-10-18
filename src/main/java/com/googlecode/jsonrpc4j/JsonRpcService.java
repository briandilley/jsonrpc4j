package com.googlecode.jsonrpc4j;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to define the path of a JSON-RPC service.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface JsonRpcService {

	/**
	 * The path that the service is available at.
	 * @return the path
	 */
	String value();

	/**
	 * Whether or not to use named parameters.
	 * @return
	 */
	boolean useNamedParams() default false;

}
