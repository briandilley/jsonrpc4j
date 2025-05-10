package com.googlecode.jsonrpc4j.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Exports user defined services using JSON-RPC protocol
 */
@SuppressWarnings("unused")
abstract class AbstractJsonServiceExporter implements InitializingBean, ApplicationContextAware {
	private static final Logger logger = LoggerFactory.getLogger(AbstractJsonServiceExporter.class);

	private ObjectMapper objectMapper;
	private JsonRpcServer jsonRpcServer;
	private ApplicationContext applicationContext;
	private ErrorResolver errorResolver = null;
	private boolean backwardsCompatible = true;
	private boolean rethrowExceptions = false;
	private boolean allowExtraParams = false;
	private boolean allowLessParams = false;
	private boolean shouldLogInvocationErrors = true;
	private InvocationListener invocationListener = null;
	private HttpStatusCodeProvider httpStatusCodeProvider = null;
	private ConvertedParameterTransformer convertedParameterTransformer = null;
	private String contentType = null;
	private List<JsonRpcInterceptor> interceptorList;
	private ExecutorService batchExecutorService = null;
	private long parallelBatchProcessingTimeout;
	private Object service;
	private Class<?> serviceInterface;

	/**
	 * {@inheritDoc}
	 */
	public void afterPropertiesSet() throws Exception {

		if (objectMapper == null && applicationContext != null && applicationContext.containsBean("objectMapper")) {
			objectMapper = (ObjectMapper) applicationContext.getBean("objectMapper");
		}
		if (objectMapper == null && applicationContext != null) {
			try {
				objectMapper = BeanFactoryUtils.beanOfTypeIncludingAncestors(applicationContext, ObjectMapper.class);
			} catch (Exception e) {
				logger.debug("Failed to obtain objectMapper from application context", e);
			}
		}
		if (objectMapper == null) {
			objectMapper = new ObjectMapper();
		}

		// Create the server. The 'handler' parameter here is either a proxy or the real instance depending on
		// the presence or absence of the interface. This is because it is not possible to create a proxy unless
		// an interface is specified.

		jsonRpcServer = new JsonRpcServer(
				objectMapper,
				null == getServiceInterface() ? getService() : getProxyForService(),
				getServiceInterface());
		jsonRpcServer.setErrorResolver(errorResolver);
		jsonRpcServer.setBackwardsCompatible(backwardsCompatible);
		jsonRpcServer.setRethrowExceptions(rethrowExceptions);
		jsonRpcServer.setAllowExtraParams(allowExtraParams);
		jsonRpcServer.setAllowLessParams(allowLessParams);
		jsonRpcServer.setInvocationListener(invocationListener);
		jsonRpcServer.setHttpStatusCodeProvider(httpStatusCodeProvider);
		jsonRpcServer.setConvertedParameterTransformer(convertedParameterTransformer);
		jsonRpcServer.setShouldLogInvocationErrors(shouldLogInvocationErrors);
		jsonRpcServer.setBatchExecutorService(batchExecutorService);
		jsonRpcServer.setParallelBatchProcessingTimeout(parallelBatchProcessingTimeout);

		if (contentType != null) {
			jsonRpcServer.setContentType(contentType);
		}
		if (interceptorList != null) {
			jsonRpcServer.setInterceptorList(interceptorList);
		}

		ReflectionUtil.clearCache();

		exportService();
	}

	/**
	 * Called when the service is ready to be exported.
	 *
	 * @throws Exception on error
	 */
	protected void exportService()
			throws Exception {
		// no-op
	}

	/**
	 * @return the objectMapper
	 */
	protected ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	/**
	 * @param objectMapper the objectMapper to set
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * @return the jsonRpcServer
	 */
	JsonRpcServer getJsonRpcServer() {
		return jsonRpcServer;
	}

	/**
	 * @return the applicationContext
	 */
	protected ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * @param errorResolver the errorResolver to set
	 */
	public void setErrorResolver(ErrorResolver errorResolver) {
		this.errorResolver = errorResolver;
	}

	/**
	 * @param backwardsCompatible the backwardsCompatible to set
	 */
	public void setBackwardsCompatible(boolean backwardsCompatible) {
		this.backwardsCompatible = backwardsCompatible;
	}

	/**
	 * @param rethrowExceptions the rethrowExceptions to set
	 */
	public void setRethrowExceptions(boolean rethrowExceptions) {
		this.rethrowExceptions = rethrowExceptions;
	}

