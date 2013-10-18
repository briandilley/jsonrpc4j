package com.googlecode.jsonrpc4j;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A multiple service dispatcher that supports JSON-RPC "method" names
 * that use dot-notation to select a server endpoint.  For example:
 * <pre>
 * {
 *    "jsonrpc": "2.0",
 *    "method": "service.method",
 *    "params": {"foo", "bar"},
 *    "id": 1
 * }
 * </pre>
 * An example of using this class is:
 * <code>
 *    JsonRpcMultiServer rpcServer = new JsonRpcMultiServer();
 *    rpcServer.addService("Foo", new FooService())
 *             .addService("Bar", new BarService());
 * </code>
 * A client can then call a <i>test(String, String)</i> method on the Foo service
 * like this:
 * <pre>
 * {
 *    "jsonrpc": "2.0",
 *    "method": "Foo.test",
 *    "params": ["some", "thing"],
 *    "id": 1
 * }
 * </pre>
 */
public class JsonRpcMultiServer extends JsonRpcServer {

	private static final Logger LOGGER = Logger.getLogger(JsonRpcMultiServer.class.getName());

	private Map<String, Object> handlerMap;
	private Map<String, Class<?>> interfaceMap;

	public JsonRpcMultiServer() {
		this(new ObjectMapper());
	}

	public JsonRpcMultiServer(ObjectMapper mapper) {
		super(mapper, (Object) null);
		this.handlerMap = new HashMap<String, Object>();
		this.interfaceMap = new HashMap<String, Class<?>>();
	}

	public JsonRpcMultiServer addService(String name, Object handler) {
		return addService(name, handler, null);
	}

	public JsonRpcMultiServer addService(String name, Object handler, Class<?> remoteInterface) {
		handlerMap.put(name, handler);
		if (remoteInterface != null) {
			interfaceMap.put(name, remoteInterface);
		}
		return this;
	}

	/**
	 * Returns the handler's class or interfaces.  The serviceName is used
	 * to look up a registered handler.
	 *
	 * @param serviceName the optional name of a service
	 * @return the class
	 */
	@Override
	protected Class<?>[] getHandlerInterfaces(String serviceName) {
		Class<?> remoteInterface = interfaceMap.get(serviceName);
		if (remoteInterface != null) {
			return new Class<?>[] {remoteInterface};
		} else if (Proxy.isProxyClass(getHandler(serviceName).getClass())) {
			return getHandler(serviceName).getClass().getInterfaces();
		} else {
			return new Class<?>[] {getHandler(serviceName).getClass()};
		}
	}

	/**
	 * Get the handler (object) that should be invoked to execute the specified
	 * RPC method based on the specified service name.
	 *
	 * @param serviceName the service name
	 * @return the handler to invoke the RPC call against
	 */
	@Override
	protected Object getHandler(String serviceName) {
		Object handler = handlerMap.get(serviceName);
		if (handler == null) {
			LOGGER.log(Level.SEVERE, "Service '" + serviceName + "' is not registered in this multi-server");
			throw new RuntimeException("Service '" + serviceName + "' does not exist");
		}
		return handler;
	}

	/**
	 * Get the service name from the methodNode.  JSON-RPC methods with the form
	 * Service.method will result in "Service" being returned in this case.
	 *
	 * @param methodNode the JsonNode for the method
	 * @return the name of the service, or <code>null</code>
	 */
	@Override
	protected String getServiceName(JsonNode methodNode) {
		String methodName = (methodNode!=null && !methodNode.isNull()) ? methodNode.asText() : null;
		if (methodName != null) {
			int ndx = methodName.indexOf('.');
			if (ndx > 0) {
				return methodName.substring(0, ndx);
			}
		}
		return methodName;
	}

	/**
	 * Get the method name from the methodNode, stripping off the service name.
	 *
	 * @param methodNode the JsonNode for the method
	 * @return the name of the method that should be invoked
	 */
	@Override
	protected String getMethodName(JsonNode methodNode) {
		String methodName = (methodNode!=null && !methodNode.isNull()) ? methodNode.asText() : null;
		if (methodName != null) {
			int ndx = methodName.indexOf('.');
			if (ndx > 0) {
				return methodName.substring(ndx + 1);
			}
		}
		return methodName;
	}
}