package com.googlecode.jsonrpc4j;

import java.io.IOException;
import java.io.InputStream;

@SuppressWarnings({"WeakerAccess", "unused"})
class NoCloseInputStream extends InputStream {
	
	private final InputStream input;
	private boolean closeAttempted = false;
	
	public NoCloseInputStream(InputStream input) {
		this.input = input;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read() throws IOException {
		return this.input.read();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(byte[] b) throws IOException {
		return this.input.read(b);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return this.input.read(b, off, len);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public long skip(long n) throws IOException {
		return this.input.skip(n);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int available() throws IOException {
		return this.input.available();
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
	public synchronized void mark(int readLimit) {
		this.input.mark(readLimit);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void reset() throws IOException {
		this.input.reset();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean markSupported() {
		return this.input.markSupported();
	}
	
	/**
	 * @return the closeAttempted
	 */
	public boolean wasCloseAttempted() {
		return closeAttempted;
	}
	
}
