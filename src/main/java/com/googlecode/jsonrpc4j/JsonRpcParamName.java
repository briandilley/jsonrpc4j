package com.googlecode.jsonrpc4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for annotating service parameters as
 * JsonRpc params by name.  This has been deprecated
 * in favor of the {@link JsonRpcParam} annotation
 * because in the future it might contain more than
 * just the param name.
 * 
 * @deprecated use {@link JsonRpcParam} instead
 *
 */
@Deprecated()
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonRpcParamName {

	/**
	 * The value of the parameter name.
	 */
	String value();

}
