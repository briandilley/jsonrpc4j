package com.googlecode.jsonrpc4j;

/**
 * Data that is added to an error.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ErrorData {
	
	private final String exceptionTypeName;
	private final String message;
	
	/**
	 * Creates it.
	 *
	 * @param exceptionTypeName the exception type name
	 * @param message           the message
	 */
	public ErrorData(String exceptionTypeName, String message) {
		this.exceptionTypeName = exceptionTypeName;
		this.message = message;
	}
	
	/**
	 * @return the exceptionTypeName
	 */
	public String getExceptionTypeName() {
		return exceptionTypeName;
	}
	
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	
	@Override
	public String toString() {
		return "ErrorData{" + "exceptionTypeName='" + exceptionTypeName + '\'' +
				", message='" + message + '\'' +
				'}';
	}
}
