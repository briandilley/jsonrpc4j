package com.googlecode.jsonrpc4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for reflection.
 */
public abstract class ReflectionUtil {
	
	private static final Map<String, Set<Method>> methodCache = new ConcurrentHashMap<>();
	
	private static final Map<Method, List<Class<?>>> parameterTypeCache = new ConcurrentHashMap<>();
	
	private static final Map<Method, List<Annotation>> methodAnnotationCache = new ConcurrentHashMap<>();
	
	private static final Map<Method, List<List<Annotation>>> methodParamAnnotationCache = new ConcurrentHashMap<>();

    private static final Map<Method, List<JsonRpcParam>> parametersNamesCache = new ConcurrentHashMap<>();

    private static final String NAME = "name";

    private static final Logger logger = LoggerFactory.getLogger(ReflectionUtil.class);

    private static final Set<Class<? extends Annotation>> webParamAnnotationClasses =
        loadWebParamAnnotationClasses();

    /**
	 * Finds methods with the given name on the given class.
	 *
	 * @param classes                    the classes
	 * @param name                       the method name
	 * @return the methods
	 */
	static Set<Method> findCandidateMethods(Class<?>[] classes, String name) {
		StringBuilder sb = new StringBuilder();
		for (Class<?> clazz : classes) {
			sb.append(clazz.getName()).append("::");
		}
		String cacheKey = sb.append(name).toString();
		if (methodCache.containsKey(cacheKey)) {
			return methodCache.get(cacheKey);
		}
		Set<Method> methods = new HashSet<>();
		for (Class<?> clazz : classes) {
            for (Method method : clazz.getMethods()) {
                if (method.isAnnotationPresent(JsonRpcMethod.class)) {
                    JsonRpcMethod methodAnnotation = method.getAnnotation(JsonRpcMethod.class);

                    if (methodAnnotation.required()) {
                        if (methodAnnotation.value().equals(name)) {
                            methods.add(method);
                        }
                    } else if (methodAnnotation.value().equals(name) || method.getName().equals(name)) {
                        methods.add(method);
                    }
                } else if (method.getName().equals(name)) {
                    methods.add(method);
                }
            }
        }
		methods = Collections.unmodifiableSet(methods);
		methodCache.put(cacheKey, methods);
		return methods;
	}
	
	/**
	 * Returns the parameter types for the given {@link Method}.
	 *
	 * @param method the {@link Method}
	 * @return the parameter types
	 */
	static List<Class<?>> getParameterTypes(Method method) {
		if (parameterTypeCache.containsKey(method)) {
			return parameterTypeCache.get(method);
		}
		List<Class<?>> types = new ArrayList<>();
		Collections.addAll(types, method.getParameterTypes());
		types = Collections.unmodifiableList(types);
		parameterTypeCache.put(method, types);
		return types;
	}
	
	/**
	 * Returns {@link Annotation}s of the given type defined
	 * on the given {@link Method}.
	 *
	 * @param <T>    the {@link Annotation} type
	 * @param method the {@link Method}
	 * @param type   the type
	 * @return the {@link Annotation}s
	 */
	public static <T extends Annotation> List<T> getAnnotations(Method method, Class<T> type) {
		return filterAnnotations(getAnnotations(method), type);
	}
	
	private static <T extends Annotation> List<T> filterAnnotations(Collection<Annotation> annotations, Class<T> type) {
		List<T> result = new ArrayList<>();
		for (Annotation annotation : annotations) {
			if (type.isInstance(annotation)) {
				result.add(type.cast(annotation));
			}
		}
		return result;
	}
	
	/**
	 * Returns all of the {@link Annotation}s defined on
	 * the given {@link Method}.
	 *
	 * @param method the {@link Method}
	 * @return the {@link Annotation}s
	 */
	private static List<Annotation> getAnnotations(Method method) {
		if (methodAnnotationCache.containsKey(method)) {
			return methodAnnotationCache.get(method);
		}
		List<Annotation> annotations = new ArrayList<>();
		Collections.addAll(annotations, method.getAnnotations());
		annotations = Collections.unmodifiableList(annotations);
		methodAnnotationCache.put(method, annotations);
		return annotations;
	}
	
	/**
	 * Returns the first {@link Annotation} of the given type
	 * defined on the given {@link Method}.
	 *
	 * @param <T>    the type
	 * @param method the method
	 * @param type   the type of annotation
	 * @return the annotation or null
	 */
	public static <T extends Annotation> T getAnnotation(Method method, Class<T> type) {
		for (Annotation a : getAnnotations(method)) {
			if (type.isInstance(a)) {
				return type.cast(a);
			}
		}
		return null;
	}
	
