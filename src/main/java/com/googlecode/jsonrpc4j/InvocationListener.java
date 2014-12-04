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

/**
 * Implementations of this interface are able to be informed when JSON-RPC services are invoked.  This allows for
 * instrumentation of the invocations so that statistics about the invocations can be recorded and reported on.
 * @author Andrew Lindesay
 */

public interface InvocationListener {

    /**
     * This method will be invoked prior to a JSON-RPC service being invoked.
     * @param method is the method that will be invoked.
     * @param arguments are the arguments that will be passed to the method when it is invoked.
     */

    void willInvoke(Method method, List<JsonNode> arguments);

    /**
     * This method will be invoked after a JSON-RPC service has been invoked.
     * @param t is the throwable that was thrown from the invocation, if no error arose, this value
     *          will be null.
     * @param result is the result of the method invocation.  If an error arose, this value will be
     *               null.
     * @param method is the method that will was invoked.
     * @param arguments are the arguments that were be passed to the method when it is invoked.
     * @param duration is approximately the number of milliseconds that elapsed during which the method was invoked.
     */

    void didInvoke(Method method, List<JsonNode> arguments, Object result, Throwable t, long duration);

}
