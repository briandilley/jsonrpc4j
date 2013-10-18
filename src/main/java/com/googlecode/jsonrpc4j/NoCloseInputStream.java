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
