package com.googlecode.jsonrpc4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonRpcFixedParam {

    /**
     * @return the parameter's name.
     */
    String name();

    /**
     * @return the parameter's value.
     */
    String value();
}
