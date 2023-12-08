package com.googlecode.jsonrpc4j.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static org.springframework.util.ClassUtils.forName;
import static org.springframework.util.ClassUtils.getAllInterfacesForClass;

/**
 * <p>
 * This class can be instantiated in a spring context in order to simplify the configuration of JSON-RPC
 * services afforded by beans in the same context.  The services to be configured are identified
 * by the annotation {@link AutoJsonRpcServiceImpl} on the implementation of the service.  Such
 * implementation beans must also have the {@link JsonRpcService} annotation associated with them; either
 * on the implementation class itself or, preferably, on an interface that the implementation implements.
 * </p>
 * <p>The path for exposing the service is obtained from {@link JsonRpcService#value()}, but it is also
 * possible to define additional paths on {@link AutoJsonRpcServiceImpl#additionalPaths()}.</p>
 * <p>Below is an example of spring context XML snippet that illustrates typical usage;</p>
 * <pre>
 * &lt;bean class=&quot;com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImplExporter&quot;/&gt;
 * &lt;bean class="MyServiceBean"/&gt;
 * </pre>
 */
@SuppressWarnings("unused")
public class AutoJsonRpcServiceImplExporter implements BeanFactoryPostProcessor {

	private static final Logger logger = LoggerFactory.getLogger(AutoJsonRpcServiceImplExporter.class);

	private static final String PATH_PREFIX = "/";

	private static final Pattern PATTERN_JSONRPC_PATH = Pattern.compile("/|(^/?[A-Za-z0-9._~-]+(/[A-Za-z0-9._~-]+)*$)");

	private ObjectMapper objectMapper;
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
	private List<JsonRpcInterceptor> interceptorList = null;
    private ExecutorService batchExecutorService = null;
    private long parallelBatchProcessingTimeout;
	
	/**
	 * Finds the beans to expose.
	 * <p>
	 * Searches parent factories as well.
	 */
	private static Map<String, String> findServiceBeanDefinitions(ConfigurableListableBeanFactory beanFactory) {
		final Map<String, String> serviceBeanNames = new HashMap<>();
		
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			AutoJsonRpcServiceImpl autoJsonRpcServiceImplAnnotation = beanFactory.findAnnotationOnBean(beanName, AutoJsonRpcServiceImpl.class);
			JsonRpcService jsonRpcServiceAnnotation = beanFactory.findAnnotationOnBean(beanName, JsonRpcService.class);
			
			if (null != autoJsonRpcServiceImplAnnotation) {
				
				if (null == jsonRpcServiceAnnotation) {
					throw new IllegalStateException("on the bean [" + beanName + "], @" +
							AutoJsonRpcServiceImpl.class.getSimpleName() + " was found, but not @" +
							JsonRpcService.class.getSimpleName() + " -- both are required");
				}
				
				List<String> paths = new ArrayList<>();
				Collections.addAll(paths, autoJsonRpcServiceImplAnnotation.additionalPaths());
				paths.add(jsonRpcServiceAnnotation.value());
				
				for (String path : paths) {
					if (!PATTERN_JSONRPC_PATH.matcher(path).matches()) {
						throw new RuntimeException("the path [" + path + "] for the bean [" + beanName + "] is not valid");
					}
					
					logger.info("exporting bean [{}] ---> [{}]", beanName, path);
					if (isNotDuplicateService(serviceBeanNames, beanName, path)) {
                        serviceBeanNames.put(path, beanName);
                    }
				}
				
			}
		}
		
