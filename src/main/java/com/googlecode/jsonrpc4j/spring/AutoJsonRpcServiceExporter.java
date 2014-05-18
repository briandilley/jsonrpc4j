/*
The MIT License (MIT)

Copyright (c) 2014 jsonrpc4j

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package com.googlecode.jsonrpc4j.spring;

import static java.lang.String.format;
import static org.springframework.util.ClassUtils.forName;
import static org.springframework.util.ClassUtils.getAllInterfacesForClass;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.ErrorResolver;
import com.googlecode.jsonrpc4j.JsonRpcService;

/**
 * Auto exports {@link JsonRpcService} annotated beans as JSON-RPC services.
 * <p>
 * Minmizes the configuration necessary to export beans as JSON-RPC services to:
 * 
 * <pre>
 * &lt;bean class=&quot;com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceExporter&quot;/&gt;
 * 
 * &ltbean class="MyServiceBean"/&gt;
 * </pre>
 */
public class AutoJsonRpcServiceExporter
	implements BeanFactoryPostProcessor {

	private static final Logger LOG = Logger.getLogger(AutoJsonRpcServiceExporter.class.getName());

	private static final String PATH_PREFIX = "/";

	private Map<String, String> serviceBeanNames = new HashMap<String, String>();

	private ObjectMapper objectMapper;
	private ErrorResolver errorResolver = null;
	private boolean backwardsComaptible = true;
	private boolean rethrowExceptions = false;
	private boolean allowExtraParams = false;
	private boolean allowLessParams = false;
	private Level exceptionLogLevel = Level.WARNING;

	public void postProcessBeanFactory(
		ConfigurableListableBeanFactory beanFactory)
		throws BeansException {
		DefaultListableBeanFactory dlbf = (DefaultListableBeanFactory) beanFactory;
		findServiceBeanDefinitions(dlbf);
		for (Entry<String, String> entry : serviceBeanNames.entrySet()) {
			String servicePath = entry.getKey();
			String serviceBeanName = entry.getValue();
			registerServiceProxy(dlbf, makeUrlPath(servicePath), serviceBeanName);
		}
	}

	/**
	 * Finds the beans to expose and puts them in the {@link #serviceBeanNames}
	 * map.
	 * <p>
	 * Searches parent factories as well.
	 */
	private void findServiceBeanDefinitions(
		ConfigurableListableBeanFactory beanFactory) {
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			JsonRpcService jsonRpcPath = beanFactory.findAnnotationOnBean(beanName, JsonRpcService.class);
			if (jsonRpcPath != null) {
				String pathValue = jsonRpcPath.value();
				LOG.fine(
					format("Found JSON-RPC path '%s' for bean [%s].",
					pathValue, beanName));
				if (serviceBeanNames.containsKey(pathValue)) {
					String otherBeanName = serviceBeanNames.get(pathValue);
					LOG.warning(format(
						"Duplicate JSON-RPC path specification: found %s on both [%s] and [%s].",
						pathValue, beanName, otherBeanName));
				}
				serviceBeanNames.put(pathValue, beanName);
			}
		}
		BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();
		if (parentBeanFactory != null 
			&& ConfigurableListableBeanFactory.class.isInstance(parentBeanFactory)) {
			findServiceBeanDefinitions((ConfigurableListableBeanFactory) parentBeanFactory);
		}
	}

	/**
	 * To make the
	 * {@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping}
	 * export a bean automatically, the name should start with a '/'.
	 */
	private String makeUrlPath(String servicePath) {
		return PATH_PREFIX.concat(servicePath);
	}

	/**
	 * Registers the new beans with the bean factory.
	 */
	private void registerServiceProxy(
		DefaultListableBeanFactory dlbf, String servicePath, String serviceBeanName) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
			.rootBeanDefinition(JsonServiceExporter.class)
			.addPropertyReference("service", serviceBeanName);
		BeanDefinition serviceBeanDefinition = findBeanDefintion(dlbf, serviceBeanName);
		for (Class<?> iface :
			getBeanInterfaces(serviceBeanDefinition, dlbf.getBeanClassLoader())) {
			if (iface.isAnnotationPresent(JsonRpcService.class)) {
				String serviceInterface = iface.getName();
				LOG.fine(format(
					"Registering interface '%s' for JSON-RPC bean [%s].",
					serviceInterface, serviceBeanName));
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
		builder.addPropertyValue("backwardsComaptible", Boolean.valueOf(backwardsComaptible));
		builder.addPropertyValue("rethrowExceptions", Boolean.valueOf(rethrowExceptions));
		builder.addPropertyValue("allowExtraParams", Boolean.valueOf(allowExtraParams));
		builder.addPropertyValue("allowLessParams", Boolean.valueOf(allowLessParams));
		builder.addPropertyValue("exceptionLogLevel", exceptionLogLevel);
		dlbf.registerBeanDefinition(servicePath, builder.getBeanDefinition());
	}

	/**
	 * Find a {@link BeanDefinition} in the {@link BeanFactory} or it's parents.
	 */
	private BeanDefinition findBeanDefintion(
		ConfigurableListableBeanFactory beanFactory, String serviceBeanName) {
		if (beanFactory.containsLocalBean(serviceBeanName)) {
			return beanFactory.getBeanDefinition(serviceBeanName);
		}
		BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();
		if (parentBeanFactory != null
			&& ConfigurableListableBeanFactory.class.isInstance(parentBeanFactory)) {
			return findBeanDefintion(
				(ConfigurableListableBeanFactory) parentBeanFactory,
				serviceBeanName);
		}
		throw new RuntimeException(format(
				"Bean with name '%s' can no longer be found.", serviceBeanName));
	}

	private Class<?>[] getBeanInterfaces(
		BeanDefinition serviceBeanDefinition, ClassLoader beanClassLoader) {
		String beanClassName = serviceBeanDefinition.getBeanClassName();
		try {
			Class<?> beanClass = forName(beanClassName, beanClassLoader);
			return getAllInterfacesForClass(beanClass, beanClassLoader);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(format("Cannot find bean class '%s'.",
					beanClassName), e);
		} catch (LinkageError e) {
			throw new RuntimeException(format("Cannot find bean class '%s'.",
					beanClassName), e);
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
	 * @param backwardsComaptible the backwardsComaptible to set
	 */
	public void setBackwardsComaptible(boolean backwardsComaptible) {
		this.backwardsComaptible = backwardsComaptible;
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
	 * @param exceptionLogLevel the exceptionLogLevel to set
	 */
	public void setExceptionLogLevel(Level exceptionLogLevel) {
		this.exceptionLogLevel = exceptionLogLevel;
	}

}
