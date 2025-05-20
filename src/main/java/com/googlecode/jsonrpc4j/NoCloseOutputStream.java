package com.googlecode.jsonrpc4j;

import java.io.IOException;
import java.io.OutputStream;

@SuppressWarnings({"WeakerAccess", "unused"})
class NoCloseOutputStream extends OutputStream {
	
	private final OutputStream ops;
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
	    // Validate input parameters
	    if (b == null) {
	        throw new NullPointerException("Input byte array cannot be null");
	    }
	    
	    if (off < 0 || len < 0 || off + len > b.length) {
	        throw new ArrayIndexOutOfBoundsException("Invalid offset or length parameters");
	    }
	    
	    // Only perform the write operation after validation
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
