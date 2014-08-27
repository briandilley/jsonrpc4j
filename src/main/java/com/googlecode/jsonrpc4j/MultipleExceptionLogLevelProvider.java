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
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

/**
 * {@link com.googlecode.jsonrpc4j.ExceptionLogLevelProvider} that supports
 * the use of multiple {@link ExceptionLogLevelProvider}s used one after
 * another until one is able to provide a log level.
 */

public class MultipleExceptionLogLevelProvider implements ExceptionLogLevelProvider {

    private List<ExceptionLogLevelProvider> exceptionLogLevelProviders;

    /**
     * This constructor creates an instance with the given
     * {@link ExceptionLogLevelProvider}s.
     * {@link #addExceptionLogLevelProvider(ExceptionLogLevelProvider)}
     * can be called to add additional {@link ExceptionLogLevelProvider}s.
     * @param providers the {@link ExceptionLogLevelProvider}s to configure
     */
    public MultipleExceptionLogLevelProvider(ExceptionLogLevelProvider... providers) {
        this.exceptionLogLevelProviders = new LinkedList<ExceptionLogLevelProvider>();
        for (ExceptionLogLevelProvider provider : providers) {
            this.exceptionLogLevelProviders.add(provider);
        }
    }

    /**
     * Adds a provider at the end of the exception log level provider
     * list.
     * @param exceptionLogLevelProvider the exception log level provider to add
     * @throws java.lang.IllegalStateException if exceptionLogLevelProvider is null
     */

    public void addExceptionLogLevelProvider(ExceptionLogLevelProvider exceptionLogLevelProvider) {
        if(exceptionLogLevelProvider==null) {
            throw new IllegalStateException("it is not possible to add a null " + ExceptionLogLevelProvider.class.getSimpleName());
        }

        this.exceptionLogLevelProviders.add(exceptionLogLevelProvider);
    }

    /**
     * {@inheritDoc}
     */
    public Level logLevel(Throwable t, Method method, List<JsonNode> arguments) {

        Level level=null;

        for(ExceptionLogLevelProvider provider : exceptionLogLevelProviders) {
            level = provider.logLevel(t,method,arguments);

            if(level!=null) {
                return level;
            }
        }

        return level;
    }

}
