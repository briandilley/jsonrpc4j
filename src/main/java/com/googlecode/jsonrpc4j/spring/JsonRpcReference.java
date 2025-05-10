package com.googlecode.jsonrpc4j.spring;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JsonRpcReference {

    String address() default "";
}
