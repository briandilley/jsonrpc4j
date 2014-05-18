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
import java.io.InputStream;

public class NoCloseInputStream
	extends InputStream {

	private InputStream ips;
	private boolean closeAttempted = false;

	public NoCloseInputStream(InputStream ips) {
		this.ips = ips;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read() throws IOException {
		return this.ips.read();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(byte[] b) throws IOException {
		return this.ips.read(b);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return this.ips.read(b, off, len);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long skip(long n) throws IOException {
		return this.ips.skip(n);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int available() throws IOException {
		return this.ips.available();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		closeAttempted = true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void mark(int readlimit) {
		this.ips.mark(readlimit);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void reset() throws IOException {
		this.ips.reset();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean markSupported() {
		return this.ips.markSupported();
	}

	/**
	 * @return the closeAttempted
	 */
	public boolean wasCloseAttempted() {
		return closeAttempted;
	}

}
