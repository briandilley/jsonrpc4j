package com.googlecode.jsonrpc4j.spring;

import com.googlecode.jsonrpc4j.JsonRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;


public class AutoJsonRpcClientProxyFactory implements ApplicationContextAware, InstantiationAwareBeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AutoJsonRpcClientProxyFactory.class);

    private ApplicationContext applicationContext;

    private URL baseUrl;

    @Override
    public boolean postProcessAfterInstantiation(final Object bean, final String beanName) throws BeansException {
        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            if (field.isAnnotationPresent(JsonRpcReference.class)) {
                // valid
                final Class serviceInterface = field.getType();
                if (!serviceInterface.isInterface()) {
                    throw new RuntimeException("JsonRpcReference must be interface.");
                }

                JsonRpcReference rpcReference = field.getAnnotation(JsonRpcReference.class);
                String fullUrl = fullUrl(rpcReference.address(), serviceInterface);
                JsonProxyFactoryBean factoryBean = createJsonBean(serviceInterface, fullUrl);
                factoryBean.afterPropertiesSet();
                // get proxy
                Object proxy = null;
                try {
                    proxy = factoryBean.getObject();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                // set bean
                field.setAccessible(true);
                field.set(bean, proxy);
                logger.debug("------------- set bean {} field {} to proxy {}", beanName, field.getName(), proxy.getClass().getName());
            }
        });
        return true;
    }

    private String fullUrl(String url, Class serviceInterface) {
        try {
            baseUrl = new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        SimpleMetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory(applicationContext);
        MetadataReader metadataReader = null;
        try {
            metadataReader = metadataReaderFactory.getMetadataReader(serviceInterface.getName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClassMetadata classMetadata = metadataReader.getClassMetadata();
        AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
        String jsonRpcPathAnnotation = JsonRpcService.class.getName();
        if (annotationMetadata.isAnnotated(jsonRpcPathAnnotation)) {
            String className = classMetadata.getClassName();
            String path = (String) annotationMetadata.getAnnotationAttributes(jsonRpcPathAnnotation).get("value");
            logger.debug("Found JSON-RPC service to proxy [{}] on path '{}'.", className, path);
            return appendBasePath(path);
        } else {
            throw new RuntimeException("JsonRpcService must be interface.");
        }

    }

    private String appendBasePath(String path) {
        try {
            return new URL(baseUrl, path).toString();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(String.format("Cannot combine URLs '%s' and '%s' to valid URL.", baseUrl, path), e);
        }
    }


    private JsonProxyFactoryBean createJsonBean(Class serviceInterface, String serviceUrl) {
        JsonProxyFactoryBean bean = new JsonProxyFactoryBean();
        bean.setServiceInterface(serviceInterface);
        bean.setServiceUrl(serviceUrl);
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}

