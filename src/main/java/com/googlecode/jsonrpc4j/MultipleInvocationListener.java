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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link com.googlecode.jsonrpc4j.InvocationListener} that supports the use
 * of multiple {@link InvocationListener}s called one after another.
 * @author Andrew Lindesay
 */

public class MultipleInvocationListener
        implements InvocationListener {

    private List<InvocationListener> invocationListeners;

    /**
     * Creates with the given {@link InvocationListener}s,
     * {@link #addInvocationListener(InvocationListener)} can be called to
     * add additional {@link InvocationListener}s.
     * @param invocationListeners the {@link InvocationListener}s
     */
    public MultipleInvocationListener(InvocationListener... invocationListeners) {
        this.invocationListeners = new LinkedList<InvocationListener>();
        Collections.addAll(this.invocationListeners, invocationListeners);
    }

    /**
     * Adds an {@link InvocationListener} to the end of the
     * list of invocation listeners.
     * @param invocationListener the {@link InvocationListener} to add
     */
    public void addInvocationListener(InvocationListener invocationListener) {
        this.invocationListeners.add(invocationListener);
    }

    /**
     * {@inheritDoc}
     */
    public void willInvoke(Method method, List<JsonNode> arguments) {
        for(InvocationListener invocationListener : invocationListeners) {
            invocationListener.willInvoke(method, arguments);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void didInvoke(Method method, List<JsonNode> arguments, Object result, Throwable t, long duration) {
        for(InvocationListener invocationListener : invocationListeners) {
            invocationListener.didInvoke(method, arguments, result, t, duration);
        }
    }

}
