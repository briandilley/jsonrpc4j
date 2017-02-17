package com.googlecode.jsonrpc4j;

import com.googlecode.jsonrpc4j.spring.rest.JsonRpcRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for create client proxies.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class ProxyUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(ProxyUtil.class);
	
	/**
	 * Creates a composite service using all of the given
	 * services.
	 *
	 * @param classLoader              the {@link ClassLoader}
	 * @param services                 the service objects
	 * @param allowMultipleInheritance whether or not to allow multiple inheritance
	 * @return the object
	 */
	public static Object createCompositeServiceProxy(ClassLoader classLoader, Object[] services, boolean allowMultipleInheritance) {
		return createCompositeServiceProxy(classLoader, services, null, allowMultipleInheritance);
	}
	
	/**
	 * Creates a composite service using all of the given
	 * services and implementing the given interfaces.
	 *
	 * @param classLoader              the {@link ClassLoader}
	 * @param services                 the service objects
	 * @param serviceInterfaces        the service interfaces
	 * @param allowMultipleInheritance whether or not to allow multiple inheritance
	 * @return the object
	 */
	public static Object createCompositeServiceProxy(ClassLoader classLoader, Object[] services, Class<?>[] serviceInterfaces, boolean allowMultipleInheritance) {
		
		Set<Class<?>> interfaces = collectInterfaces(services, serviceInterfaces);
		final Map<Class<?>, Object> serviceClassToInstanceMapping = buildServiceMap(services, allowMultipleInheritance, interfaces);
		// now create the proxy
		return Proxy.newProxyInstance(classLoader, interfaces.toArray(new Class<?>[0]), new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				Class<?> clazz = method.getDeclaringClass();
				if (clazz == Object.class) {
					return proxyObjectMethods(method, proxy, args);
				}
				return method.invoke(serviceClassToInstanceMapping.get(clazz), args);
			}
		});
	}
	
	private static Set<Class<?>> collectInterfaces(Object[] services, Class<?>[] serviceInterfaces) {
		Set<Class<?>> interfaces = new HashSet<>();
		if (serviceInterfaces != null) {
			interfaces.addAll(Arrays.asList(serviceInterfaces));
		} else {
			for (Object o : services) {
				interfaces.addAll(Arrays.asList(o.getClass().getInterfaces()));
			}
		}
		return interfaces;
	}
	
	private static Map<Class<?>, Object> buildServiceMap(Object[] services, boolean allowMultipleInheritance, Set<Class<?>> interfaces) {
		final Map<Class<?>, Object> serviceMap = new HashMap<>();
		for (Class<?> clazz : interfaces) {
			if (serviceMap.containsKey(clazz) && allowMultipleInheritance) {
				continue;
			} else if (serviceMap.containsKey(clazz)) {
				throw new IllegalArgumentException("Multiple inheritance not allowed " + clazz.getName());
			}
			for (Object o : services) {
				if (clazz.isInstance(o)) {
					logger.debug("Using {} for {}", o.getClass().getName(), clazz.getName());
					serviceMap.put(clazz, o);
					break;
				}
			}
			if (!serviceMap.containsKey(clazz)) {
				throw new IllegalArgumentException("None of the provided services implement " + clazz.getName());
			}
		}
		return serviceMap;
	}
	
	private static Object proxyObjectMethods(Method method, Object proxyObject, Object[] args) {
		String name = method.getName();
		if (name.equals("toString")) {
			return proxyObject.getClass().getName() + "@" + System.identityHashCode(proxyObject);
		}
		if (name.equals("hashCode")) {
			return System.identityHashCode(proxyObject);
		}
		if (name.equals("equals")) {
			return proxyObject == args[0];
		}
		throw new RuntimeException(method.getName() + " is not a member of java.lang.Object");
	}
	
	/**
	 * Creates a {@link Proxy} of the given {@code proxyInterface}
	 * that uses the given {@link JsonRpcClient}.
	 *
	 * @param <T>            the proxy type
	 * @param classLoader    the {@link ClassLoader}
	 * @param proxyInterface the interface to proxy
	 * @param client         the {@link JsonRpcClient}
	 * @param socket         the {@link Socket}
	 * @return the proxied interface
	 * @throws IOException if an I/O error occurs when creating the input stream,  the output stream, the socket
	 *                     is closed, the socket is not connected,  or the socket input has been shutdown using shutdownInput()
	 */
	@SuppressWarnings("WeakerAccess")
	public static <T> T createClientProxy(ClassLoader classLoader, Class<T> proxyInterface, final JsonRpcClient client, Socket socket) throws IOException {
		return createClientProxy(classLoader, proxyInterface, client, socket.getInputStream(), socket.getOutputStream());
	}
	
	/**
	 * Creates a {@link Proxy} of the given {@code proxyInterface}
	 * that uses the given {@link JsonRpcClient}.
	 *
	 * @param <T>            the proxy type
	 * @param classLoader    the {@link ClassLoader}
	 * @param proxyInterface the interface to proxy
	 * @param client         the {@link JsonRpcClient}
	 * @param input          the {@link InputStream}
	 * @param output         the {@link OutputStream}
	 * @return the proxied interface
	 */
	@SuppressWarnings({"unchecked", "WeakerAccess"})
	public static <T> T createClientProxy(ClassLoader classLoader, Class<T> proxyInterface, final JsonRpcClient client, final InputStream input, final OutputStream output) {
		
		// create and return the proxy
		return (T) Proxy.newProxyInstance(classLoader, new Class<?>[]{proxyInterface}, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (isDeclaringClassAnObject(method)) return proxyObjectMethods(method, proxy, args);
				
				final Object arguments = ReflectionUtil.parseArguments(method, args);
				final String methodName = getMethodName(method);
				return client.invokeAndReadResponse(methodName, arguments, method.getGenericReturnType(), output, input);
			}
		});
	}
	
	private static boolean isDeclaringClassAnObject(Method method) {
		return method.getDeclaringClass() == Object.class;
	}
	
	private static String getMethodName(Method method) {
		final JsonRpcMethod jsonRpcMethod = ReflectionUtil.getAnnotation(method, JsonRpcMethod.class);
		if (jsonRpcMethod == null) {
			return method.getName();
		} else {
			return jsonRpcMethod.value();
		}
	}
	
	public static <T> T createClientProxy(Class<T> clazz, JsonRpcRestClient client) {
		return createClientProxy(clazz.getClassLoader(), clazz, client);
	}
	
	/**
	 * Creates a {@link Proxy} of the given {@code proxyInterface} that uses the given {@link JsonRpcHttpClient}.
	 *
	 * @param <T>            the proxy type
	 * @param classLoader    the {@link ClassLoader}
	 * @param proxyInterface the interface to proxy
	 * @param client         the {@link JsonRpcHttpClient}
	 * @return the proxied interface
	 */
	public static <T> T createClientProxy(ClassLoader classLoader, Class<T> proxyInterface, final IJsonRpcClient client) {
		return createClientProxy(classLoader, proxyInterface, client, new HashMap<String, String>());
	}
	
	/**
	 * Creates a {@link Proxy} of the given {@code proxyInterface}
	 * that uses the given {@link IJsonRpcClient}.
	 *
	 * @param <T>            the proxy type
	 * @param classLoader    the {@link ClassLoader}
	 * @param proxyInterface the interface to proxy
	 * @param client         the {@link JsonRpcHttpClient}
	 * @param extraHeaders   extra HTTP headers to be added to each response
	 * @return the proxied interface
	 */
	@SuppressWarnings("unchecked")
	private static <T> T createClientProxy(ClassLoader classLoader, Class<T> proxyInterface, final IJsonRpcClient client, final Map<String, String> extraHeaders) {
		
		return (T) Proxy.newProxyInstance(classLoader, new Class<?>[]{proxyInterface}, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (isDeclaringClassAnObject(method)) return proxyObjectMethods(method, proxy, args);
				
				final Object arguments = ReflectionUtil.parseArguments(method, args);
				final String methodName = getMethodName(method);
				return client.invoke(methodName, arguments, method.getGenericReturnType(), extraHeaders);
			}
		});
	}
	
}
