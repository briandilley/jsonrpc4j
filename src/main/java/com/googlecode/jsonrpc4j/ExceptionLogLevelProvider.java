/*
The MIT License (MIT)

Copyright (c) 2014 jsonrpc4j

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;

/**
 * Implementations of this interface may be able to provide a log level for
 * an exception which has occurred as a result of handling a method invocation.
 * Implementations can be configured on the
 * {@link com.googlecode.jsonrpc4j.JsonRpcServer} class.
 */

public interface ExceptionLogLevelProvider {

    /**
     * Implementations of this method should return a log level that indicates
     * the logging level of the thrown throwable.  If the method has no opinion
     * about the logging level of the throwable then it should return null.
     *
     * @param t the {@link Throwable}
     * @param method the {@link Method} that threw the {@link Throwable}
     * @param arguments the {@code arguments} that were passed to the {@link Method}
     * @return the {@link Level} or null if there is no opinion about the level
     */

    Level logLevel(Throwable t, Method method, List<JsonNode> arguments);

}
