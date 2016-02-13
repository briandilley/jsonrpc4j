/*
 * The MIT License
 *
 * Copyright (c) 2014 jsonrpc4j
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jsonrpc4j.spring.rest;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import com.googlecode.jsonrpc4j.JsonRpcClient.RequestListener;
import com.googlecode.jsonrpc4j.ReflectionUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.remoting.support.UrlBasedRemoteAccessor;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author toha
 * @param <T> the bean type
 */
public class JsonRestProxyFactoryBean<T>
	extends UrlBasedRemoteAccessor
	implements MethodInterceptor,
	InitializingBean,
	FactoryBean<T>,
	ApplicationContextAware {

    
	private T				    proxyObject			= null;
	private RequestListener		requestListener		= null;
	private ObjectMapper		objectMapper		= null;
	private RestTemplate		restTemplate		= null;
	private JsonRpcRestClient	jsonRpcRestClient	= null;
	private Map<String, String>	extraHttpHeaders	= new HashMap<String, String>();
    
	private SSLContext sslContext 				= null;
	private HostnameVerifier hostNameVerifier 	= null;
    
	private ApplicationContext	applicationContext;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		// create proxy
		proxyObject = ProxyFactory.getProxy(getObjectType(), this);

		// find the ObjectMapper
		if (objectMapper == null
			&& applicationContext != null
			&& applicationContext.containsBean("objectMapper")) {
			objectMapper = (ObjectMapper) applicationContext.getBean("objectMapper");
		}
		if (objectMapper == null && applicationContext != null) {
			try {
				objectMapper = BeanFactoryUtils
					.beanOfTypeIncludingAncestors(applicationContext, ObjectMapper.class);
			} catch (Exception e) { /* no-op */ }
		}
		if (objectMapper==null) {
			objectMapper = new ObjectMapper();
		}

		// create JsonRpcHttpClient
		try {
            jsonRpcRestClient = new JsonRpcRestClient(new URL(getServiceUrl()), objectMapper, restTemplate, new HashMap<String, String>());
			jsonRpcRestClient.setRequestListener(requestListener);
            jsonRpcRestClient.setSslContext(sslContext);
            jsonRpcRestClient.setHostNameVerifier(hostNameVerifier);
		} catch (MalformedURLException mue) {
			throw new RuntimeException(mue);
		}
	}

	/**
	 * {@inheritDoc}
	 */
    @Override
	public Object invoke(MethodInvocation invocation)
		throws Throwable {

		// handle toString()
		Method method = invocation.getMethod();
		if (method.getDeclaringClass() == Object.class && method.getName().equals("toString")) {
			return proxyObject.getClass().getName() + "@" + System.identityHashCode(proxyObject);
		}

		// get return type
		Type retType = (invocation.getMethod().getGenericReturnType() != null)
			? invocation.getMethod().getGenericReturnType()
			: invocation.getMethod().getReturnType();

		// get arguments
		Object arguments = ReflectionUtil.parseArguments(
			invocation.getMethod(), invocation.getArguments());

		// invoke it
		return jsonRpcRestClient.invoke(
			invocation.getMethod().getName(),
			arguments,
			retType, extraHttpHeaders);
	}

	/**
	 * {@inheritDoc}
	 */
    @Override
	public T getObject() {
		return proxyObject;
	}

	/**
	 * {@inheritDoc}
	 */
    @SuppressWarnings("unchecked")
    @Override
	public Class<T> getObjectType() {
		return (Class<T>) getServiceInterface();
	}

	/**
	 * {@inheritDoc}
	 */
    @Override
	public boolean isSingleton() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
    @Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * @param objectMapper the objectMapper to set
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * @param extraHttpHeaders the extraHttpHeaders to set
	 */
	public void setExtraHttpHeaders(Map<String, String> extraHttpHeaders) {
		this.extraHttpHeaders = extraHttpHeaders;
	}

	/**
	 * @param requestListener the requestListener to set
	 */
	public void setRequestListener(JsonRpcClient.RequestListener requestListener) {
		this.requestListener = requestListener;
	}

    /**
     * @param sslContext SSL contest for JsonRpcClient
     */
    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

	/**
	 * @param hostNameVerifier the hostNameVerifier to pass to JsonRpcClient
	 */
    public void setHostNameVerifier(HostnameVerifier hostNameVerifier)   {
        this.hostNameVerifier = hostNameVerifier;
    }
    
    /**
     * @param restTemplate externak RestTemplate
     */
    public void setRestTemplate(RestTemplate restTemplate)  {
        this.restTemplate = restTemplate;
    }
    
}
