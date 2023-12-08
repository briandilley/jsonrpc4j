package com.googlecode.jsonrpc4j.spring.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

public class JsonRpcResponseErrorHandler
		implements ResponseErrorHandler {
	
	public static final JsonRpcResponseErrorHandler INSTANCE =  new JsonRpcResponseErrorHandler();
	
    private  final Logger logger = LoggerFactory.getLogger(this.getClass());
    
	/**
	 * for supported codes see {@link com.googlecode.jsonrpc4j.DefaultHttpStatusCodeProvider}
	 */
	private final Set<Integer> JSON_RPC_STATUES = new HashSet<Integer>();
	
	
	private JsonRpcResponseErrorHandler() {
		JSON_RPC_STATUES.add(HttpURLConnection.HTTP_INTERNAL_ERROR);
		JSON_RPC_STATUES.add(HttpURLConnection.HTTP_BAD_REQUEST);
		JSON_RPC_STATUES.add(HttpURLConnection.HTTP_NOT_FOUND);
	}
	
	@Override
	public boolean hasError(ClientHttpResponse response)
			throws IOException {

		final HttpStatusCode httpStatus = getHttpStatusCode(response);
		
		if (JSON_RPC_STATUES.contains(httpStatus.value())) {
			// Checks the content type. If application/json-rpc then allow handler to read message
			final MediaType contentType = response.getHeaders().getContentType();
			if (MappingJacksonRPC2HttpMessageConverter.APPLICATION_JSON_RPC.isCompatibleWith(contentType))
				return false;
		}

		return httpStatus.is4xxClientError() || httpStatus.is5xxServerError();
	}
	
	@Override
	public void handleError(ClientHttpResponse response)
			throws IOException {
		final HttpStatusCode statusCode = getHttpStatusCode(response);

		if (statusCode.is4xxClientError()) {
			throw new HttpClientErrorException(statusCode, response.getStatusText(), response.getHeaders(), getResponseBody(response), getCharset(response));
		} else if (statusCode.is5xxServerError()) {
			throw new HttpServerErrorException(statusCode, response.getStatusText(), response.getHeaders(), getResponseBody(response), getCharset(response));
		} else {
			throw new RestClientException("Unknown status code [" + statusCode + "]");
		}
	}


	private HttpStatusCode getHttpStatusCode(ClientHttpResponse response) throws IOException {
		final HttpStatusCode statusCode;
		try {
			statusCode = response.getStatusCode();
		} catch (IllegalArgumentException ex) {
			throw new UnknownHttpStatusCodeException(response.getStatusCode().value(),
					response.getStatusText(), response.getHeaders(), getResponseBody(response), getCharset(response));
		}
		return statusCode;
	}
	
	private byte[] getResponseBody(ClientHttpResponse response) {
		try {
			final InputStream responseBody = response.getBody();
			if (responseBody != null) {
				return FileCopyUtils.copyToByteArray(responseBody);
			}
		} catch (IOException ex) {
            // No body in response 401 for example
            logger.debug("Can not read resonse body", ex);
		}
		return new byte[0];
	}
	
	private Charset getCharset(ClientHttpResponse response) {
		HttpHeaders headers = response.getHeaders();
		MediaType contentType = headers.getContentType();
		return contentType != null ? contentType.getCharset() : null;
	}
	
}
