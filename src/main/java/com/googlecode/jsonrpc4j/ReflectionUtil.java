package com.googlecode.jsonrpc4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Utilities for reflection.
 */
@SuppressWarnings("unused")
public abstract class ReflectionUtil {

	private static final Map<String, Set<Method>> methodCache = new ConcurrentHashMap<>();

	private static final Map<Method, List<Class<?>>> parameterTypeCache = new ConcurrentHashMap<>();

	private static final Map<Method, List<Annotation>> methodAnnotationCache = new ConcurrentHashMap<>();

	private static final Map<Method, List<List<Annotation>>> methodParamAnnotationCache = new ConcurrentHashMap<>();

	/**
	 * Finds methods with the given name on the given class.
	 * @param classes the classes
	 * @param name the method name
	 * @return the methods
	 */
	static Set<Method> findCandidateMethods(Class<?>[] classes, String name) {
		StringBuilder sb = new StringBuilder();
		for (Class<?> clazz : classes) {
			sb.append(clazz.getName()).append("::");
		}
		String cacheKey = sb.append(name).toString();
		if (methodCache.containsKey(cacheKey)) { return methodCache.get(cacheKey); }
		Set<Method> methods = new HashSet<>();
		for (Class<?> clazz : classes) {
			for (Method method : clazz.getMethods()) {
				if (method.getName().equals(name) || annotationMatches(method, name)) {
					methods.add(method);
				}
			}
		}
		methods = Collections.unmodifiableSet(methods);
		methodCache.put(cacheKey, methods);
		return methods;
	}

	/**
	 * Checks for the annotation {@link JsonRpcMethod} on {@code method} to see if its value matches {@code name}
	 * @param method the method to check
	 * @param name the expected method name
	 * @return true if {@code method} is named {@code name}
	 */
	private static boolean annotationMatches(Method method, String name) {
		if (method.isAnnotationPresent(JsonRpcMethod.class)) {
			JsonRpcMethod methodAnnotation = method.getAnnotation(JsonRpcMethod.class);
			if (methodAnnotation.value().equals(name)) { return true; }
		}
		return false;
	}

	/**
	 * Returns the parameter types for the given {@link Method}.
	 * @param method the {@link Method}
	 * @return the parameter types
	 */
	static List<Class<?>> getParameterTypes(Method method) {
		if (parameterTypeCache.containsKey(method)) { return parameterTypeCache.get(method); }
		List<Class<?>> types = new ArrayList<>();
		Collections.addAll(types, method.getParameterTypes());
		types = Collections.unmodifiableList(types);
		parameterTypeCache.put(method, types);
		return types;
	}

	/**
	 * Returns {@link Annotation}s of the given type defined
	 * on the given {@link Method}.
	 * @param <T> the {@link Annotation} type
	 * @param method the {@link Method}
	 * @param type the type
	 * @return the {@link Annotation}s
	 */
	public static <T extends Annotation> List<T> getAnnotations(Method method, Class<T> type) {
		return getAnnotations(method).stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
	}

	/**
	 * Returns all of the {@link Annotation}s defined on
	 * the given {@link Method}.
	 * @param method the {@link Method}
	 * @return the {@link Annotation}s
	 */
	private static List<Annotation> getAnnotations(Method method) {
		if (methodAnnotationCache.containsKey(method)) { return methodAnnotationCache.get(method); }
		List<Annotation> annotations = new ArrayList<>();
		Collections.addAll(annotations, method.getAnnotations());
		annotations = Collections.unmodifiableList(annotations);
		methodAnnotationCache.put(method, annotations);
		return annotations;
	}

	/**
	 * Returns the first {@link Annotation} of the given type
	 * defined on the given {@link Method}.
	 * @param <T> the type
	 * @param method the method
	 * @param type the type of annotation
	 * @return the annotation or null
	 */
	static <T extends Annotation> T getAnnotation(Method method, Class<T> type) {
		for (Annotation a : getAnnotations(method)) {
			if (type.isInstance(a)) { return type.cast(a); }
		}
		return null;
	}

	/**
	 * Returns the parameter {@link Annotation}s of the
	 * given type for the given {@link Method}.
	 * @param <T> the {@link Annotation} type
	 * @param type the type
	 * @param method the {@link Method}
	 * @return the {@link Annotation}s
	 */
	static <T extends Annotation> List<List<T>> getParameterAnnotations(Method method, Class<T> type) {
		List<List<T>> annotations = new ArrayList<>();
		for (List<Annotation> paramAnnotations : getParameterAnnotations(method)) {
			List<T> listAnnotations = paramAnnotations.stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
			annotations.add(listAnnotations);
		}
		return annotations;
	}

	/**
	 * Returns the parameter {@link Annotation}s for the
	 * given {@link Method}.
	 * @param method the {@link Method}
	 * @return the {@link Annotation}s
	 */
	private static List<List<Annotation>> getParameterAnnotations(Method method) {
		if (methodParamAnnotationCache.containsKey(method)) { return methodParamAnnotationCache.get(method); }
		List<List<Annotation>> annotations = new ArrayList<>();
		for (Annotation[] paramAnnotations : method.getParameterAnnotations()) {
			List<Annotation> listAnnotations = new ArrayList<>();
			Collections.addAll(listAnnotations, paramAnnotations);
			annotations.add(listAnnotations);
		}
		annotations = Collections.unmodifiableList(annotations);
		methodParamAnnotationCache.put(method, annotations);
		return annotations;
	}

	/**
	 * Parses the given arguments for the given method optionally
	 * turning them into named parameters.
	 * @param method the method
	 * @param arguments the arguments
	 * @return the parsed arguments
	 */
	public static Object parseArguments(Method method, Object[] arguments) {

		Map<String, Object> namedParams = getNamedParameters(method, arguments);

		if (namedParams.size() > 0) {
			return namedParams;
		} else {
			return arguments != null ? arguments : new Object[] {};
		}
	}

	/**
	 * Checks method for @JsonRpcParam annotations and returns named parameters.
	 * @param method the method
	 * @param arguments the arguments
	 * @return named parameters or empty if no annotations found
	 * @throws RuntimeException if some parameters are annotated and others not
	 */
	private static Map<String, Object> getNamedParameters(Method method, Object[] arguments) {

		Map<String, Object> namedParams = new HashMap<>();

		Annotation[][] paramAnnotations = method.getParameterAnnotations();
		for (int i = 0; i < paramAnnotations.length; i++) {
			Annotation[] ann = paramAnnotations[i];
			for (Annotation an : ann) {
				if (JsonRpcParam.class.isInstance(an)) {
					JsonRpcParam jAnn = (JsonRpcParam) an;
					namedParams.put(jAnn.value(), arguments[i]);
					break;
				}
			}
		}

		if (arguments != null && arguments.length > 0 && namedParams.size() > 0 && namedParams.size() != arguments.length) { throw new RuntimeException("JsonRpcParam annotations were not found for all parameters on method " + method.getName()); }

		return namedParams;
	}
}
