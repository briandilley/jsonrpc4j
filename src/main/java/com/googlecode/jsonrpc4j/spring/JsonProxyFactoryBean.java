package com.googlecode.jsonrpc4j.spring;

import com.googlecode.jsonrpc4j.JsonRpcClient.RequestListener;
import com.googlecode.jsonrpc4j.ExceptionResolver;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.ReflectionUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import tools.jackson.databind.json.JsonMapper;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link FactoryBean} for creating a JSON-RPC client proxy
 * (aka consumer) for accessing an HTTP based JSON-RPC service.
 */
@SuppressWarnings("unused")
public class JsonProxyFactoryBean implements MethodInterceptor, InitializingBean, FactoryBean<Object>, ApplicationContextAware {

	private static final Logger logger = LoggerFactory.getLogger(JsonProxyFactoryBean.class);

	private Class<?> serviceInterface;
	private String serviceUrl;

	private Object proxyObject = null;
	private RequestListener requestListener = null;
	private JsonMapper objectMapper = null;
	private JsonRpcHttpClient jsonRpcHttpClient = null;
	private Map<String, String> extraHttpHeaders = new HashMap<>();
	private String contentType;

	private SSLContext sslContext = null;
	private HostnameVerifier hostNameVerifier = null;
	
	private ExceptionResolver exceptionResolver; 

	private ApplicationContext applicationContext;

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() {
		if (serviceInterface == null) {
			throw new IllegalArgumentException("Property 'serviceInterface' is required");
		}
		if (serviceUrl == null) {
			throw new IllegalArgumentException("Property 'serviceUrl' is required");
		}
		proxyObject = ProxyFactory.getProxy(getServiceInterface(), this);

		if (jsonRpcHttpClient==null) {
			if (objectMapper == null && applicationContext != null && applicationContext.containsBean("objectMapper")) {
				objectMapper = (JsonMapper) applicationContext.getBean("objectMapper");
			}
			if (objectMapper == null && applicationContext != null) {
				try {
					objectMapper = BeanFactoryUtils.beanOfTypeIncludingAncestors(applicationContext, JsonMapper.class);
				} catch (Exception e) {
					logger.debug(e.getMessage(), e);
				}
			}
			if (objectMapper == null) {
				objectMapper = new JsonMapper();
			}
	
			try {
				jsonRpcHttpClient = new JsonRpcHttpClient(objectMapper, new URL(getServiceUrl()), extraHttpHeaders);
				jsonRpcHttpClient.setRequestListener(requestListener);
				jsonRpcHttpClient.setSslContext(sslContext);
				jsonRpcHttpClient.setHostNameVerifier(hostNameVerifier);
	
				if (contentType != null) {
					jsonRpcHttpClient.setContentType(contentType);
				}
				
				if (exceptionResolver!=null) {
					jsonRpcHttpClient.setExceptionResolver(exceptionResolver);
				}
			} catch (MalformedURLException mue) {
				throw new RuntimeException(mue);
			}
		}

		ReflectionUtil.clearCache();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object invoke(MethodInvocation invocation)
			throws Throwable {
		Method method = invocation.getMethod();
		if (method.getDeclaringClass() == Object.class && method.getName().equals("toString")) {
			return proxyObject.getClass().getName() + "@" + System.identityHashCode(proxyObject);
		}

		Type retType = (invocation.getMethod().getGenericReturnType() != null) ? invocation.getMethod().getGenericReturnType() : invocation.getMethod().getReturnType();
		Object arguments = ReflectionUtil.parseArguments(invocation.getMethod(), invocation.getArguments());

		return jsonRpcHttpClient.invoke(ProxyUtil.getMethodName(method), arguments, retType, extraHttpHeaders);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getObject() {
		return proxyObject;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?> getObjectType() {
		return getServiceInterface();
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
	public void setObjectMapper(JsonMapper objectMapper) {
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
	public void setRequestListener(RequestListener requestListener) {
		this.requestListener = requestListener;
	}

	/**
	 * @param sslContext SSL context to pass to JsonRpcClient
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
	 * @param contentType the contentType to pass to JsonRpcClient
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void setJsonRpcHttpClient(JsonRpcHttpClient jsonRpcHttpClient) {
		this.jsonRpcHttpClient = jsonRpcHttpClient;
	}

	public void setExceptionResolver(ExceptionResolver exceptionResolver) {
		this.exceptionResolver = exceptionResolver;
	}

	/**
	 * @return the service interface for the proxy
	 */
	public Class<?> getServiceInterface() {
		return serviceInterface;
	}

	/**
	 * @param serviceInterface the service interface to proxy
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		if (serviceInterface != null && !serviceInterface.isInterface()) {
			throw new IllegalArgumentException("'serviceInterface' must be an interface");
		}
		this.serviceInterface = serviceInterface;
	}

	/**
	 * @return the URL of the remote service
	 */
	public String getServiceUrl() {
		return serviceUrl;
	}

	/**
	 * @param serviceUrl the URL of the remote service
	 */
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

}
