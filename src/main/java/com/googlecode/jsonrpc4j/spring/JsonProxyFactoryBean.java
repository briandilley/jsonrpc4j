package com.googlecode.jsonrpc4j.spring;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.remoting.support.UrlBasedRemoteAccessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.JsonRpcClient.RequestListener;
import com.googlecode.jsonrpc4j.ReflectionUtil;

/**
 * {@link FactoryBean} for creating a {@link UrlBasedRemoteAccessor}
 * (aka consumer) for accessing an HTTP based JSON-RPC service.
 *
 */
public class JsonProxyFactoryBean
	extends UrlBasedRemoteAccessor
	implements MethodInterceptor,
	InitializingBean,
	FactoryBean<Object>,
	ApplicationContextAware {

	private boolean				useNamedParams		= false;
	private Object				proxyObject			= null;
	private RequestListener		requestListener		= null;
	private ObjectMapper		objectMapper		= null;
	private JsonRpcHttpClient	jsonRpcHttpClient	= null;
	private Map<String, String>	extraHttpHeaders	= new HashMap<String, String>();
	private ApplicationContext	applicationContext;

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		// create proxy
		proxyObject = ProxyFactory.getProxy(getServiceInterface(), this);

		// find the ObjectMapper
		if (objectMapper == null
			&& applicationContext != null
			&& applicationContext.containsBean("objectMapper")) {
			objectMapper = (ObjectMapper) applicationContext.getBean("objectMapper");
		}
		if (objectMapper == null && applicationContext != null) {
			try {
				objectMapper = (ObjectMapper)BeanFactoryUtils
					.beanOfTypeIncludingAncestors(applicationContext, ObjectMapper.class);
			} catch (Exception e) { /* no-op */ }
		}
		if (objectMapper==null) {
			objectMapper = new ObjectMapper();
		}

		// create JsonRpcHttpClient
		try {
			jsonRpcHttpClient = new JsonRpcHttpClient(objectMapper, new URL(getServiceUrl()), extraHttpHeaders);
			jsonRpcHttpClient.setRequestListener(requestListener);
		} catch (MalformedURLException mue) {
			throw new RuntimeException(mue);
		}
	}

	/**
	 * {@inheritDoc}
	 */
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
			invocation.getMethod(), invocation.getArguments(), useNamedParams);

		// invoke it
		return jsonRpcHttpClient.invoke(
			invocation.getMethod().getName(),
			arguments,
			retType, extraHttpHeaders);
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getObject()
		throws Exception {
		return proxyObject;
	}

	/**
	 * {@inheritDoc}
	 */
	public Class<?> getObjectType() {
		return getServiceInterface();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isSingleton() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
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
	public void setRequestListener(RequestListener requestListener) {
		this.requestListener = requestListener;
	}

	/**
	 * @param useNamedParams the useNamedParams to set
	 */
	public void setUseNamedParams(boolean useNamedParams) {
		this.useNamedParams = useNamedParams;
	}

}
