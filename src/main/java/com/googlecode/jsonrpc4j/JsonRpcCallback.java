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

/**
 * This interface is used by the JsonRpcHttpAsyncClient for recieving
 * RPC responses.  When an invocation is made, one of {@code onComplete()}
 * or {@code onError()} is guaranteed to be called.
 *
 * @author Brett Wooldridge
 *
 * @param <T> the return type of the JSON-RPC call
 */
public interface JsonRpcCallback<T> {

        /**
         * Called if the remote invocation was successful.
         *
         * @param result the result object of the call (possibly null)
         */
        void onComplete(T result);

        /**
         * Called if there was an error in the remove invocation.
         *
         * @param t the {@code Throwable} (possibly wrapping) the invocation error
         */
        void onError(Throwable t);
}
