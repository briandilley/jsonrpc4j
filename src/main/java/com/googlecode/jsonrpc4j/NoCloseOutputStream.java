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

import java.io.IOException;
import java.io.OutputStream;

public class NoCloseOutputStream
	extends OutputStream {

	private OutputStream ops;
	private boolean closeAttempted = false;

	public NoCloseOutputStream(OutputStream ops) {
		this.ops = ops;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(int b) throws IOException {
		this.ops.write(b);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(byte[] b) throws IOException {
		this.ops.write(b);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		this.ops.write(b, off, len);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flush() throws IOException {
		this.ops.flush();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		closeAttempted = true;
	}

	/**
	 * @return the closeAttempted
	 */
	public boolean wasCloseAttempted() {
		return closeAttempted;
	}

}
