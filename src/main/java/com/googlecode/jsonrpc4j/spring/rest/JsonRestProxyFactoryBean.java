package com.googlecode.jsonrpc4j.spring.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.ExceptionResolver;
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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @param <T> the bean type
 * @author toha
 */
@SuppressWarnings("unused")
class JsonRestProxyFactoryBean<T> extends UrlBasedRemoteAccessor implements MethodInterceptor, InitializingBean, FactoryBean<T>, ApplicationContextAware {

	private T proxyObject = null;
	private RequestListener requestListener = null;
	private ObjectMapper objectMapper = null;
	private RestTemplate restTemplate = null;
	private JsonRpcRestClient jsonRpcRestClient = null;
	private Map<String, String> extraHttpHeaders = new HashMap<>();

	private SSLContext sslContext = null;
	private HostnameVerifier hostNameVerifier = null;

	
	private ExceptionResolver exceptionResolver; 
	
	private ApplicationContext applicationContext;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		proxyObject = ProxyFactory.getProxy(getObjectType(), this);

		if (jsonRpcRestClient==null) {
			
			if (objectMapper == null && applicationContext != null && applicationContext.containsBean("objectMapper")) {
				objectMapper = (ObjectMapper) applicationContext.getBean("objectMapper");
			
			}
			if (objectMapper == null && applicationContext != null) {
				try {
					objectMapper = BeanFactoryUtils.beanOfTypeIncludingAncestors(applicationContext, ObjectMapper.class);
				} catch (Exception e) {
					logger.debug(e);
				}
			}
			
			if (objectMapper == null) {
				objectMapper = new ObjectMapper();
			}

			try {
				jsonRpcRestClient = new JsonRpcRestClient(new URL(getServiceUrl()), objectMapper, restTemplate, new HashMap<String, String>());
				jsonRpcRestClient.setRequestListener(requestListener);
				jsonRpcRestClient.setSslContext(sslContext);
				jsonRpcRestClient.setHostNameVerifier(hostNameVerifier);
				
				if (exceptionResolver!=null) {
					jsonRpcRestClient.setExceptionResolver(exceptionResolver);
				}
				
			} catch (MalformedURLException mue) {
				throw new RuntimeException(mue);
			}
			
		}
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		if (method.getDeclaringClass() == Object.class && method.getName().equals("toString")) {
			return proxyObject.getClass().getName() + "@" + System.identityHashCode(proxyObject);
		}

		Type retType = (invocation.getMethod().getGenericReturnType() != null) ? invocation.getMethod().getGenericReturnType() : invocation.getMethod().getReturnType();
		Object arguments = ReflectionUtil.parseArguments(invocation.getMethod(), invocation.getArguments());

		return jsonRpcRestClient.invoke(invocation.getMethod().getName(), arguments, retType, extraHttpHeaders);
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
	public void setHostNameVerifier(HostnameVerifier hostNameVerifier) {
		this.hostNameVerifier = hostNameVerifier;
	}

	/**
	 * @param restTemplate external RestTemplate
	 */
	public void setRestTemplate(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public void setJsonRpcRestClient(JsonRpcRestClient jsonRpcRestClient) {
		this.jsonRpcRestClient = jsonRpcRestClient;
	}

	public void setExceptionResolver(ExceptionResolver exceptionResolver) {
		this.exceptionResolver = exceptionResolver;
	}

	
	
}
