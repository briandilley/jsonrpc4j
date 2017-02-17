package com.googlecode.jsonrpc4j;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to define the path of a JSON-RPC service.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface JsonRpcService {
	
	/**
	 * The path that the service is available at.
	 *
	 * @return the service path
	 */
	String value();
}
