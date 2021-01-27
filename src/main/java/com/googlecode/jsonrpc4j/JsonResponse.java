package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Contains the JSON-RPC answer in {@code response}
 * {@code exceptionToRethrow} contains exception, which should be thrown when property {@code rethrowExceptions}
 * is active
 */
public class JsonResponse {
    private JsonNode response;
    private int code;
    private RuntimeException exceptionToRethrow;

    public JsonResponse() {
    }

    public JsonResponse(JsonNode response, int code) {
        this.response = response;
        this.code = code;
    }

    public JsonNode getResponse() {
        return response;
    }

    public void setResponse(JsonNode response) {
        this.response = response;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public RuntimeException getExceptionToRethrow() {
        return exceptionToRethrow;
    }

    public void setExceptionToRethrow(RuntimeException exceptionToRethrow) {
        this.exceptionToRethrow = exceptionToRethrow;
    }
}
