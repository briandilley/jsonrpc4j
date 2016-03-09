package com.googlecode.jsonrpc4j.spring.servicesansinterface;

import com.googlecode.jsonrpc4j.JsonRpcService;

/**
 * <p>Unlike the {@link com.googlecode.jsonrpc4j.spring.service.Service} /
 * {@link com.googlecode.jsonrpc4j.spring.service.ServiceImpl} example, this case has no interface
 * so the bean has the @JsonRpcService directly into the implementation.  This setup worked
 * in jsonrpc4j 1.1, but failed in 1.2.</p>
 */

@JsonRpcService("ServiceSansInterface")
public class ServiceSansInterfaceImpl {
}
