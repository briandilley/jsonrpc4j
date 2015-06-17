package com.googlecode.jsonrpc4j.spring.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.jsonrpc4j.IJsonRpcClient;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import java.lang.reflect.Type;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class JsonRpcRestClient
	extends JsonRpcClient
	implements IJsonRpcClient {

	private URL serviceUrl;
	private RestTemplate restTemplate;

	private final Map<String, String> headers = new HashMap<String, String>();

	private SslClientHttpRequestFactory requestFactory = null;

	public JsonRpcRestClient(URL serviceUrl) {
		this(serviceUrl, new ObjectMapper());
	}

	public JsonRpcRestClient(URL serviceUrl, Map<String, String> headers) {
		this(serviceUrl, new ObjectMapper(), headers);
	}

	public JsonRpcRestClient(URL serviceUrl, ObjectMapper mapper, Map<String, String> headers) {
		this(serviceUrl, mapper, null, headers);
	}

	public JsonRpcRestClient(URL serviceUrl, ObjectMapper mapper) {
		this(serviceUrl, mapper, null, new HashMap<String, String>());
	}

	public JsonRpcRestClient(URL serviceUrl, RestTemplate restTemplate) {
		this(serviceUrl, new ObjectMapper(), restTemplate, null);
	}

	public JsonRpcRestClient(URL serviceUrl, ObjectMapper mapper, RestTemplate restTemplate, Map<String, String> headers) {
		super(mapper);
		this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
		this.serviceUrl = serviceUrl;

		if (headers != null) {
			this.headers.putAll(headers);
		}

		// Now check RestTemplate containts required converters
		this.initRestTemplate();

	}

	/**
	 * @param connectionProxy the connectionProxy to set
	 */
	public void setConnectionProxy(Proxy connectionProxy) {
		this.getRequestFactory().setProxy(connectionProxy);
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

	private SslClientHttpRequestFactory getRequestFactory() {
		if (this.requestFactory == null) {
			this.requestFactory = new SslClientHttpRequestFactory();
		}
		return requestFactory;
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

	/**
	 * @see IJsonRpcClient#invoke(java.lang.String, java.lang.Object) 
	 */
	@Override
	public void invoke(String methodName, Object argument)
		throws Throwable {
		invoke(methodName, argument, null, new HashMap<String, String>());
	}

	/**
	 * @see IJsonRpcClient#invoke(java.lang.String, java.lang.Object, java.lang.reflect.Type) 
	 */
	@Override
	public Object invoke(
		String methodName, Object argument, Type returnType)
		throws Throwable {
		return invoke(methodName, argument, returnType, new HashMap<String, String>());
	}

	/**
	 * @see IJsonRpcClient#invoke(java.lang.String, java.lang.Object, java.lang.Class) 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T invoke(
		String methodName, Object argument, Class<T> clazz)
		throws Throwable {
		return (T) invoke(methodName, argument, Type.class.cast(clazz));
	}

	/**
	 * @see IJsonRpcClient#invoke(java.lang.String, java.lang.Object, java.lang.Class, java.util.Map) 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T invoke(
		String methodName, Object argument, Class<T> clazz,
		Map<String, String> extraHeaders)
		throws Throwable {
		return (T) invoke(methodName, argument, Type.class.cast(clazz), extraHeaders);
	}

	/**
	 * @see IJsonRpcClient#invoke(java.lang.String, java.lang.Object, java.lang.reflect.Type, java.util.Map)
	 */
	@Override
	public Object invoke(
		String methodName, Object argument, Type returnType,
		Map<String, String> extraHeaders)
		throws Throwable {

		final ObjectNode request = super.createRequest(methodName, argument);
		MultiValueMap<String, String> httpHeaders = new LinkedMultiValueMap<String, String>();

		for (Map.Entry<String, String> entry : this.headers.entrySet()) {
			httpHeaders.add(entry.getKey(), entry.getValue());
		}

		if (extraHeaders != null) {
			for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
				httpHeaders.add(entry.getKey(), entry.getValue());
			}
		}

		// NB: Too bad code. May be it is better to always use external requestFactory?
		if (this.requestFactory != null && restTemplate.getRequestFactory() != this.requestFactory) {
			this.restTemplate.setRequestFactory(this.requestFactory);
		}

		final HttpEntity<?> requestHttpEntity = new HttpEntity(request, httpHeaders);
		final ResponseEntity<ObjectNode> responseEntity = this.restTemplate.postForEntity(this.serviceUrl.toURI(), requestHttpEntity, ObjectNode.class);
		final Object response = this.readResponse(returnType, responseEntity.getBody());

		return response;
	}

	/**
	 * Check RestTemplate containts required converters
	 */
	private void initRestTemplate() {

		boolean isContaintsConverter = false;
		for (HttpMessageConverter<?> httpMessageConverter : this.restTemplate.getMessageConverters()) {
			if (MappingJacksonRPC2HttpMessageConverter.class.isAssignableFrom(httpMessageConverter.getClass())) {
				isContaintsConverter = true;
				break;
			}
		}

		if (!isContaintsConverter) {

			final MappingJacksonRPC2HttpMessageConverter messageConverter = new MappingJacksonRPC2HttpMessageConverter();
			messageConverter.setObjectMapper(this.getObjectMapper());

			final List<HttpMessageConverter<?>> restMessageConverters = new ArrayList();
			restMessageConverters.addAll(this.restTemplate.getMessageConverters());
			// Place RPC converter on the first place!
			restMessageConverters.add(0, messageConverter);

			this.restTemplate.setMessageConverters(restMessageConverters);
		}

	}

}
