package com.googlecode.jsonrpc4j.spring;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.remoting.support.RemoteExporter;

import com.googlecode.jsonrpc4j.ConvertedParameterTransformer;
import com.googlecode.jsonrpc4j.ErrorResolver;
import com.googlecode.jsonrpc4j.HttpStatusCodeProvider;
import com.googlecode.jsonrpc4j.InvocationListener;
import com.googlecode.jsonrpc4j.JsonRpcServer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link RemoteExporter} that exports services using Json
 * according to the JSON-RPC proposal specified at:
 * <a href="http://groups.google.com/group/json-rpc">
 * http://groups.google.com/group/json-rpc</a>.
 *
 */
@SuppressWarnings("unused")
abstract class AbstractJsonServiceExporter extends RemoteExporter implements InitializingBean, ApplicationContextAware {

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
				logger.debug(e);
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

		if (contentType != null) {
			jsonRpcServer.setContentType(contentType);
		}

		exportService();
	}

	/**
	 * Called when the service is ready to be exported.
	 * @throws Exception on error
	 */
	void exportService()
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
	 *
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

}