	/**
	 * @param allowExtraParams the allowExtraParams to set
	 */
	public void setAllowExtraParams(boolean allowExtraParams) {
		this.allowExtraParams = allowExtraParams;
	}

	/**
	 * @param allowLessParams the allowLessParams to set
	 */
	public void setAllowLessParams(boolean allowLessParams) {
		this.allowLessParams = allowLessParams;
	}

	/**
	 * @param invocationListener the invocationListener to set
	 */
	public void setInvocationListener(InvocationListener invocationListener) {
		this.invocationListener = invocationListener;
	}

	/**
	 * @param httpStatusCodeProvider the HttpStatusCodeProvider to set
	 */
	public void setHttpStatusCodeProvider(HttpStatusCodeProvider httpStatusCodeProvider) {
		this.httpStatusCodeProvider = httpStatusCodeProvider;
	}

	/**
	 * @param convertedParameterTransformer the convertedParameterTransformer to set
	 */
	public void setConvertedParameterTransformer(ConvertedParameterTransformer convertedParameterTransformer) {
		this.convertedParameterTransformer = convertedParameterTransformer;
	}

	public void setShouldLogInvocationErrors(boolean shouldLogInvocationErrors) {
		this.shouldLogInvocationErrors = shouldLogInvocationErrors;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void setInterceptorList(List<JsonRpcInterceptor> interceptorList) {
		this.interceptorList = interceptorList;
	}

    /**
     * @param batchExecutorService the {@link ExecutorService} to set
     */
    public void setBatchExecutorService(ExecutorService batchExecutorService) {
        this.batchExecutorService = batchExecutorService;
    }

    /**
     * @param parallelBatchProcessingTimeout timeout used for parallel batch processing
     */
    public void setParallelBatchProcessingTimeout(long parallelBatchProcessingTimeout) {
        this.parallelBatchProcessingTimeout = parallelBatchProcessingTimeout;
    }

	/**
	 * Set the service to export.
	 * Typically populated via a bean reference.
	 */
	public void setService(Object service) {
		this.service = service;
	}

	/**
	 * Return the service to export.
	 */
	public Object getService() {
		return this.service;
	}

	/**
	 * Set the interface of the service to export.
	 * The interface must be suitable for the particular service and remoting strategy.
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		if (serviceInterface == null) {
			throw new IllegalArgumentException("'serviceInterface' must not be null");
		}
		if (!serviceInterface.isInterface()) {
			throw new IllegalArgumentException("'serviceInterface' must be an interface");
		}
		this.serviceInterface = serviceInterface;
	}

	/**
	 * Return the interface of the service to export.
	 */
	public Class<?> getServiceInterface() {
		return this.serviceInterface;
	}


	/**
	 * Check whether a service reference has been set,
	 * and whether it matches the specified service.
	 * @see #setServiceInterface
	 * @see #setService
	 */
	protected void checkServiceInterface() throws IllegalArgumentException {
		Class<?> serviceInterface = getServiceInterface();
		if (serviceInterface == null) {
			throw new IllegalArgumentException("Property 'serviceInterface' is required");
		}

		Object service = getService();
		if (service instanceof String) {
			throw new IllegalArgumentException(
				"Service [" + service + "] is a String rather than an actual service reference:"
					+ " Have you accidentally specified the service bean name as value "
					+ " instead of as reference?"
			);
		}
		if (!serviceInterface.isInstance(service)) {
			throw new IllegalArgumentException(
				"Service interface [" + serviceInterface.getName()
				+ "] needs to be implemented by service [" + service + "] of class ["
				+ service.getClass().getName() + "]"
			);
		}
	}


	/**
	 * Get a proxy for the given service object, implementing the specified
	 * service interface.
	 * <p>Used to export a proxy that does not expose any internals but just
	 * a specific interface intended for remote access.
	 *
	 * @return the proxy
	 * @see #setServiceInterface
	 * @see #setService
	 */
	protected Object getProxyForService() {
		Object targetService = getService();
		if (targetService == null) {
			throw new IllegalArgumentException("Property 'service' is required");
		}
		checkServiceInterface();

		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.addInterface(getServiceInterface());
		proxyFactory.setTarget(targetService);
		proxyFactory.setOpaque(true);

		return proxyFactory.getProxy(ClassUtils.getDefaultClassLoader());
	}
}
