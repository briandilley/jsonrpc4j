package com.googlecode.jsonrpc4j;

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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for create client proxies.
 */
public abstract class ProxyUtil {

	private static final Logger LOGGER = Logger.getLogger(ProxyUtil.class.getName());

	/**
	 * Creates a composite service using all of the given
	 * services.
	 * 
	 * @param classLoader the {@link ClassLoader}
	 * @param services the service objects
	 * @param allowMultipleInheritance whether or not to allow multiple inheritance
	 * @return the object
	 */
	public static Object createCompositeServiceProxy(
		ClassLoader classLoader, Object[] services, boolean allowMultipleInheritance) {
		return createCompositeServiceProxy(classLoader, services, null, allowMultipleInheritance);
	}

	/**
	 * Creates a composite service using all of the given
	 * services and implementing the given interfaces.
	 * 
	 * @param classLoader the {@link ClassLoader}
	 * @param services the service objects
	 * @param serviceInterfaces the service interfaces
	 * @param allowMultipleInheritance whether or not to allow multiple inheritance
	 * @return the object
	 */
	public static Object createCompositeServiceProxy(
		ClassLoader classLoader, Object[] services,
		Class<?>[] serviceInterfaces, boolean allowMultipleInheritance) {
		
		// get interfaces
		Set<Class<?>> interfaces = new HashSet<Class<?>>();
		if (serviceInterfaces!=null) {
			interfaces.addAll(Arrays.asList(serviceInterfaces));
		} else {
			for (Object o : services) {
				interfaces.addAll(Arrays.asList(o.getClass().getInterfaces()));
			}
		}

		// build the service map
		final Map<Class<?>, Object> serviceMap = new HashMap<Class<?>, Object>();
		for (Class<?> clazz : interfaces) {

			// we will allow for this, but the first
			// object that was registered wins
			if (serviceMap.containsKey(clazz) && allowMultipleInheritance) {
				continue;
			} else if (serviceMap.containsKey(clazz)) {
				throw new IllegalArgumentException(
					"Multiple inheritance not allowed "+clazz.getName());
			}

			// find a service for this interface
			for (Object o : services) {
				if (clazz.isInstance(o)) {
					if (LOGGER.isLoggable(Level.FINE)) {
						LOGGER.fine("Using "+o.getClass().getName()+" for "+clazz.getName());
					}
					serviceMap.put(clazz, o);
					break;
				}
			}

			// make sure we have one
			if (!serviceMap.containsKey(clazz)) {
				throw new IllegalArgumentException(
					"None of the provided services implement "+clazz.getName());
			}
		}

		// now create the proxy
		return Proxy.newProxyInstance(classLoader, interfaces.toArray(new Class<?>[0]),
			new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
				Class<?> clazz = method.getDeclaringClass();
				if (clazz == Object.class) {
					return proxyObjectMethods(method, proxy, args);
				}
				return method.invoke(serviceMap.get(clazz), args);
			}
		});
	}

	/**
	 * Creates a {@link Proxy} of the given {@link proxyInterface}
	 * that uses the given {@link JsonRpcClient}.
	 * @param <T> the proxy type
	 * @param classLoader the {@link ClassLoader}
	 * @param proxyInterface the interface to proxy
	 * @param client the {@link JsonRpcClient}
	 * @param socket the {@link Socket}
	 * @return the proxied interface
	 */
	public static <T> T createClientProxy(
		ClassLoader classLoader,
		Class<T> proxyInterface,
		final JsonRpcClient client,
		Socket socket) throws IOException {

		// create and return the proxy
		return createClientProxy(
			classLoader, proxyInterface, false, client,
			socket.getInputStream(), socket.getOutputStream());
	}

	/**
	 * Creates a {@link Proxy} of the given {@link proxyInterface}
	 * that uses the given {@link JsonRpcClient}.
	 * @param <T> the proxy type
	 * @param classLoader the {@link ClassLoader}
	 * @param proxyInterface the interface to proxy
	 * @param client the {@link JsonRpcClient}
	 * @param ips the {@link InputStream}
	 * @param ops the {@link OutputStream}
	 * @return the proxied interface
	 */
	@SuppressWarnings("unchecked")
	public static <T> T createClientProxy(
		ClassLoader classLoader,
		Class<T> proxyInterface,
		final boolean useNamedParams,
		final JsonRpcClient client,
		final InputStream ips,
		final OutputStream ops) {

		// create and return the proxy
		return (T)Proxy.newProxyInstance(
			classLoader,
			new Class<?>[] {proxyInterface},
			new InvocationHandler() {
				public Object invoke(Object proxy, Method method, Object[] args)
					throws Throwable {
					if (method.getDeclaringClass() == Object.class) {
						return proxyObjectMethods(method, proxy, args);
					}
					Object arguments = ReflectionUtil.parseArguments(method, args, useNamedParams);
					return client.invokeAndReadResponse(
						method.getName(), arguments, method.getGenericReturnType(), ops, ips);
				}
			});
	}

	/**
	 * Creates a {@link Proxy} of the given {@link proxyInterface}
	 * that uses the given {@link JsonRpcHttpClient}.
	 * @param <T> the proxy type
	 * @param classLoader the {@link ClassLoader}
	 * @param proxyInterface the interface to proxy
	 * @param client the {@link JsonRpcHttpClient}
	 * @param extraHeaders extra HTTP headers to be added to each response
	 * @return the proxied interface
	 */
	@SuppressWarnings("unchecked")
	public static <T> T createClientProxy(
		ClassLoader classLoader,
		Class<T> proxyInterface,
		final boolean useNamedParams,
		final JsonRpcHttpClient client,
		final Map<String, String> extraHeaders) {

		// create and return the proxy
		return (T)Proxy.newProxyInstance(
			classLoader,
			new Class<?>[] {proxyInterface},
			new InvocationHandler() {
				public Object invoke(Object proxy, Method method, Object[] args)
					throws Throwable {
					if (method.getDeclaringClass() == Object.class) {
						return proxyObjectMethods(method, proxy, args);
					}
					Object arguments = ReflectionUtil.parseArguments(method, args, useNamedParams);
					return client.invoke(
						method.getName(), arguments, method.getGenericReturnType(), extraHeaders);
				}
			});
	}

	/**
	 * Creates a {@link Proxy} of the given {@link proxyInterface}
	 * that uses the given {@link JsonRpcHttpClient}.
	 * @param <T> the proxy type
	 * @param classLoader the {@link ClassLoader}
	 * @param proxyInterface the interface to proxy
	 * @param client the {@link JsonRpcHttpClient}
	 * @return the proxied interface
	 */
	public static <T> T createClientProxy(
		ClassLoader classLoader,
		Class<T> proxyInterface,
		final JsonRpcHttpClient client) {
		return createClientProxy(classLoader, proxyInterface, false, client, new HashMap<String, String>());
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
}
