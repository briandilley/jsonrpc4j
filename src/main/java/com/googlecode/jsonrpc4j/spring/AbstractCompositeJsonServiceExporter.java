package com.googlecode.jsonrpc4j.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Abstract class for exposing composite services via spring.
 */
@SuppressWarnings("unused")
abstract class AbstractCompositeJsonServiceExporter implements InitializingBean, ApplicationContextAware {
	private static final Logger logger = LoggerFactory.getLogger(AbstractCompositeJsonServiceExporter.class);

	private ObjectMapper objectMapper;
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
	
	private JsonRpcServer jsonRpcServer;

	private boolean allowMultipleInheritance = false;
	private Class<?>[] serviceInterfaces = null;
	private Object[] services = new Object[0];

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void afterPropertiesSet()
			throws Exception {

		// find the ObjectMapper
		if (objectMapper == null
				&& applicationContext != null
				&& applicationContext.containsBean("objectMapper")) {
			objectMapper = (ObjectMapper) applicationContext.getBean("objectMapper");
		}
		if (objectMapper == null && applicationContext != null) {
			try {
				objectMapper = BeanFactoryUtils.beanOfTypeIncludingAncestors(applicationContext, ObjectMapper.class);
			} catch (Exception e) {
				logger.error("Could not load ObjectMapper from ApplicationContext", e);
			}
		}
		if (objectMapper == null) {
			objectMapper = new ObjectMapper();
		}

		// create the service
		Object service = ProxyUtil.createCompositeServiceProxy(
				getClass().getClassLoader(), services,
				serviceInterfaces, allowMultipleInheritance);

		// create the server
		jsonRpcServer = new JsonRpcServer(objectMapper, service);
		jsonRpcServer.setErrorResolver(errorResolver);
		jsonRpcServer.setBackwardsCompatible(backwardsCompatible);
		jsonRpcServer.setRethrowExceptions(rethrowExceptions);
		jsonRpcServer.setAllowExtraParams(allowExtraParams);
		jsonRpcServer.setAllowLessParams(allowLessParams);

		jsonRpcServer.setInvocationListener(invocationListener);
		jsonRpcServer.setHttpStatusCodeProvider(httpStatusCodeProvider);
		jsonRpcServer.setConvertedParameterTransformer(convertedParameterTransformer);
		jsonRpcServer.setShouldLogInvocationErrors(shouldLogInvocationErrors);

		if (contentType != null) {
			jsonRpcServer.setContentType(contentType);
		}

		ReflectionUtil.clearCache();
		
		// export
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
	 * @return the jsonRpcServer
	 */
	JsonRpcServer getJsonRpcServer() {
		return jsonRpcServer;
	}

	/**
	 * @param objectMapper the objectMapper to set
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * @param applicationContext the applicationContext to set
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
	
	
	/**
	 * @param allowMultipleInheritance the allowMultipleInheritance to set
	 */
	public void setAllowMultipleInheritance(boolean allowMultipleInheritance) {
		this.allowMultipleInheritance = allowMultipleInheritance;
	}

	/**
	 * @param serviceInterfaces the serviceInterfaces to set
	 */
	public void setServiceInterfaces(Class<?>[] serviceInterfaces) {
		this.serviceInterfaces = serviceInterfaces;
	}

	/**
	 * @param services the services to set
	 */
	public void setServices(Object[] services) {
		this.services = services;
	}

}
