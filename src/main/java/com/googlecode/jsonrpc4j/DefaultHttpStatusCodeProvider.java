package com.googlecode.jsonrpc4j;

import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.BULK_ERROR;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.CUSTOM_SERVER_ERROR_LOWER;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.CUSTOM_SERVER_ERROR_UPPER;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.ERROR_NOT_HANDLED;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.INTERNAL_ERROR;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.INVALID_REQUEST;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.METHOD_NOT_FOUND;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.METHOD_PARAMS_INVALID;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.PARSE_ERROR;
import java.net.HttpURLConnection;

import java.util.Arrays;

/**
 * This default implementation of a {@link HttpStatusCodeProvider} follows the rules defined in the
 * <a href="http://www.jsonrpc.org/historical/json-rpc-over-http.html">JSON-RPC over HTTP</a> document.
 */
@SuppressWarnings("WeakerAccess")
public enum DefaultHttpStatusCodeProvider implements HttpStatusCodeProvider {
	INSTANCE;

	@Override
	public int getHttpStatusCode(int resultCode) {
		if (resultCode == 0) return HttpURLConnection.HTTP_OK; // Toha: pure java constants

		if (isErrorCode(resultCode)) {
			return HttpURLConnection.HTTP_INTERNAL_ERROR;
		} else if (resultCode == INVALID_REQUEST.code || resultCode == PARSE_ERROR.code) {
			return HttpURLConnection.HTTP_BAD_REQUEST;
		} else if (resultCode == METHOD_NOT_FOUND.code) { return HttpURLConnection.HTTP_NOT_FOUND; }

		return HttpURLConnection.HTTP_OK;
	}

	private boolean isErrorCode(int result) {
		for (ErrorResolver.JsonError error : Arrays.asList(INTERNAL_ERROR, METHOD_PARAMS_INVALID, ERROR_NOT_HANDLED, BULK_ERROR)) {
			if (error.code == result) return true;
		}
		return CUSTOM_SERVER_ERROR_UPPER >= result && result >= CUSTOM_SERVER_ERROR_LOWER;
	}
}
