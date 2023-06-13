package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.googlecode.jsonrpc4j.ErrorResolver.JsonError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.ERROR_NOT_HANDLED;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.INTERNAL_ERROR;
import static com.googlecode.jsonrpc4j.ReflectionUtil.findCandidateMethods;
import static com.googlecode.jsonrpc4j.ReflectionUtil.getParameterTypes;
import static com.googlecode.jsonrpc4j.Util.hasNonNullData;

/**
 * A JSON-RPC request server reads JSON-RPC requests from an input stream and writes responses to an output stream.
 * Can even run on Android system.
 */
public class JsonRpcBasicServer {
	
	public static final String JSONRPC_CONTENT_TYPE = "application/json-rpc";
	public static final String PARAMS = "params";
	public static final String METHOD = "method";
	public static final String JSONRPC = "jsonrpc";
	public static final String ID = "id";
	public static final String CONTENT_ENCODING = "Content-Encoding";
	public static final String ACCEPT_ENCODING = "Accept-Encoding";
	public static final String ERROR = "error";
	public static final String ERROR_MESSAGE = "message";
	public static final String ERROR_CODE = "code";
	public static final String DATA = "data";
	public static final String RESULT = "result";
	public static final String EXCEPTION_TYPE_NAME = "exceptionTypeName";
	public static final String VERSION = "2.0";
	public static final int CODE_OK = 0;
	public static final String NAME = "name";
	public static final String NULL = "null";
	private static final Logger logger = LoggerFactory.getLogger(JsonRpcBasicServer.class);
	private static final ErrorResolver DEFAULT_ERROR_RESOLVER = new MultipleErrorResolver(AnnotationsErrorResolver.INSTANCE, DefaultErrorResolver.INSTANCE);
	private static final Pattern BASE64_PATTERN = Pattern.compile("[A-Za-z0-9_=-]+");
	
	private final ObjectMapper mapper;
	private final Class<?> remoteInterface;
	private final Object handler;
	protected HttpStatusCodeProvider httpStatusCodeProvider = null;
	private boolean backwardsCompatible = true;
	private boolean rethrowExceptions = false;
	private boolean allowExtraParams = false;
	private boolean allowLessParams = false;
	private RequestInterceptor requestInterceptor = null;
	private ErrorResolver errorResolver = null;
	private InvocationListener invocationListener = null;
	private ConvertedParameterTransformer convertedParameterTransformer = null;
	private boolean shouldLogInvocationErrors = true;
	private List<JsonRpcInterceptor> interceptorList = new ArrayList<>();
    private ExecutorService batchExecutorService = null;
    private long parallelBatchProcessingTimeout = Long.MAX_VALUE;
	private final Set<Class<? extends Annotation>> webParamAnnotationClasses;

	/**
	 * Creates the server with the given {@link ObjectMapper} delegating
	 * all calls to the given {@code handler}.
	 *
	 * @param mapper  the {@link ObjectMapper}
	 * @param handler the {@code handler}
	 */
	public JsonRpcBasicServer(final ObjectMapper mapper, final Object handler) {
		this(mapper, handler, null);
	}
	
	/**
	 * Creates the server with the given {@link ObjectMapper} delegating
	 * all calls to the given {@code handler} {@link Object} but only
	 * methods available on the {@code remoteInterface}.
	 *
	 * @param mapper          the {@link ObjectMapper}
	 * @param handler         the {@code handler}
	 * @param remoteInterface the interface
	 */
	public JsonRpcBasicServer(final ObjectMapper mapper, final Object handler, final Class<?> remoteInterface) {
		this.mapper = mapper;
		this.handler = handler;
		this.remoteInterface = remoteInterface;
		this.webParamAnnotationClasses = loadWebParamAnnotationClasses();
		if (handler != null) {
			logger.debug("created server for interface {} with handler {}", remoteInterface, handler.getClass());
		}
	}
	
	/**
	 * Creates the server with a default {@link ObjectMapper} delegating
	 * all calls to the given {@code handler} {@link Object} but only
	 * methods available on the {@code remoteInterface}.
	 *
	 * @param handler         the {@code handler}
	 * @param remoteInterface the interface
	 */
	public JsonRpcBasicServer(final Object handler, final Class<?> remoteInterface) {
		this(new ObjectMapper(), handler, remoteInterface);
	}
	
	/**
	 * Creates the server with a default {@link ObjectMapper} delegating
	 * all calls to the given {@code handler}.
	 *
	 * @param handler the {@code handler}
	 */
	public JsonRpcBasicServer(final Object handler) {
		this(new ObjectMapper(), handler, null);
	}
	
