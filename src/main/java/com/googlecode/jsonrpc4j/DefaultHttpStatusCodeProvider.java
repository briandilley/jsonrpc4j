package com.googlecode.jsonrpc4j;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.BULK_ERROR;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.CUSTOM_SERVER_ERROR_LOWER;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.CUSTOM_SERVER_ERROR_UPPER;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.ERROR_NOT_HANDLED;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.INTERNAL_ERROR;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.INVALID_REQUEST;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.METHOD_NOT_FOUND;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.METHOD_PARAMS_INVALID;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.PARSE_ERROR;

/**
 * This default implementation of a {@link HttpStatusCodeProvider} follows the rules defined in the
 * <a href="http://www.jsonrpc.org/historical/json-rpc-over-http.html">JSON-RPC over HTTP</a> document.
 */
public enum DefaultHttpStatusCodeProvider implements HttpStatusCodeProvider {
	INSTANCE;

	@Override
	public int getHttpStatusCode(int resultCode) {
		if (resultCode == 0) return HttpServletResponse.SC_OK;

		if (isErrorCode(resultCode)) {
			return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		} else if (resultCode == INVALID_REQUEST.code || resultCode == PARSE_ERROR.code) {
			return HttpServletResponse.SC_BAD_REQUEST;
		} else if (resultCode == METHOD_NOT_FOUND.code) {
			return HttpServletResponse.SC_NOT_FOUND;
		}

		return HttpServletResponse.SC_OK;
	}

	private boolean isErrorCode(int result) {
		for (ErrorResolver.JsonError error : Arrays.asList(INTERNAL_ERROR, METHOD_PARAMS_INVALID, ERROR_NOT_HANDLED, BULK_ERROR)) {
			if (error.code == result) return true;
		}
		return CUSTOM_SERVER_ERROR_UPPER >= result && result >= CUSTOM_SERVER_ERROR_LOWER;
	}
}
