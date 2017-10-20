package com.googlecode.jsonrpc4j.spring.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.jsonrpc4j.DefaultHttpStatusCodeProvider;
import com.googlecode.jsonrpc4j.IJsonRpcClient;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import com.googlecode.jsonrpc4j.JsonRpcClientException;
import org.springframework.http.HttpEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.lang.reflect.Type;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({"unused", "WeakerAccess"})
public class JsonRpcRestClient extends JsonRpcClient implements IJsonRpcClient {

	private final AtomicReference<URL> serviceUrl = new AtomicReference<>();
	private final RestTemplate restTemplate;

	private final Map<String, String> headers = new HashMap<>();

	private final SslClientHttpRequestFactory requestFactory;

	public JsonRpcRestClient(URL serviceUrl) {
		this(serviceUrl, new ObjectMapper());
	}

	private JsonRpcRestClient(URL serviceUrl, ObjectMapper mapper) {
		this(serviceUrl, mapper, null, new HashMap<String, String>());
	}

	@SuppressWarnings("WeakerAccess")
	public JsonRpcRestClient(URL serviceUrl, ObjectMapper mapper, RestTemplate restTemplate, Map<String, String> headers) {
		super(mapper);
		this.requestFactory = restTemplate != null ? null : new SslClientHttpRequestFactory();
		this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate(this.requestFactory);
		this.serviceUrl.set(serviceUrl);

		if (headers != null) {
			this.headers.putAll(headers);
		}

		// Now check RestTemplate contains required converters
		this.initRestTemplate();

	}

	/**
	 * Check RestTemplate contains required converters
	 */
	private void initRestTemplate() {

		boolean isContainsConverter = false;
		for (HttpMessageConverter<?> httpMessageConverter : this.restTemplate.getMessageConverters()) {
			if (MappingJacksonRPC2HttpMessageConverter.class.isAssignableFrom(httpMessageConverter.getClass())) {
				isContainsConverter = true;
				break;
			}
		}

		if (!isContainsConverter) {

			final MappingJacksonRPC2HttpMessageConverter messageConverter = new MappingJacksonRPC2HttpMessageConverter();
			messageConverter.setObjectMapper(this.getObjectMapper());

			final List<HttpMessageConverter<?>> restMessageConverters = new ArrayList<>();
			restMessageConverters.addAll(this.restTemplate.getMessageConverters());
			// Place JSON-RPC converter on the first place!
			restMessageConverters.add(0, messageConverter);

			this.restTemplate.setMessageConverters(restMessageConverters);
		}

		// use specific JSON-RPC error handler if it has not been changed to custom 
		if (restTemplate.getErrorHandler() instanceof org.springframework.web.client.DefaultResponseErrorHandler) {
			restTemplate.setErrorHandler(JsonRpcResponseErrorHandler.INSTANCE);
		}
	}

	public JsonRpcRestClient(URL serviceUrl, Map<String, String> headers) {
		this(serviceUrl, new ObjectMapper(), headers);
	}

	private JsonRpcRestClient(URL serviceUrl, ObjectMapper mapper, Map<String, String> headers) {
		this(serviceUrl, mapper, null, headers);
	}

	public JsonRpcRestClient(URL serviceUrl, RestTemplate restTemplate) {
		this(serviceUrl, new ObjectMapper(), restTemplate, null);
	}

	/**
	 * @param connectionProxy the connectionProxy to set
	 */
	public void setConnectionProxy(Proxy connectionProxy) {
		this.getRequestFactory().setProxy(connectionProxy);
	}

	private SslClientHttpRequestFactory getRequestFactory() {
		if (this.requestFactory == null) {
			throw new IllegalStateException("Used external RequestTemplate instance");
		}
		return requestFactory;
	}

	/**
	 * @param connectionTimeoutMillis the connectionTimeoutMillis to set
	 */
	public void setConnectionTimeoutMillis(int connectionTimeoutMillis) {
		this.getRequestFactory().setConnectTimeout(connectionTimeoutMillis);
	}

	/**
	 * @param readTimeoutMillis the readTimeoutMillis to set
	 */
	public void setReadTimeoutMillis(int readTimeoutMillis) {
		this.getRequestFactory().setReadTimeout(readTimeoutMillis);
	}

	/**
	 * @return the headers
	 */
	public Map<String, String> getHeaders() {
		return Collections.unmodifiableMap(headers);
	}

	/**
	 * @param headers the headers to set
	 */
	public void setHeaders(Map<String, String> headers) {
		this.headers.clear();
		this.headers.putAll(headers);
	}

	/**
	 * @param sslContext the sslContext to set
	 */
	public void setSslContext(SSLContext sslContext) {
		if (sslContext != null) {
			this.getRequestFactory().setSslContext(sslContext);
		}
	}

	/**
	 * @param hostNameVerifier the hostNameVerifier to set
	 */
	public void setHostNameVerifier(HostnameVerifier hostNameVerifier) {
		if (hostNameVerifier != null) {
			this.getRequestFactory().setHostNameVerifier(hostNameVerifier);
		}
	}

	public URL getServiceUrl() {
		return serviceUrl.get();
	}

	public void setServiceUrl(URL serviceUrl) {
		this.serviceUrl.set(serviceUrl);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void invoke(String methodName, Object argument) throws Throwable {
		invoke(methodName, argument, null, new HashMap<String, String>());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object invoke(String methodName, Object argument, Type returnType) throws Throwable {
		return invoke(methodName, argument, returnType, new HashMap<String, String>());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object invoke(String methodName, Object argument, Type returnType, Map<String, String> extraHeaders) throws Throwable {

		final ObjectNode request = super.createRequest(methodName, argument);
		final MultiValueMap<String, String> httpHeaders = new LinkedMultiValueMap<>();

		for (Map.Entry<String, String> entry : this.headers.entrySet()) {
			httpHeaders.add(entry.getKey(), entry.getValue());
		}

		if (extraHeaders != null) {
			for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
				httpHeaders.add(entry.getKey(), entry.getValue());
			}
		}

		final HttpEntity<ObjectNode> requestHttpEntity = new HttpEntity<>(request, httpHeaders);
		JsonNode response;

		try {
			response = this.restTemplate.postForObject(serviceUrl.get().toExternalForm(), requestHttpEntity, ObjectNode.class);
		} catch (HttpStatusCodeException httpStatusCodeException) {
			logger.error("HTTP Error code={} status={}\nresponse={}"
					, httpStatusCodeException.getStatusCode().value()
					, httpStatusCodeException.getStatusText()
					, httpStatusCodeException.getResponseBodyAsString()
			);
			Integer jsonErrorCode = DefaultHttpStatusCodeProvider.INSTANCE.getJsonRpcCode(httpStatusCodeException.getStatusCode().value());
			if (jsonErrorCode == null) {
				jsonErrorCode = httpStatusCodeException.getStatusCode().value();
			}
			throw new JsonRpcClientException(jsonErrorCode, httpStatusCodeException.getStatusText(), null);
		} catch (HttpMessageConversionException httpMessageConversionException) {
			logger.error("Can not convert (request/response)", httpMessageConversionException);
			throw new JsonRpcClientException(0, "Invalid JSON-RPC response", null);
		}

		return this.readResponse(returnType, response);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T invoke(String methodName, Object argument, Class<T> clazz) throws Throwable {
		return (T) invoke(methodName, argument, Type.class.cast(clazz));
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T invoke(String methodName, Object argument, Class<T> clazz, Map<String, String> extraHeaders) throws Throwable {
		return (T) invoke(methodName, argument, Type.class.cast(clazz), extraHeaders);
	}

}