	private Set<Class<? extends Annotation>> loadWebParamAnnotationClasses() {
		final ClassLoader classLoader = JsonRpcBasicServer.class.getClassLoader();
		Set<Class<? extends Annotation>> webParamClasses = new HashSet<>(2);
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
	 * Returns parameters into an {@link InputStream} of JSON data.
	 *
	 * @param method the method
	 * @param id     the id
	 * @param params the base64 encoded params
	 * @return the {@link InputStream}
	 * @throws IOException on error
	 */
	static InputStream createInputStream(String method, String id, String params) throws IOException {
		
		StringBuilder envelope = new StringBuilder();
		
		envelope.append("{\"");
		envelope.append(JSONRPC);
		envelope.append("\":\"");
		envelope.append(VERSION);
		envelope.append("\",\"");
		envelope.append(ID);
		envelope.append("\":");
		
		// the 'id' value is assumed to be numerical.
		
		if (null != id && !id.isEmpty()) {
			envelope.append(id);
		} else {
			envelope.append("null");
		}
		
		envelope.append(",\"");
		envelope.append(METHOD);
		envelope.append("\":");
		
		if (null != method && !method.isEmpty()) {
			envelope.append('"');
			envelope.append(method);
			envelope.append('"');
		} else {
			envelope.append("null");
		}
		envelope.append(",\"");
		envelope.append(PARAMS);
		envelope.append("\":");
		
		if (null != params && !params.isEmpty()) {
			String decodedParams;
			
			// some specifications suggest that the GET "params" query parameter should be Base64 encoded and
			// some suggest not.  Try to deal with both scenarios -- the code here was previously only doing
			// Base64 decoding.
			// http://www.simple-is-better.org/json-rpc/transport_http.html
			// http://www.jsonrpc.org/historical/json-rpc-over-http.html#encoded-parameters
			
			if (BASE64_PATTERN.matcher(params).matches()) {
				decodedParams = new String(Base64.getDecoder().decode(params), StandardCharsets.UTF_8);
			} else {
				switch (params.charAt(0)) {
					case '[':
					case '{':
						decodedParams = params;
						break;
					
					default:
						throw new IOException("badly formed 'param' parameter starting with; [" + params.charAt(0) + "]");
				}
			}
			
			envelope.append(decodedParams);
		} else {
			envelope.append("[]");
		}
		
		envelope.append('}');
		
		return new ByteArrayInputStream(envelope.toString().getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * Handles a single request from the given {@link InputStream},
	 * that is to say that a single {@link JsonNode} is read from
	 * the stream and treated as a JSON-RPC request.  All responses
	 * are written to the given {@link OutputStream}.
	 *
	 * @param input  the {@link InputStream}
	 * @param output the {@link OutputStream}
	 * @return the error code, or {@code 0} if none
	 * @throws IOException on error
	 */
	public int handleRequest(final InputStream input, final OutputStream output) throws IOException {
		final ReadContext readContext = ReadContext.getReadContext(input, mapper);
		try {
			readContext.assertReadable();
			final JsonNode jsonNode = readContext.nextValue();
			for (JsonRpcInterceptor interceptor : interceptorList) {
				interceptor.preHandleJson(jsonNode);
			}
            JsonResponse jsonResponse = handleJsonNodeRequest(jsonNode);
			writeAndFlushValue(output, jsonResponse.getResponse());
			if (jsonResponse.getExceptionToRethrow() != null) {
			    throw jsonResponse.getExceptionToRethrow();
            }
			return jsonResponse.getCode();
        } catch (JsonParseException | JsonMappingException e) {
            JsonResponse responseError = createResponseError(VERSION, NULL, JsonError.PARSE_ERROR);
            writeAndFlushValue(output, responseError.getResponse());
            return responseError.getCode();
		}
	}
	
	/**
	 * Returns the handler's class or interfaces.  The variable serviceName is ignored in this class.
	 *
	 * @param serviceName the optional name of a service
	 * @return the class
	 */
	protected Class<?>[] getHandlerInterfaces(final String serviceName) {
		if (remoteInterface != null) {
			return new Class<?>[]{remoteInterface};
		} else if (Proxy.isProxyClass(handler.getClass())) {
			return handler.getClass().getInterfaces();
		} else {
			return new Class<?>[]{handler.getClass()};
		}
	}
	
	/**
	 * Handles the given {@link JsonNode} and creates {@link JsonResponse}
	 *
	 * @param node the {@link JsonNode}
	 * @return the {@link JsonResponse} instance
	 */
    protected JsonResponse handleJsonNodeRequest(final JsonNode node)
            throws JsonParseException, JsonMappingException {
        if (node.isArray()) {
            return handleArray((ArrayNode) node);
        }
        if (node.isObject()) {
            return handleObject((ObjectNode) node);
        }
        return createResponseError(VERSION, NULL, JsonError.INVALID_REQUEST);
    }

	/**
	 * Handles the given {@link ArrayNode} and creates {@link JsonResponse}
	 * if {@code batchExecutorService} is configured, then handles batch in parallel otherwise handles it sequentially
     *
	 * @param node the {@link JsonNode}
	 * @return the {@link JsonResponse} instance
	 */
	private JsonResponse handleArray(ArrayNode node) {
        logger.debug("Handling {} requests", node.size());

        if (batchExecutorService != null) {
            return getBatchResponseInParallel(node);
        } else {
            return getBatchResponseSequentially(node);
        }
	}

    /**
     * Handles the given {@link ArrayNode} sequentially and creates {@link JsonResponse}
     *
     * @param node the {@link JsonNode}
     * @return the {@link JsonResponse} instance
     */
    private JsonResponse getBatchResponseSequentially(ArrayNode node) {
        JsonError result = JsonError.OK;
        ArrayNode batchResult = mapper.createArrayNode();
        int errorCount = 0;
        JsonResponse response = new JsonResponse();

        for (int i = 0; i < node.size(); i++) {
	        JsonResponse nodeResult;
        	try {
		        nodeResult = handleJsonNodeRequest(node.get(i));
	        } catch(Exception e) {
		        nodeResult = createResponseError(VERSION, NULL, JsonError.PARSE_ERROR);
	        }
            handleRethrowException(response, nodeResult);
            batchResult.add(nodeResult.getResponse());
            if (isError(nodeResult)) {
                result = JsonError.BULK_ERROR;
                errorCount += 1;
            }
        }

        logger.debug("served {} requests, error {}, result {}", node.size(), errorCount, result);

        response.setResponse(batchResult);
        response.setCode(result.getCode());
        return response;
    }

    /**
     * Handles the given {@link ArrayNode} in parallel and creates {@link JsonResponse}
     *
     * @param node the {@link JsonNode}
     * @return the {@link JsonResponse} instance
     */
	private JsonResponse getBatchResponseInParallel(ArrayNode node) {
        JsonError result = JsonError.OK;
        ArrayNode batchResult = mapper.createArrayNode();
        int errorCount = 0;
        JsonResponse response = new JsonResponse();

        Map<Object, Future<JsonResponse>> responses = new HashMap<>();
        for (int i = 0; i < node.size(); i++) {
            JsonNode jsonNode = node.get(i);
            Object id = parseId(jsonNode.get(ID));
            Future<JsonResponse> responseFuture = batchExecutorService.submit(() -> handleJsonNodeRequest(jsonNode));
            responses.put(id, responseFuture);
        }

        for (Map.Entry<Object, Future<JsonResponse>> responseFuture : responses.entrySet()) {
            JsonResponse singleJsonResponse = getSingleJsonResponse(responseFuture);
            handleRethrowException(response, singleJsonResponse);
            batchResult.add(singleJsonResponse.getResponse());
            if (isError(singleJsonResponse)) {
                result = JsonError.BULK_ERROR;
                errorCount += 1;
            }
        }

        logger.debug("served {} requests, error {}, result {}", node.size(), errorCount, result);

        response.setResponse(batchResult);
        response.setCode(result.getCode());
        return response;
    }


    private void handleRethrowException(JsonResponse response, JsonResponse singleJsonResponse) {
        if (singleJsonResponse.getExceptionToRethrow() != null && response.getExceptionToRethrow() == null) {
            response.setExceptionToRethrow(singleJsonResponse.getExceptionToRethrow());
        }
    }

    /**
     * Gets the {@link JsonResponse} from the {@link Future}
     *
     * @param responseFuture {@link Map.Entry} with an id of the JSON-RPC request as a key
     *                       and {@link Future} with {@link JsonResponse} as a value
     * @return the {@link JsonResponse} instance
     */
    private JsonResponse getSingleJsonResponse(Map.Entry<Object, Future<JsonResponse>> responseFuture) {
        JsonResponse response;
	    try {
            response = responseFuture.getValue().get(parallelBatchProcessingTimeout, TimeUnit.MILLISECONDS);
        } catch (Throwable t) {
            JsonError jsonError = new JsonError(INTERNAL_ERROR.code, t.getMessage(), t.getClass().getName());
            return createResponseError(VERSION, responseFuture.getKey(), jsonError);
        }
	    return response;
    }

    private boolean isError(JsonResponse result) {
		return result.getCode() != JsonError.OK.code;
	}
	
	/**
	 * Handles the given {@link ObjectNode} and creates {@link JsonResponse}
	 *
	 * @param node   the {@link JsonNode}
	 * @return the {@link JsonResponse} instance
	 */
	private JsonResponse handleObject(final ObjectNode node)
			throws JsonParseException, JsonMappingException {
		logger.debug("Request: {}", node);
		
		if (!isValidRequest(node)) {
			return createResponseError(VERSION, NULL, JsonError.INVALID_REQUEST);
		}
		Object id = parseId(node.get(ID));
		
		String jsonRpc = hasNonNullData(node, JSONRPC) ? node.get(JSONRPC).asText() : VERSION;
		if (!hasNonNullData(node, METHOD)) {
			return createResponseError(jsonRpc, id, JsonError.METHOD_NOT_FOUND);
		}

		final String fullMethodName = node.get(METHOD).asText();
		final String partialMethodName = getMethodName(fullMethodName);
		final String serviceName = getServiceName(fullMethodName);
		
		Set<Method> methods = findCandidateMethods(getHandlerInterfaces(serviceName), partialMethodName);
		if (methods.isEmpty()) {
			return createResponseError(jsonRpc, id, JsonError.METHOD_NOT_FOUND);
		}
		AMethodWithItsArgs methodArgs = findBestMethodByParamsNode(methods, node.get(PARAMS));
		if (methodArgs == null) {
			return createResponseError(jsonRpc, id, JsonError.METHOD_PARAMS_INVALID);
		}
		try (InvokeListenerHandler handler = new InvokeListenerHandler(methodArgs, invocationListener)) {
			try {
				if (this.requestInterceptor != null) {
					this.requestInterceptor.interceptRequest(node);
				}
				Object target = getHandler(serviceName);
				// interceptors preHandle
				for (JsonRpcInterceptor interceptor : interceptorList) {
					interceptor.preHandle(target, methodArgs.method, methodArgs.arguments);
				}
				// invocation
				JsonNode result = invoke(target, methodArgs.method, methodArgs.arguments);
				handler.result = result;
				// interceptors postHandle
				for (JsonRpcInterceptor interceptor : interceptorList) {
					interceptor.postHandle(target, methodArgs.method, methodArgs.arguments, result);
				}
				if (!isNotificationRequest(id)) {
					return createResponseSuccess(jsonRpc, id, handler.result);
				}
				return new JsonResponse(null, JsonError.OK.code);
			} catch (JsonParseException | JsonMappingException e) {
				throw e; // rethrow this, it will be handled as PARSE_ERROR later
			} catch (ParameterConvertException pce) {
				handler.error = pce.getCause();
				return handleParameterConvertError(pce, id, jsonRpc);
			} catch (Throwable e) {
				handler.error = e;
				return handleError(id, jsonRpc, methodArgs, e);
			}
		}
	}

	private JsonResponse handleParameterConvertError(ParameterConvertException pce, Object id, String jsonRpc) {
		String errorMsg = "Failed to read method parameter at index " + pce.paramIndex;
		JsonError jsonError = new JsonError(
			JsonError.METHOD_PARAMS_INVALID.code,
			errorMsg,
			null
		);
		return createResponseError(jsonRpc, id, jsonError);
	}

    private JsonResponse handleError(Object id, String jsonRpc, AMethodWithItsArgs methodArgs, Throwable e) {
		Throwable unwrappedException = getException(e);
		
		if (shouldLogInvocationErrors) {
			logger.warn("Error in JSON-RPC Service", unwrappedException);
		}
		
		JsonError error = resolveError(methodArgs, unwrappedException);
        JsonResponse responseError = createResponseError(jsonRpc, id, error);
        if (rethrowExceptions) {
            responseError.setExceptionToRethrow(new RuntimeException(unwrappedException));
        }
        return responseError;

	}
	
	private Throwable getException(final Throwable thrown) {
		Throwable e = thrown;
		while (e instanceof InvocationTargetException) {
			// noinspection ThrowableResultOfMethodCallIgnored
			e = ((InvocationTargetException) e).getTargetException();
			while (e instanceof UndeclaredThrowableException) {
				// noinspection ThrowableResultOfMethodCallIgnored
				e = ((UndeclaredThrowableException) e).getUndeclaredThrowable();
			}
		}
		return e;
	}
	
	private JsonError resolveError(AMethodWithItsArgs methodArgs, Throwable e) {
		JsonError error;
		final ErrorResolver currentResolver = errorResolver == null ? DEFAULT_ERROR_RESOLVER : errorResolver;
		error = currentResolver.resolveError(e, methodArgs.method, methodArgs.arguments);
		if (error == null) {
			error = new JsonError(ERROR_NOT_HANDLED.code, e.getMessage(), e.getClass().getName());
		}
		return error;
	}
	
	private boolean isNotificationRequest(Object id) {
		return id == null;
	}
	
	private boolean isValidRequest(ObjectNode node) {
		return backwardsCompatible || hasMethodAndVersion(node);
	}
	
	private boolean hasMethodAndVersion(ObjectNode node) {
		return node.has(JSONRPC) && node.has(METHOD);
	}
	
	/**
	 * Get the service name from the methodNode.  In this class, it is always
	 * <code>null</code>.  Subclasses may parse the methodNode for service name.
	 *
	 * @param methodName the JsonNode for the method
	 * @return the name of the service, or <code>null</code>
	 */
	protected String getServiceName(final String methodName) {
		return null;
	}
	
	/**
	 * Get the method name from the methodNode.
	 *
	 * @param methodName the JsonNode for the method
	 * @return the name of the method that should be invoked
	 */
	protected String getMethodName(final String methodName) {
		return methodName;
	}
	
	/**
	 * Get the handler (object) that should be invoked to execute the specified
	 * RPC method.  Used by subclasses to return handlers specific to a service.
	 *
	 * @param serviceName an optional service name
	 * @return the handler to invoke the RPC call against
	 */
	protected Object getHandler(String serviceName) {
		return handler;
	}
	
	/**
	 * Invokes the given method on the {@code handler} passing
	 * the given params (after converting them to beans\objects)
	 * to it.
	 *
	 * @param target optional service name used to locate the target object
	 *               to invoke the Method on
	 * @param method the method to invoke
	 * @param params the params to pass to the method
	 * @return the return value (or null if no return)
	 * @throws IOException               on error
	 * @throws IllegalAccessException    on error
	 * @throws InvocationTargetException on error
	 */
	private JsonNode invoke(Object target, Method method, List<JsonNode> params) throws IOException, IllegalAccessException, InvocationTargetException {
		logger.debug("Invoking method: {} with args {}", method.getName(), params);

		Object result;

        if (method.getGenericParameterTypes().length == 1 && method.isVarArgs()) {
			Class<?> componentType = method.getParameterTypes()[0].getComponentType();
			result = componentType.isPrimitive() ?
				invokePrimitiveVarargs(target, method, params, componentType) :
				invokeNonPrimitiveVarargs(target, method, params, componentType);
        } else {
            Object[] convertedParams = convertJsonToParameters(method, params);
			if (convertedParameterTransformer != null) {
				convertedParams = convertedParameterTransformer.transformConvertedParameters(target, convertedParams);
			}
			result = method.invoke(target, convertedParams);
        }

		logger.debug("Invoked method: {}, result {}", method.getName(), result);

		return hasReturnValue(method) ? mapper.valueToTree(result) : null;
	}

	private Object invokePrimitiveVarargs(Object target, Method method, List<JsonNode> params, Class<?> componentType) throws IllegalAccessException, InvocationTargetException {
		// need to cast to object here in order to support primitives.
		Object convertedParams = Array.newInstance(componentType, params.size());

		for (int i = 0; i < params.size(); i++) {
			Object object = convertAndLogParam(method, params, i);
			Array.set(convertedParams, i, object);
		}

		return method.invoke(target, convertedParams);
	}

	private Object invokeNonPrimitiveVarargs(Object target, Method method, List<JsonNode> params, Class<?> componentType) throws IllegalAccessException, InvocationTargetException {
		Object[] convertedParams = (Object[]) Array.newInstance(componentType, params.size());

		for (int i = 0; i < params.size(); i++) {
			Object object = convertAndLogParam(method, params, i);
			convertedParams[i] = object;
		}

		return method.invoke(target, new Object[] { convertedParams });
	}

	private Object convertAndLogParam(Method method, List<JsonNode> params, int paramIndex) {
		JsonNode jsonNode = params.get(paramIndex);
		Class<?> type = JsonUtil.getJavaTypeForJsonType(jsonNode);
		Object object;
		try {
			object = mapper.convertValue(jsonNode, type);
		} catch (IllegalArgumentException e) {
			logger.debug(
				"[{}] Failed to convert param: {} -> {}",
				method.getName(),
				paramIndex,
				type.getName()
			);
			throw new ParameterConvertException(paramIndex, e);
		}
		logger.debug("[{}] param: {} -> {}", method.getName(), paramIndex, type.getName());
		return object;
	}

	private boolean hasReturnValue(Method m) {
		return !"void".equalsIgnoreCase(m.getGenericReturnType().getTypeName());
	}
	
	private Object[] convertJsonToParameters(Method m, List<JsonNode> params) throws IOException {
		Object[] convertedParams = new Object[params.size()];
		Type[] parameterTypes = m.getGenericParameterTypes();
		
		for (int i = 0; i < parameterTypes.length; i++) {
			JsonParser paramJsonParser = mapper.treeAsTokens(params.get(i));
			JavaType paramJavaType = mapper.getTypeFactory().constructType(parameterTypes[i]);
			ObjectReader reader =
				mapper
					.readerFor(paramJavaType)
					.with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
			try {
				convertedParams[i] = reader.readValue(paramJsonParser);
			} catch (JsonParseException | JsonMappingException e) {
				logger.debug(
					"[{}] Failed to convert param: {} -> {}",
					m.getName(),
					i,
					parameterTypes[i].getTypeName()
				);
				throw new ParameterConvertException(i, e);
			}
		}
		return convertedParams;
	}

    /**
     * Creates a response.
     *
     * @param jsonRpc the version string
     * @param id      the id of the request
     * @param result  the result object
     * @param errorObject the error data (if any)
     * @return the response object
     */
    private JsonResponse createResponse(String jsonRpc, Object id, JsonNode result, JsonError errorObject) {
        ObjectNode response = mapper.createObjectNode();
        response.put(JSONRPC, jsonRpc);
        if (id instanceof Integer) {
            response.put(ID, ((Integer) id).intValue());
        } else if (id instanceof Long) {
            response.put(ID, ((Long) id).longValue());
        } else if (id instanceof Float) {
            response.put(ID, ((Float) id).floatValue());
        } else if (id instanceof Double) {
            response.put(ID, ((Double) id).doubleValue());
        } else if (id instanceof BigDecimal) {
            response.put(ID, (BigDecimal) id);
        } else {
            response.put(ID, (String) id);
        }

        int responseCode = JsonError.OK.code;
        if (errorObject != null) {
            ObjectNode error = mapper.createObjectNode();
            error.put(ERROR_CODE, errorObject.code);
            error.put(ERROR_MESSAGE, errorObject.message);
            if (errorObject.data != null) {
                error.set(DATA, mapper.valueToTree(errorObject.data));
            }
            responseCode = errorObject.getCode();
            response.set(ERROR, error);
        } else {
            response.set(RESULT, result);
        }

        for (JsonRpcInterceptor interceptor : interceptorList) {
            interceptor.postHandleJson(response);
        }

        return new JsonResponse(response, responseCode);
    }

	/**
	 * Convenience method for creating an error response.
	 *
	 * @param jsonRpc     the jsonrpc string
	 * @param id          the id
	 * @param errorObject the error data (if any)
	 * @return the error response
	 */
    private JsonResponse createResponseError(String jsonRpc, Object id, JsonError errorObject) {
        return createResponse(jsonRpc, id, null, errorObject);
    }

	/**
	 * Creates a success response.
	 *
	 * @param jsonRpc the version string
	 * @param id      the id of the request
	 * @param result  the result object
	 * @return the response object
	 */
    private JsonResponse createResponseSuccess(String jsonRpc, Object id, JsonNode result) {
        return createResponse(jsonRpc, id, result, null);
    }

	/**
	 * Finds the {@link Method} from the supplied {@link Set} that
	 * best matches the rest of the arguments supplied and returns
	 * it as a {@link AMethodWithItsArgs} class.
	 *
	 * @param methods    the {@link Method}s
	 * @param paramsNode the {@link JsonNode} passed as the parameters
	 * @return the {@link AMethodWithItsArgs}
	 */
	private AMethodWithItsArgs findBestMethodByParamsNode(Set<Method> methods, JsonNode paramsNode) {
		if (hasNoParameters(paramsNode)) {
			return findBestMethodUsingParamIndexes(methods, 0, null);
		}
		AMethodWithItsArgs matchedMethod;
		if (paramsNode.isArray()) {
			matchedMethod = findBestMethodUsingParamIndexes(methods, paramsNode.size(), (ArrayNode) paramsNode);
		} else if (paramsNode.isObject()) {
			matchedMethod = findBestMethodUsingParamNames(methods, collectFieldNames(paramsNode), (ObjectNode) paramsNode);
		} else {
			throw new IllegalArgumentException("Unknown params node type: " + paramsNode);
		}
        if (matchedMethod == null) {
            matchedMethod = findBestMethodForVarargs(methods, paramsNode);
		}
		return matchedMethod;
	}
	
	private Set<String> collectFieldNames(JsonNode paramsNode) {
		Set<String> fieldNames = new HashSet<>();
		Iterator<String> itr = paramsNode.fieldNames();
		while (itr.hasNext()) {
			fieldNames.add(itr.next());
		}
		return fieldNames;
	}
	
	private boolean hasNoParameters(JsonNode paramsNode) {
		return isNullNodeOrValue(paramsNode);
	}
	
	/**
	 * Finds the {@link Method} from the supplied {@link Set} that
	 * best matches the rest of the arguments supplied and returns
	 * it as a {@link AMethodWithItsArgs} class.
	 *
	 * @param methods    the {@link Method}s
	 * @param paramCount the number of expect parameters
	 * @param paramNodes the parameters for matching types
	 * @return the {@link AMethodWithItsArgs}
	 */
	private AMethodWithItsArgs findBestMethodUsingParamIndexes(Set<Method> methods, int paramCount, ArrayNode paramNodes) {
		int numParams = isNullNodeOrValue(paramNodes) ? 0 : paramNodes.size();
		int bestParamNumDiff = Integer.MAX_VALUE;
		Set<Method> matchedMethods = collectMethodsMatchingParamCount(methods, paramCount, bestParamNumDiff);
		if (matchedMethods.isEmpty()) {
			return null;
		}
		Method bestMethod = getBestMatchingArgTypeMethod(paramNodes, numParams, matchedMethods);
		return new AMethodWithItsArgs(bestMethod, paramCount, paramNodes);
	}
	
	private Method getBestMatchingArgTypeMethod(ArrayNode paramNodes, int numParams, Set<Method> matchedMethods) {
		if (matchedMethods.size() == 1 || numParams == 0) {
			return matchedMethods.iterator().next();
		}
		Method bestMethod = null;
		int mostMatches = Integer.MIN_VALUE;
		for (Method method : matchedMethods) {
			List<Class<?>> parameterTypes = getParameterTypes(method);
			int numMatches = getNumArgTypeMatches(paramNodes, numParams, parameterTypes);
			if (hasMoreMatches(mostMatches, numMatches)) {
				mostMatches = numMatches;
				bestMethod = method;
			}
		}
		return bestMethod;
	}

    /**
     * Finds the {@link Method} from the supplied {@link Set} that
     * matches the method name annotation and have varargs.
     * it as a {@link AMethodWithItsArgs} class.
     *
     * @param methods    the {@link Method}s
     * @param paramsNode the {@link JsonNode} of request
     * @return the {@link AMethodWithItsArgs}
     */
	private AMethodWithItsArgs findBestMethodForVarargs(Set<Method> methods, JsonNode paramsNode) {
		for (Method method : methods) {
            if (method.getParameterTypes().length != 1) {
                continue;
			}
            if (method.isVarArgs()) {
                return new AMethodWithItsArgs(method, paramsNode);
			}
		}
		return null;
	}
	
	private int getNumArgTypeMatches(ArrayNode paramNodes, int numParams, List<Class<?>> parameterTypes) {
		int numMatches = 0;
		for (int i = 0; i < parameterTypes.size() && i < numParams; i++) {
			if (isMatchingType(paramNodes.get(i), parameterTypes.get(i))) {
				numMatches++;
			}
		}
		return numMatches;
	}
	
	private Set<Method> collectMethodsMatchingParamCount(Set<Method> methods, int paramCount, int bestParamNumDiff) {
		Set<Method> matchedMethods = new HashSet<>();
		// check every method
		for (Method method : methods) {
			Class<?>[] paramTypes = method.getParameterTypes();
			final int paramNumDiff = paramTypes.length - paramCount;
			if (hasLessOrEqualAbsParamDiff(bestParamNumDiff, paramNumDiff) && acceptParamCount(paramNumDiff)) {
				if (hasLessAbsParamDiff(bestParamNumDiff, paramNumDiff)) {
					matchedMethods.clear();
				}
				matchedMethods.add(method);
				bestParamNumDiff = paramNumDiff;
			}
		}
		return matchedMethods;
	}
	
	private boolean hasLessAbsParamDiff(int bestParamNumDiff, int paramNumDiff) {
		return Math.abs(paramNumDiff) < Math.abs(bestParamNumDiff);
	}
	
	private boolean acceptParamCount(int paramNumDiff) {
		return paramNumDiff == 0 || acceptNonExactParam(paramNumDiff);
	}
	
	private boolean acceptNonExactParam(int paramNumDiff) {
		return acceptMoreParam(paramNumDiff) || acceptLessParam(paramNumDiff);
	}
	
	private boolean acceptLessParam(int paramNumDiff) {
		return allowLessParams && paramNumDiff > 0;
	}
	
	private boolean acceptMoreParam(int paramNumDiff) {
		return allowExtraParams && paramNumDiff < 0;
	}
	
	private boolean hasLessOrEqualAbsParamDiff(int bestParamNumDiff, int paramNumDiff) {
		return Math.abs(paramNumDiff) <= Math.abs(bestParamNumDiff);
	}
	
	/**
	 * Finds the {@link Method} from the supplied {@link Set} that best matches the rest of the arguments supplied and
	 * returns it as a {@link AMethodWithItsArgs} class.
	 *
	 * @param methods    the {@link Method}s
	 * @param paramNames the parameter allNames
	 * @param paramNodes the parameters for matching types
	 * @return the {@link AMethodWithItsArgs}
	 */
	private AMethodWithItsArgs findBestMethodUsingParamNames(Set<Method> methods, Set<String> paramNames, ObjectNode paramNodes) {
		ParameterCount max = new ParameterCount();
		
		for (Method method : methods) {
			List<Class<?>> parameterTypes = getParameterTypes(method);
			
			int typeNameCountDiff = parameterTypes.size() - paramNames.size();
			if (!acceptParamCount(typeNameCountDiff)) {
				continue;
			}
			
			ParameterCount parStat = new ParameterCount(paramNames, paramNodes, parameterTypes, method);
			if (!acceptParamCount(parStat.nameCount - paramNames.size())) {
				continue;
			}
			if (hasMoreMatches(max.nameCount, parStat.nameCount) || parStat.nameCount == max.nameCount && hasMoreMatches(max.typeCount, parStat.typeCount)) {
				max = parStat;
			}
		}
		if (max.method == null) {
			return null;
		}
		return new AMethodWithItsArgs(max.method, paramNames, max.allNames, paramNodes);
		
	}
	
	private boolean hasMoreMatches(int maxMatchingParams, int numMatchingParams) {
		return numMatchingParams > maxMatchingParams;
	}
	
	private boolean missingAnnotation(JsonRpcParam name) {
		return name == null;
	}
	
	/**
	 * Determines whether or not the given {@link JsonNode} matches
	 * the given type.  This method is limited to a few java types
	 * only and shouldn't be used to determine with great accuracy
	 * whether or not the types match.
	 *
	 * @param node the {@link JsonNode}
	 * @param type the {@link Class}
	 * @return true if the types match, false otherwise
	 */
	@SuppressWarnings("SimplifiableIfStatement")
	private boolean isMatchingType(JsonNode node, Class<?> type) {
		if (node.isNull()) {
			return true;
		}
		if (node.isTextual()) {
			return String.class.isAssignableFrom(type);
		}
		if (node.isNumber()) {
			return isNumericAssignable(type);
		}
		if (node.isArray() && type.isArray()) {
			return node.size() > 0 && isMatchingType(node.get(0), type.getComponentType());
		}
		if (node.isArray()) {
			return type.isArray() || Collection.class.isAssignableFrom(type);
		}
		if (node.isBinary()) {
			return byteOrCharAssignable(type);
		}
		if (node.isBoolean()) {
			return boolean.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type);
		}
		if (node.isObject() || node.isPojo()) {
			return !type.isPrimitive() && !String.class.isAssignableFrom(type) &&
					!Number.class.isAssignableFrom(type) && !Boolean.class.isAssignableFrom(type);
		}
		return false;
	}
	
	private boolean byteOrCharAssignable(Class<?> type) {
		return byte[].class.isAssignableFrom(type) || Byte[].class.isAssignableFrom(type) ||
				char[].class.isAssignableFrom(type) || Character[].class.isAssignableFrom(type);
	}
	
	private boolean isNumericAssignable(Class<?> type) {
		return Number.class.isAssignableFrom(type) || short.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)
				|| long.class.isAssignableFrom(type) || float.class.isAssignableFrom(type) || double.class.isAssignableFrom(type);
	}
	
	/**
	 * Writes and flushes a value to the given {@link OutputStream}
	 * and prevents Jackson from closing it. Also writes newline.
	 *
	 * @param output the {@link OutputStream}
	 * @param value  the value to write
	 * @throws IOException on error
	 */
	private void writeAndFlushValue(OutputStream output, JsonNode value) throws IOException {
	    if (value == null) {
	        return;
        }
		logger.debug("Response: {}", value);

	    mapper.writeValue(new NoCloseOutputStream(output), value);
		output.write('\n');
	}
	
	private Object parseId(JsonNode node) {
		if (isNullNodeOrValue(node)) {
			return null;
		}
		if (node.isDouble()) {
			return node.asDouble();
		}
		if (node.isFloatingPointNumber()) {
			return node.asDouble();
		}
		if (node.isInt()) {
			return node.asInt();
		}
		if (node.isLong()) {
			return node.asLong();
		}
		//TODO(donequis): consider parsing bigints
		if (node.isIntegralNumber()) {
			return node.asInt();
		}
		if (node.isTextual()) {
			return node.asText();
		}
		throw new IllegalArgumentException("Unknown id type");
	}
	
	private boolean isNullNodeOrValue(JsonNode node) {
		return node == null || node.isNull();
	}
	
	/**
	 * Sets whether or not the server should be backwards
	 * compatible to JSON-RPC 1.0.  This only includes the
	 * omission of the jsonrpc property on the request object,
	 * not the class hinting.
	 *
	 * @param backwardsCompatible the backwardsCompatible to set
	 */
	public void setBackwardsCompatible(boolean backwardsCompatible) {
		this.backwardsCompatible = backwardsCompatible;
	}
	
	/**
	 * Sets whether or not the server should re-throw exceptions.
	 *
	 * @param rethrowExceptions true or false
	 */
	public void setRethrowExceptions(boolean rethrowExceptions) {
		this.rethrowExceptions = rethrowExceptions;
	}
	
	/**
	 * Sets whether or not the server should allow superfluous
	 * parameters to method calls.
	 *
	 * @param allowExtraParams true or false
	 */
	public void setAllowExtraParams(boolean allowExtraParams) {
		this.allowExtraParams = allowExtraParams;
	}
	
	/**
	 * Sets whether or not the server should allow less parameters
	 * than required to method calls (passing null for missing params).
	 *
	 * @param allowLessParams the allowLessParams to set
	 */
	public void setAllowLessParams(boolean allowLessParams) {
		this.allowLessParams = allowLessParams;
	}

	/**
	 * Sets the {@link ErrorResolver} used for resolving errors.
	 * Multiple {@link ErrorResolver}s can be used at once by
	 * using the {@link MultipleErrorResolver}.
	 *
	 * @param errorResolver the errorResolver to set
	 * @see MultipleErrorResolver
	 */
	public void setErrorResolver(ErrorResolver errorResolver) {
		this.errorResolver = errorResolver;
	}
	
	/**
	 * Sets the {@link InvocationListener} instance that can be
	 * used to provide feedback for capturing method-invocation
	 * statistics.
	 *
	 * @param invocationListener is the listener to set
	 */
	
	public void setInvocationListener(InvocationListener invocationListener) {
		this.invocationListener = invocationListener;
	}
	
	/**
	 * Sets the {@link HttpStatusCodeProvider} instance to use for HTTP error results.
	 *
	 * @param httpStatusCodeProvider the status code provider to use for translating JSON-RPC error codes into
	 *                               HTTP status messages.
	 */
	public void setHttpStatusCodeProvider(HttpStatusCodeProvider httpStatusCodeProvider) {
		this.httpStatusCodeProvider = httpStatusCodeProvider;
	}
	
	/**
	 * Sets the {@link ConvertedParameterTransformer} instance that can be
	 * used to mutate the deserialized arguments passed to the service method during invocation.
	 *
	 * @param convertedParameterTransformer the transformer to set
	 */
	public void setConvertedParameterTransformer(ConvertedParameterTransformer convertedParameterTransformer) {
		this.convertedParameterTransformer = convertedParameterTransformer;
	}
	
	/**
	 * If true, then when errors arise in the invocation of JSON-RPC services, the error will be
	 * logged together with the underlying stack trace.  When false, no error will be logged.
	 * An alternative mechanism for logging invocation errors is to employ an implementation of
	 * {@link InvocationListener}.
	 *
	 * @param shouldLogInvocationErrors see method description
	 */
	public void setShouldLogInvocationErrors(boolean shouldLogInvocationErrors) {
		this.shouldLogInvocationErrors = shouldLogInvocationErrors;
	}

    /**
     * Sets the configured {@link ExecutorService} to use it for parallel JSON-RPC batch processing
     *
     * @param batchExecutorService configured {@link ExecutorService}
     */
    public void setBatchExecutorService(ExecutorService batchExecutorService) {
        this.batchExecutorService = batchExecutorService;
    }

    public void setParallelBatchProcessingTimeout(long parallelBatchProcessingTimeout) {
        this.parallelBatchProcessingTimeout = parallelBatchProcessingTimeout;
    }
	
	/**
	 * Simple inner class for the {@code findXXX} methods.
	 */
	private static class AMethodWithItsArgs {
		private final List<JsonNode> arguments = new ArrayList<>();
		private final Method method;
		
		public AMethodWithItsArgs(Method method, int paramCount, ArrayNode paramNodes) {
			this(method);
			collectArgumentsBasedOnCount(method, paramCount, paramNodes);
		}
		
		public AMethodWithItsArgs(Method method) {
			this.method = method;
		}
		
		private void collectArgumentsBasedOnCount(Method method, int paramCount, ArrayNode paramNodes) {
			int numParameters = method.getParameterTypes().length;
			for (int i = 0; i < numParameters; i++) {
				if (i < paramCount) {
					addArgument(paramNodes.get(i));
				} else {
					addArgument(NullNode.getInstance());
				}
			}
		}
		
		public AMethodWithItsArgs(Method method, Set<String> paramNames, List<JsonRpcParam> allNames, ObjectNode paramNodes) {
			this(method);
			collectArgumentsBasedOnName(method, paramNames, allNames, paramNodes);
		}
		
		public AMethodWithItsArgs(Method method, JsonNode jsonNode) {
			this(method);
			collectVarargsFromNode(jsonNode);
		}

		private void collectArgumentsBasedOnName(Method method, Set<String> paramNames, List<JsonRpcParam> allNames, ObjectNode paramNodes) {
			Class<?>[] types = method.getParameterTypes();
			int numParameters = types.length;
			for (int i = 0; i < numParameters; i++) {
				JsonRpcParam param = allNames.get(i);
				if (param != null && paramNames.contains(param.value())) {
					if (types[i].isArray() && method.isVarArgs() && numParameters == 1) {
						collectVarargsFromNode(paramNodes.get(param.value()));
					} else {
						addArgument(paramNodes.get(param.value()));
					}
				} else {
					addArgument(NullNode.getInstance());
				}
			}
		}

		private void collectVarargsFromNode(JsonNode node) {
			if (node.isArray()) {
				ArrayNode arrayNode = (ArrayNode) node;
				for (int i = 0; i < node.size(); i++) {
					addArgument(arrayNode.get(i));
				}
			}

			if (node.isObject()) {
				ObjectNode objectNode = (ObjectNode) node;
				Iterator<Map.Entry<String,JsonNode>> items = objectNode.fields();
				while (items.hasNext()) {
					Map.Entry<String,JsonNode> item = items.next();
					JsonNode name = JsonNodeFactory.instance.objectNode().put(item.getKey(),item.getKey());
					addArgument(name.get(item.getKey()));
					addArgument(item.getValue());
				}
			}
		}

		public void addArgument(JsonNode argumentJsonNode) {
		    arguments.add(argumentJsonNode);
        }
	}
	
	private static class InvokeListenerHandler implements AutoCloseable {
		
		private final long startMs = System.currentTimeMillis();
		private final AMethodWithItsArgs methodArgs;
		private final InvocationListener invocationListener;
		public Throwable error = null;
		public JsonNode result = null;
		
		public InvokeListenerHandler(AMethodWithItsArgs methodArgs, InvocationListener invocationListener) {
			this.methodArgs = methodArgs;
			this.invocationListener = invocationListener;
			if (this.invocationListener != null) {
				this.invocationListener.willInvoke(methodArgs.method, methodArgs.arguments);
			}
		}
		
		@Override
		public void close() {
			if (invocationListener != null) {
				invocationListener.didInvoke(methodArgs.method, methodArgs.arguments, result, error, System.currentTimeMillis() - startMs);
			}
		}
	}
	
	private class ParameterCount {
		private final int typeCount;
		private final int nameCount;
		private final List<JsonRpcParam> allNames;
		private final Method method;
		
		public ParameterCount(Set<String> paramNames, ObjectNode paramNodes, List<Class<?>> parameterTypes, Method method) {
			this.allNames = getAnnotatedParameterNames(method);
			this.method = method;
			int typeCount = 0;
			int nameCount = 0;
			int at = 0;
			
			for (JsonRpcParam name : this.allNames) {
				if (missingAnnotation(name)) {
					continue;
				}
				String paramName = name.value();
				boolean hasParamName = paramNames.contains(paramName);
				if (hasParamName) {
					nameCount += 1;
				}
				if (hasParamName && isMatchingType(paramNodes.get(paramName), parameterTypes.get(at))) {
					typeCount += 1;
				}
				at += 1;
			}
			this.typeCount = typeCount;
			this.nameCount = nameCount;
		}
		
		@SuppressWarnings("Convert2streamapi")
		private List<JsonRpcParam> getAnnotatedParameterNames(Method method) {
			List<JsonRpcParam> parameterNames = new ArrayList<>();
			for (List<? extends Annotation> webParamAnnotation : getWebParameterAnnotations(method)) {
				if (!webParamAnnotation.isEmpty()) {
					parameterNames.add(createNewJsonRcpParamType(webParamAnnotation.get(0)));
				}
			}
			for (List<JsonRpcParam> annotation : getJsonRpcParamAnnotations(method)) {
				if (!annotation.isEmpty()) {
					parameterNames.add(annotation.get(0));
				}
			}
			return parameterNames;
		}
		
		private List<List<? extends Annotation>> getWebParameterAnnotations(Method method) {
			List<List<? extends Annotation>> annotations = new ArrayList<>();
			for (Class<? extends Annotation> clazz : JsonRpcBasicServer.this.webParamAnnotationClasses) {
				annotations.addAll(
					ReflectionUtil.getParameterAnnotations(method, clazz)
				);
			}
			return annotations;
		}
		
		private JsonRpcParam createNewJsonRcpParamType(final Annotation annotation) {
			return new JsonRpcParam() {
				public Class<? extends Annotation> annotationType() {
					return JsonRpcParam.class;
				}
				
				public String value() {
					try {
						Method method = annotation.getClass().getMethod(NAME);
						return (String) method.invoke(annotation);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};
		}
		
		private List<List<JsonRpcParam>> getJsonRpcParamAnnotations(Method method) {
			return ReflectionUtil.getParameterAnnotations(method, JsonRpcParam.class);
		}
		
		public ParameterCount() {
			typeCount = -1;
			nameCount = -1;
			allNames = null;
			method = null;
		}
	}

	/**
	 * Gets the {@link RequestInterceptor} instance.
	 *
	 * @return previously set request interceptor object instance. There are no interceptors
	 * by default, and this method returns {@code null}, if interceptor was not previously set.
	 */
	public RequestInterceptor getRequestInterceptor() {
		return requestInterceptor;
	}

	/**
	 * Sets the {@link RequestInterceptor} instance
	 *
	 * @param requestInterceptor interceptor object instance,
	 *                           which will be invoked prior to any JSON-RPC service being invoked.
	 */
	public void setRequestInterceptor(RequestInterceptor requestInterceptor) {
		this.requestInterceptor = requestInterceptor;
	}

	/**
	 * Gets the collection of {@link JsonRpcInterceptor} instances.
	 *
	 * @return collection of previously set {@link JsonRpcInterceptor} instances.
	 * There are no interceptors by default, and this method returns empty collection, if
	 * interceptors were not previously set.
	 */
	public List<JsonRpcInterceptor> getInterceptorList() {
		return interceptorList;
	}

	/**
	 * Sets the collection of {@link JsonRpcInterceptor} instances.
	 *
	 * @param interceptorList collection of {@link JsonRpcInterceptor} instances, which are
	 *                        called at different stages of request handling.
	 *                        Parameter cannot be {@code null}.
	 */
	public void setInterceptorList(List<JsonRpcInterceptor> interceptorList) {
		if (interceptorList == null) {
			throw new IllegalArgumentException("Interceptors list can't be null");
		}
		this.interceptorList = interceptorList;
	}
  
  private static class ParameterConvertException extends RuntimeException {
		private final int paramIndex;

		private ParameterConvertException(int index, Throwable throwable) {
			super(throwable);
			this.paramIndex = index;
		}
	}
}