	/**
	 * Returns the parameter {@link Annotation}s of the
	 * given type for the given {@link Method}.
	 *
	 * @param <T>    the {@link Annotation} type
	 * @param type   the type
	 * @param method the {@link Method}
	 * @return the {@link Annotation}s
	 */
	static <T extends Annotation> List<List<T>> getParameterAnnotations(Method method, Class<T> type) {
		List<List<T>> annotations = new ArrayList<>();
		for (List<Annotation> paramAnnotations : getParameterAnnotations(method)) {
			annotations.add(filterAnnotations(paramAnnotations, type));
		}
		return annotations;
	}
	
	/**
	 * Returns the parameter {@link Annotation}s for the
	 * given {@link Method}.
	 *
	 * @param method the {@link Method}
	 * @return the {@link Annotation}s
	 */
	private static List<List<Annotation>> getParameterAnnotations(Method method) {
		if (methodParamAnnotationCache.containsKey(method)) {
			return methodParamAnnotationCache.get(method);
		}
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
	 *
	 * @param method    the method
	 * @param arguments the arguments
	 * @return the parsed arguments
	 */
	public static Object parseArguments(Method method, Object[] arguments) {

		JsonRpcParamsPassMode paramsPassMode = JsonRpcParamsPassMode.AUTO;
		JsonRpcMethod jsonRpcMethod = getAnnotation(method, JsonRpcMethod.class);
		if (jsonRpcMethod != null)
			paramsPassMode = jsonRpcMethod.paramsPassMode();

		Map<String, Object> params = new LinkedHashMap<>();

		params.putAll(getFixedParametersCollection(method));
		params.putAll(getFixedParameters(method));
		params.putAll(getNamedParameters(method, arguments));

		switch (paramsPassMode) {
			case ARRAY:
				if (params.size() > 0) {
					Object[] parsed = new Object[params.size()];
					int i = 0;
					for (Object value : params.values()) {
						parsed[i++] = value;
					}
					return parsed;
				} else {
					return arguments != null ? arguments : new Object[]{};
				}
			case OBJECT:
				if (params.size() > 0) {
					return params;
				} else {
					if (arguments == null) {
                        return new Object[]{};
                    }
					throw new IllegalArgumentException(
							"OBJECT parameters pass mode is impossible without declaring JsonRpcParam annotations for all parameters on method "
									+ method.getName());
				}
			case AUTO:
			default:
				if (params.size() > 0) {
					return params;
				} else {
					return arguments != null ? arguments : new Object[]{};
				}
		}
	}
	
	/**
	 * Checks method for @JsonRpcParam annotations and returns named parameters.
	 *
	 * @param method    the method
	 * @param arguments the arguments
	 * @return named parameters or empty if no annotations found
	 * @throws IllegalArgumentException if some parameters are annotated and others not
	 */
	private static Map<String, Object> getNamedParameters(Method method, Object[] arguments) {
		
		Map<String, Object> namedParams = new LinkedHashMap<>();
		
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
		
		if (arguments != null && arguments.length > 0 && namedParams.size() > 0 && namedParams.size() != arguments.length) {
			throw new IllegalArgumentException("JsonRpcParam annotations were not found for all parameters on method " + method.getName());
		}
		
		return namedParams;
	}

    private static Set<Class<? extends Annotation>> loadWebParamAnnotationClasses() {
        final ClassLoader classLoader = ReflectionUtil.class.getClassLoader();
        Set<Class<? extends Annotation>> webParamClasses = new HashSet<>(2, 1.0f);
        for (String className: Arrays.asList("javax.jws.WebParam", "jakarta.jws.WebParam")) {
            try {
                Class<? extends Annotation> clazz =
                    classLoader
                        .loadClass(className)
                        .asSubclass(Annotation.class);
                // check that method with name "name" is present
                clazz.getMethod(NAME);
                webParamClasses.add(clazz);
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                logger.debug("Could not find {}.{}", className, NAME);
            }
        }

        if (webParamClasses.isEmpty()) {
            logger.debug(
                "Could not find any @WebParam classes in classpath." +
                    " @WebParam support is disabled"
            );
        }

        return Collections.unmodifiableSet(webParamClasses);
    }

    /**
     * Checks method for {@link JsonRpcParam}, javax.jws.WebParam and jakarta.jws.WebParam annotations,
     * and returns all parameters names declared for this method.
     *
     * @param method the method
     * @param logger the logger
     * @return a list of parameter names as {@link JsonRpcParam} objects.
     * All names from the javax.jws.WebParam and jakarta.jws.WebParam annotations are copied into
     * {@link JsonRpcParam} objects.
     */
    @SuppressWarnings("Convert2streamapi")
    public static List<JsonRpcParam> getAnnotatedParameterNames(Method method, Logger logger) {
        List<JsonRpcParam> paramNames = parametersNamesCache.get(method);
        if (paramNames != null) {
            return paramNames;
        }

        int parameterCount = method.getParameterCount();
        paramNames = new ArrayList<>(parameterCount);

        List<List<Annotation>> parametersAnnotations = getParameterAnnotations(method);
        for (int i = 0; i < parameterCount; i++) {
            List<Annotation> parameterAnnotations = parametersAnnotations.get(i);
            List<JsonRpcParam> declaredNames = new ArrayList<>();

            for (Annotation annotation : parameterAnnotations) {
                if (annotation instanceof JsonRpcParam) {
                    declaredNames.add((JsonRpcParam) annotation);
                }

                for (Class<? extends Annotation> clazz : webParamAnnotationClasses) {
                    if (clazz.isInstance(annotation)) {
                        declaredNames.add(
                            createNewJsonRcpParamType(annotation)
                        );
                    }
                }
            }

            JsonRpcParam paramName;
            if (declaredNames.size() > 1) {
                paramName = declaredNames.get(0);
                for (JsonRpcParam name : declaredNames) {
                    if (!Objects.equals(paramName.value(), name.value())) {
                        logger.warn(
                            "Method '{}' has multiple parameter names declared"
                                + " for the parameter at index {}."
                                + " Only the first name '{}' can be used."
                                + " Create additional parameters "
                                + " if alternative names are required.",
                            method.toGenericString(),
                            i,
                            paramName.value()
                        );
                    }
                }

            } else if (!declaredNames.isEmpty()) {
                paramName = declaredNames.get(0);
            } else {
                paramName = null;
                logger.warn(
                    "Method '{}' has no parameter name declared"
                        + " for the parameter at index {}.",
                    method.toGenericString(),
                    i
                );
            }

            paramNames.add(paramName);
        }

        parametersNamesCache.putIfAbsent(method, Collections.unmodifiableList(paramNames));

        return paramNames;
    }

    private static JsonRpcParam createNewJsonRcpParamType(final Annotation annotation) {
        return new JsonRpcParam() {
            public Class<? extends Annotation> annotationType() {
                return JsonRpcParam.class;
            }

            public String value() {
                try {
                    Method method = annotation.getClass().getMethod(JsonRpcBasicServer.NAME);
                    return (String) method.invoke(annotation);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private static List<List<? extends Annotation>> getWebParameterAnnotations(Method method) {
        List<List<? extends Annotation>> annotations = new ArrayList<>();
        for (Class<? extends Annotation> clazz : webParamAnnotationClasses) {
            annotations.addAll(
                ReflectionUtil.getParameterAnnotations(method, clazz)
            );
        }
        return annotations;
    }

    private List<List<JsonRpcParam>> getJsonRpcParamAnnotations(Method method) {
        return ReflectionUtil.getParameterAnnotations(method, JsonRpcParam.class);
    }

	/**
	 * Checks method for @JsonRpcFixedParam annotations and returns fixed
	 * parameters.
	 * 
	 * @param method the method
	 * @return fixed parameters or empty if no annotations found
	 */
	public static Map<String, Object> getFixedParameters(Method method) {

		Map<String, Object> fixedParams = new LinkedHashMap<>();

		for (Annotation an : getAnnotations(method)) {
			if (an instanceof JsonRpcFixedParam) {
				JsonRpcFixedParam jAnn = (JsonRpcFixedParam) an;
				fixedParams.put(jAnn.name(), jAnn.value());
			}
		}

		return fixedParams;
	}

	/**
	 * Checks method for @JsonRpcFixedParams annotation and returns fixed
	 * parameters.
	 * 
	 * @param method the method
	 * @return fixed parameters or empty if no annotations found
	 */
	public static Map<String, Object> getFixedParametersCollection(Method method) {

		Map<String, Object> fixedParams = new LinkedHashMap<>();

		JsonRpcFixedParams jsonRpcFixedParams = getAnnotation(method, JsonRpcFixedParams.class);

		if (jsonRpcFixedParams != null) {
			for (JsonRpcFixedParam fixedParam : jsonRpcFixedParams.fixedParams()) {
				fixedParams.put(fixedParam.name(), fixedParam.value());
			}
		}

		return fixedParams;
	}

	public static void clearCache() {
		methodCache.clear();
		parameterTypeCache.clear();
		methodAnnotationCache.clear();
		methodParamAnnotationCache.clear();
        parametersNamesCache.clear();
	}
}
