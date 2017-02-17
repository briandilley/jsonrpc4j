package com.googlecode.jsonrpc4j;

import java.io.IOException;

/**
 * @author brian
 */
@SuppressWarnings({"serial", "WeakerAccess", "unused"})
class StreamEndedException extends IOException {
	
	public StreamEndedException() {
	}
	
	/**
	 * @param message the detail message
	 */
	public StreamEndedException(String message) {
		super(message);
	}
	
	/**
	 * @param cause the cause (a null value is permitted, and indicates that the cause is nonexistent or unknown)
	 */
	public StreamEndedException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * @param message the detail message
	 * @param cause   the cause (a null value is permitted, and indicates that the cause is nonexistent or unknown)
	 */
	public StreamEndedException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