		collectFromParentBeans(beanFactory, serviceBeanNames);
		return serviceBeanNames;
	}
	
	@SuppressWarnings("Convert2streamapi")
	private static void collectFromParentBeans(ConfigurableListableBeanFactory beanFactory, Map<String, String> serviceBeanNames) {
		BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();
		if (parentBeanFactory != null && ConfigurableListableBeanFactory.class.isInstance(parentBeanFactory)) {
			for (Entry<String, String> entry : findServiceBeanDefinitions((ConfigurableListableBeanFactory) parentBeanFactory).entrySet()) {
				if (isNotDuplicateService(serviceBeanNames, entry.getKey(), entry.getValue())) {
                    serviceBeanNames.put(entry.getKey(), entry.getValue());
                }
			}
		}
	}
	
	private static boolean isNotDuplicateService(Map<String, String> serviceBeanNames, String beanName, String pathValue) {
		if (serviceBeanNames.containsKey(pathValue)) {
			String otherBeanName = serviceBeanNames.get(pathValue);
			logger.debug("Duplicate JSON-RPC path specification: found {} on both [{}] and [{}].", pathValue, beanName, otherBeanName);
			return false;
		}
		return true;
	}
	
	private static boolean hasServiceAnnotation(JsonRpcService jsonRpcPath) {
		return jsonRpcPath != null;
	}
	
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;
		Map<String, String> servicePathToBeanName = findServiceBeanDefinitions(defaultListableBeanFactory);
		for (Entry<String, String> entry : servicePathToBeanName.entrySet()) {
			registerServiceProxy(defaultListableBeanFactory, makeUrlPath(entry.getKey()), entry.getValue());
		}
	}
	
	/**
	 * To make the
	 * {@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping}
	 * export a bean automatically, the name should start with a '/'.
	 */
	private String makeUrlPath(String servicePath) {
		if (null == servicePath || 0 == servicePath.length()) {
			throw new IllegalArgumentException("the service path must be provided");
		}
		
		if ('/' == servicePath.charAt(0)) {
			return servicePath;
		}
		
		return PATH_PREFIX.concat(servicePath);
	}
	
	/**
	 * Registers the new beans with the bean factory.
	 */
	private void registerServiceProxy(DefaultListableBeanFactory defaultListableBeanFactory, String servicePath, String serviceBeanName) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(JsonServiceExporter.class).addPropertyReference("service", serviceBeanName);
		BeanDefinition serviceBeanDefinition = findBeanDefinition(defaultListableBeanFactory, serviceBeanName);
		for (Class<?> currentInterface : getBeanInterfaces(serviceBeanDefinition, defaultListableBeanFactory.getBeanClassLoader())) {
			if (currentInterface.isAnnotationPresent(JsonRpcService.class)) {
				String serviceInterface = currentInterface.getName();
				logger.debug("Registering interface '{}' for JSON-RPC bean [{}].", serviceInterface, serviceBeanName);
				builder.addPropertyValue("serviceInterface", serviceInterface);
				break;
			}
		}
		if (objectMapper != null) {
			builder.addPropertyValue("objectMapper", objectMapper);
		}
		
		if (errorResolver != null) {
			builder.addPropertyValue("errorResolver", errorResolver);
		}
		
		if (invocationListener != null) {
			builder.addPropertyValue("invocationListener", invocationListener);
		}

		if (httpStatusCodeProvider != null) {
			builder.addPropertyValue("httpStatusCodeProvider", httpStatusCodeProvider);
		}
		
		if (convertedParameterTransformer != null) {
			builder.addPropertyValue("convertedParameterTransformer", convertedParameterTransformer);
		}
		
		if (contentType != null) {
			builder.addPropertyValue("contentType", contentType);
		}

		if (interceptorList != null) {
			builder.addPropertyValue("interceptorList", interceptorList);
		}

		if (batchExecutorService != null) {
		    builder.addPropertyValue("batchExecutorService", batchExecutorService);
        }

		builder.addPropertyValue("backwardsCompatible", backwardsCompatible);
		builder.addPropertyValue("rethrowExceptions", rethrowExceptions);
		builder.addPropertyValue("allowExtraParams", allowExtraParams);
		builder.addPropertyValue("allowLessParams", allowLessParams);
		builder.addPropertyValue("shouldLogInvocationErrors", shouldLogInvocationErrors);
		
		defaultListableBeanFactory.registerBeanDefinition(servicePath, builder.getBeanDefinition());
	}
	
	/**
	 * Find a {@link BeanDefinition} in the {@link BeanFactory} or it's parents.
	 */
	private BeanDefinition findBeanDefinition(ConfigurableListableBeanFactory beanFactory, String serviceBeanName) {
		if (beanFactory.containsLocalBean(serviceBeanName)) {
			return beanFactory.getBeanDefinition(serviceBeanName);
		}
		BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();
		if (parentBeanFactory != null && ConfigurableListableBeanFactory.class.isInstance(parentBeanFactory)) {
			return findBeanDefinition((ConfigurableListableBeanFactory) parentBeanFactory, serviceBeanName);
		}
		throw new NoSuchBeanDefinitionException(serviceBeanName);
	}
	
	private Class<?>[] getBeanInterfaces(BeanDefinition serviceBeanDefinition, ClassLoader beanClassLoader) {
		String beanClassName = serviceBeanDefinition.getBeanClassName();
		try {
			Class<?> beanClass = forName(beanClassName, beanClassLoader);
			return getAllInterfacesForClass(beanClass, beanClassLoader);
		} catch (ClassNotFoundException | LinkageError e) {
			throw new IllegalStateException(format("Cannot find bean class '%s'.", beanClassName), e);
		}
	}
	
	/**
	 * @param objectMapper the objectMapper to set
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
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
		if (interceptorList == null || interceptorList.isEmpty()) {
			throw new IllegalArgumentException("Interceptor list must be not null and not empty");
		}
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
}
