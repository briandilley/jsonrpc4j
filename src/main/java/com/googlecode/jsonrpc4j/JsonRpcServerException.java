package com.googlecode.jsonrpc4j;

/**
 * Exception thrown by a JSON-RPC server when an error occurs.
 *
 */
public class JsonRpcServerException extends RuntimeException {

    private final int code;
    private final Object data;

    /**
     * Creates the exception.
     *
     * @param code    the code from the server
     * @param message the message from the server
     * @param data    the data from the server
     */
    public JsonRpcServerException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    /**
     * Creates the exception.
     *
     * @param code    the code from the server
     * @param message the message from the server
     * @param data    the data from the server
     * @param cause   the cause
     */
    public JsonRpcServerException(int code, String message, Object data, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.data = data;
    }

    /**
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /**
     * @return the data
     */
    public Object getData() {
        return data;
    }
}
