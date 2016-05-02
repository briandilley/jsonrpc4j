package com.googlecode.jsonrpc4j;

import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.ERROR_NOT_HANDLED;
import static com.googlecode.jsonrpc4j.ReflectionUtil.findCandidateMethods;
import static com.googlecode.jsonrpc4j.ReflectionUtil.getParameterTypes;
import static com.googlecode.jsonrpc4j.Util.hasNonNullData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jsonrpc4j.ErrorResolver.JsonError;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import net.iharder.Base64;

/**
 * A JSON-RPC request server reads JSON-RPC requests from an input stream and writes responses to an output stream.
 * Can even run on Android system.
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public class JsonRpcBasicServer {

	private static Pattern BASE64_PATTERN = Pattern.compile("[A-Za-z0-9_=-]+");

	public static final String JSONRPC_CONTENT_TYPE = "application/json-rpc";

	public static final String PARAMS = "params";
	public static final String METHOD = "method";
	public static final String JSONRPC = "jsonrpc";
	public static final String ID = "id";

	public static final String ERROR = "error";
	public static final String ERROR_MESSAGE = "message";
	public static final String ERROR_CODE = "code";
	public static final String DATA = "data";
	public static final String RESULT = "result";
	public static final String EXCEPTION_TYPE_NAME = "exceptionTypeName";

	public static final String VERSION = "2.0";

	public static final int CODE_OK = 0;
	public static final String WEB_PARAM_ANNOTATION_CLASS_LOADER = "javax.jws.WebParam";
	public static final String NAME = "name";
	public static final String NULL = "null";
	private static final Logger logger = LoggerFactory.getLogger(JsonRpcBasicServer.class);
	private static final ErrorResolver DEFAULT_ERROR_RESOLVER = new MultipleErrorResolver(AnnotationsErrorResolver.INSTANCE, DefaultErrorResolver.INSTANCE);
	private static Class<? extends Annotation> WEB_PARAM_ANNOTATION_CLASS;
	private static Method WEB_PARAM_NAME_METHOD;

	static {
		loadAnnotationSupportEngine();
	}

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
		if (handler != null) logger.debug("created server for interface {} with handler {}", remoteInterface, handler.getClass());
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

	private static void loadAnnotationSupportEngine() {
		final ClassLoader classLoader = JsonRpcBasicServer.class.getClassLoader();
		try {
			WEB_PARAM_ANNOTATION_CLASS = classLoader.loadClass(WEB_PARAM_ANNOTATION_CLASS_LOADER).asSubclass(Annotation.class);
			WEB_PARAM_NAME_METHOD = WEB_PARAM_ANNOTATION_CLASS.getMethod(NAME);
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			logger.error("Could not find {}.{}", WEB_PARAM_ANNOTATION_CLASS_LOADER, NAME, e);
		}
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

		if (null!=id && 0!=id.length()) {
			envelope.append(id);
		}
		else {
			envelope.append("null");
		}

		envelope.append(",\"");
		envelope.append(METHOD);
		envelope.append("\":");

		if (null!=method && 0!=method.length()) {
			envelope.append('"');
			envelope.append(method);
			envelope.append('"');
		}
		else {
			envelope.append("null");
		}
		envelope.append(",\"");
		envelope.append(PARAMS);
		envelope.append("\":");

		if (null!=params && 0!=params.length()) {
			String decodedParams;

			// some specifications suggest that the GET "params" query parameter should be Base64 encoded and
			// some suggest not.  Try to deal with both scenarios -- the code here was previously only doing
			// Base64 decoding.
			// http://www.simple-is-better.org/json-rpc/transport_http.html
			// http://www.jsonrpc.org/historical/json-rpc-over-http.html#encoded-parameters

			if(BASE64_PATTERN.matcher(params).matches()) {
				decodedParams = new String(Base64.decode(params), StandardCharsets.UTF_8);
			}
			else {
				switch(params.charAt(0)) {
					case '[':
					case '{':
						decodedParams = params;
						break;

					default:
						throw new IOException("badly formed 'param' parameter starting with; [" + params.charAt(0) + "]");
				}
			}

			envelope.append(decodedParams);
		}
		else {
			envelope.append("[]");
		}

		envelope.append('}');

		return new ByteArrayInputStream(envelope.toString().getBytes(StandardCharsets.UTF_8));
	}

	public RequestInterceptor getRequestInterceptor() {
		return requestInterceptor;
	}

	public void setRequestInterceptor(RequestInterceptor requestInterceptor) {
		this.requestInterceptor = requestInterceptor;
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
			return handleJsonNodeRequest(jsonNode, output).code;
		} catch (JsonParseException e) {
			return writeAndFlushValueError(output, createResponseError(JSONRPC, NULL, JsonError.PARSE_ERROR)).code;
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
			return new Class<?>[] { remoteInterface };
		} else if (Proxy.isProxyClass(handler.getClass())) {
			return handler.getClass().getInterfaces();
		} else {
			return new Class<?>[] { handler.getClass() };
		}
	}

	/**
	 * Handles the given {@link JsonNode} and writes the responses to the given {@link OutputStream}.
	 *
	 * @param node   the {@link JsonNode}
	 * @param output the {@link OutputStream}
	 * @return the error code, or {@code 0} if none
	 * @throws IOException on error
	 */
	protected JsonError handleJsonNodeRequest(final JsonNode node, final OutputStream output) throws IOException {
		if (node.isArray()) return handleArray(ArrayNode.class.cast(node), output);
		if (node.isObject()) return handleObject(ObjectNode.class.cast(node), output);
		return this.writeAndFlushValueError(output, this.createResponseError(VERSION, NULL, JsonError.INVALID_REQUEST));
	}

	/**
	 * Handles the given {@link ArrayNode} and writes the
	 * responses to the given {@link OutputStream}.
	 *
	 * @param node   the {@link JsonNode}
	 * @param output the {@link OutputStream}
	 * @return the error code, or {@code 0} if none
	 * @throws IOException on error
	 */
	private JsonError handleArray(ArrayNode node, OutputStream output) throws IOException {
		logger.debug("Handling {} requests", node.size());
		JsonError result = JsonError.OK;
		output.write('[');
		int errorCount = 0;
		for (int i = 0; i < node.size(); i++) {
			JsonError nodeResult = handleJsonNodeRequest(node.get(i), output);
			if (isError(nodeResult)) {
				result = JsonError.BULK_ERROR;
				errorCount += 1;
			}
			if (i != node.size() - 1) output.write(',');
		}
		output.write(']');
		logger.debug("served {} requests, error {}, result {}", node.size(), errorCount, result);
		// noinspection unchecked
		return result;
	}

	private boolean isError(JsonError result) {
		return result.code != JsonError.OK.code;
	}

	/**
	 * Handles the given {@link ObjectNode} and writes the
	 * responses to the given {@link OutputStream}.
	 *
	 * @param node   the {@link JsonNode}
	 * @param output the {@link OutputStream}
	 * @return the error code, or {@code 0} if none
	 * @throws IOException on error
	 */
	private JsonError handleObject(final ObjectNode node, final OutputStream output) throws IOException {
		logger.debug("Request: {}", node);

		if (!isValidRequest(node))
			return writeAndFlushValueError(output, createResponseError(VERSION, NULL, JsonError.INVALID_REQUEST));
		Object id = parseId(node.get(ID));

		String jsonRpc = hasNonNullData(node, JSONRPC) ? node.get(JSONRPC).asText() : VERSION;
		if (!hasNonNullData(node, METHOD))
			return writeAndFlushValueError(output, createResponseError(jsonRpc, id, JsonError.METHOD_NOT_FOUND));

		final String fullMethodName = node.get(METHOD).asText();
		final String partialMethodName = getMethodName(fullMethodName);
		final String serviceName = getServiceName(fullMethodName);

		Set<Method> methods = findCandidateMethods(getHandlerInterfaces(serviceName), partialMethodName);
		if (methods.isEmpty())
			return writeAndFlushValueError(output, createResponseError(jsonRpc, id, JsonError.METHOD_NOT_FOUND));
		AMethodWithItsArgs methodArgs = findBestMethodByParamsNode(methods, node.get(PARAMS));
		if (methodArgs == null) return writeAndFlushValueError(output, createResponseError(jsonRpc, id, JsonError.METHOD_PARAMS_INVALID));
		try (InvokeListenerHandler handler = new InvokeListenerHandler(methodArgs, invocationListener)) {
			try {
				if (this.requestInterceptor != null) this.requestInterceptor.interceptRequest(node);
				handler.result = invoke(getHandler(serviceName), methodArgs.method, methodArgs.arguments);
				if (!isNotificationRequest(id)) {
					ObjectNode response = createResponseSuccess(jsonRpc, id, handler.result);
					writeAndFlushValue(output, response);
				}
				return JsonError.OK;
			} catch (Throwable e) {
				handler.error = e;
				return handleError(output, id, jsonRpc, methodArgs, e);
			}
		}
	}

	private JsonError handleError(OutputStream output, Object id, String jsonRpc, AMethodWithItsArgs methodArgs, Throwable e) throws IOException {
		Throwable unwrappedException = getException(e);
		logger.warn("Error in JSON-RPC Service", unwrappedException);
		JsonError error = resolveError(methodArgs, unwrappedException);
		writeAndFlushValueError(output, createResponseError(jsonRpc, id, error));
		if (rethrowExceptions) { throw new RuntimeException(unwrappedException); }
		return error;
	}

	private Throwable getException(final Throwable thrown) {
		Throwable e = thrown;
		while (InvocationTargetException.class.isInstance(e)) {
			// noinspection ThrowableResultOfMethodCallIgnored
			e = InvocationTargetException.class.cast(e).getTargetException();
			while (UndeclaredThrowableException.class.isInstance(e)) {
				// noinspection ThrowableResultOfMethodCallIgnored
				e = UndeclaredThrowableException.class.cast(e).getUndeclaredThrowable();
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
		return !backwardsCompatible || hasMethodAndVersion(node);
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
	 * @param method      the method to invoke
	 * @param params the params to pass to the method
	 * @return the return value (or null if no return)
	 * @throws IOException               on error
	 * @throws IllegalAccessException    on error
	 * @throws InvocationTargetException on error
	 */
	private JsonNode invoke(Object target, Method method, List<JsonNode> params) throws IOException, IllegalAccessException, InvocationTargetException {
		logger.debug("Invoking method: {} with args {}", method.getName(), params);
		Object[] convertedParams = convertJsonToParameters(method, params);
		if (convertedParameterTransformer != null) {
			convertedParams = convertedParameterTransformer.transformConvertedParameters(target, convertedParams);
		}
		Object result = method.invoke(target, convertedParams);
		logger.debug("Invoked method: {}, result {}", method.getName(), result);
		return hasReturnValue(method) ? mapper.valueToTree(result) : null;
	}

	private boolean hasReturnValue(Method m) {
		return m.getGenericReturnType() != null;
	}

	private Object[] convertJsonToParameters(Method m, List<JsonNode> params) throws IOException {
		Object[] convertedParams = new Object[params.size()];
		Type[] parameterTypes = m.getGenericParameterTypes();

		for (int i = 0; i < parameterTypes.length; i++) {
			JsonParser paramJsonParser = mapper.treeAsTokens(params.get(i));
			JavaType paramJavaType = mapper.getTypeFactory().constructType(parameterTypes[i]);
			convertedParams[i] = mapper.readValue(paramJsonParser, paramJavaType);
		}
		return convertedParams;
	}

	/**
	 * Convenience method for creating an error response.
	 *
	 * @param jsonRpc the jsonrpc string
	 * @param id      the id
	 * @param errorObject    the error data (if any)
	 * @return the error response
	 */
	private ErrorObjectWithJsonError createResponseError(String jsonRpc, Object id, JsonError errorObject) {
		ObjectNode response = mapper.createObjectNode();
		ObjectNode error = mapper.createObjectNode();
		error.put(ERROR_CODE, errorObject.code);
		error.put(ERROR_MESSAGE, errorObject.message);
		if (errorObject.data != null) {
			error.set(DATA, mapper.valueToTree(errorObject.data));
		}
		response.put(JSONRPC, jsonRpc);
		if (Integer.class.isInstance(id)) {
			response.put(ID, Integer.class.cast(id).intValue());
		} else if (Long.class.isInstance(id)) {
			response.put(ID, Long.class.cast(id).longValue());
		} else if (Float.class.isInstance(id)) {
			response.put(ID, Float.class.cast(id).floatValue());
		} else if (Double.class.isInstance(id)) {
			response.put(ID, Double.class.cast(id).doubleValue());
		} else if (BigDecimal.class.isInstance(id)) {
			response.put(ID, BigDecimal.class.cast(id));
		} else {
			response.put(ID, String.class.cast(id));
		}
		response.set(ERROR, error);
		return new ErrorObjectWithJsonError(response, errorObject);
	}

	/**
	 * Creates a success response.
	 *
	 * @param jsonRpc the version string
	 * @param id the id of the request
	 * @param result the result object
	 * @return the response object
	 */
	private ObjectNode createResponseSuccess(String jsonRpc, Object id, JsonNode result) {
		ObjectNode response = mapper.createObjectNode();
		response.put(JSONRPC, jsonRpc);
		if (Integer.class.isInstance(id)) {
			response.put(ID, Integer.class.cast(id).intValue());
		} else if (Long.class.isInstance(id)) {
			response.put(ID, Long.class.cast(id).longValue());
		} else if (Float.class.isInstance(id)) {
			response.put(ID, Float.class.cast(id).floatValue());
		} else if (Double.class.isInstance(id)) {
			response.put(ID, Double.class.cast(id).doubleValue());
		} else if (BigDecimal.class.isInstance(id)) {
			response.put(ID, BigDecimal.class.cast(id));
		} else {
			response.put(ID, String.class.cast(id));
		}
		response.set(RESULT, result);
		return response;
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
		if (hasNoParameters(paramsNode)) return findBestMethodUsingParamIndexes(methods, 0, null);
		if (paramsNode.isArray()) return findBestMethodUsingParamIndexes(methods, paramsNode.size(), ArrayNode.class.cast(paramsNode));
		if (paramsNode.isObject()) return findBestMethodUsingParamNames(methods, collectFieldNames(paramsNode), ObjectNode.class.cast(paramsNode));
		throw new IllegalArgumentException("Unknown params node type: " + paramsNode.toString());
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
		if (matchedMethods.isEmpty()) return null;
		Method bestMethod = getBestMatchingArgTypeMethod(paramNodes, numParams, matchedMethods);
		return new AMethodWithItsArgs(bestMethod, paramCount, paramNodes);
	}

	private Method getBestMatchingArgTypeMethod(ArrayNode paramNodes, int numParams, Set<Method> matchedMethods) {
		if (matchedMethods.size() == 1 || numParams == 0) return matchedMethods.iterator().next();
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
				if (hasLessAbsParamDiff(bestParamNumDiff, paramNumDiff)) matchedMethods.clear();
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
	 * @param methods the {@link Method}s
	 * @param paramNames the parameter allNames
	 * @param paramNodes the parameters for matching types
	 * @return the {@link AMethodWithItsArgs}
	 */
	private AMethodWithItsArgs findBestMethodUsingParamNames(Set<Method> methods, Set<String> paramNames, ObjectNode paramNodes) {
		ParameterCount max = new ParameterCount();

		for (Method method : methods) {
			List<Class<?>> parameterTypes = getParameterTypes(method);

			int typeNameCountDiff = parameterTypes.size() - paramNames.size();
			if (!acceptParamCount(typeNameCountDiff)) continue;

			ParameterCount parStat = new ParameterCount(paramNames, paramNodes, parameterTypes, method);
			if (!acceptParamCount(parStat.nameCount - paramNames.size())) continue;
			if (hasMoreMatches(max.nameCount, parStat.nameCount) || parStat.nameCount == max.nameCount && hasMoreMatches(max.typeCount, parStat.typeCount)) max = parStat;
		}
		if (max.method == null) return null;
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
		if (node.isNull()) return true;
		if (node.isTextual()) return String.class.isAssignableFrom(type);
		if (node.isNumber()) return isNumericAssignable(type);
		if (node.isArray() && type.isArray()) return node.size() > 0 && isMatchingType(node.get(0), type.getComponentType());
		if (node.isArray()) return type.isArray() || Collection.class.isAssignableFrom(type);
		if (node.isBinary()) return byteOrCharAssignable(type);
		if (node.isBoolean()) return boolean.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type);
		if (node.isObject() || node.isPojo()) { return !type.isPrimitive() && !String.class.isAssignableFrom(type) &&
				!Number.class.isAssignableFrom(type) && !Boolean.class.isAssignableFrom(type); }
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

	private JsonError writeAndFlushValueError(OutputStream output, ErrorObjectWithJsonError value) throws IOException {
		logger.warn("failed " + value);
		writeAndFlushValue(output, value.node);
		return value.error;
	}

	/**
	 * Writes and flushes a value to the given {@link OutputStream}
	 * and prevents Jackson from closing it. Also writes newline.
	 *
	 * @param output the {@link OutputStream}
	 * @param value  the value to write
	 * @throws IOException on error
	 */
	private void writeAndFlushValue(OutputStream output, Object value) throws IOException {
		logger.debug("Response: {}", value);
		mapper.writeValue(new NoCloseOutputStream(output), value);
		output.write('\n');
	}

	private Object parseId(JsonNode node) {
		if (isNullNodeOrValue(node)) return null;
		if (node.isDouble()) return node.asDouble();
		if (node.isFloatingPointNumber()) return node.asDouble();
		if (node.isInt()) return node.asInt();
		if (node.isIntegralNumber()) return node.asInt();
		if (node.isLong()) return node.asLong();
		if (node.isTextual()) return node.asText();
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

	private static class ErrorObjectWithJsonError {
		private final ObjectNode node;
		private final JsonError error;

		public ErrorObjectWithJsonError(ObjectNode node, JsonError error) {
			this.node = node;
			this.error = error;
		}

		@Override
		public String toString() {
			return "ErrorObjectWithJsonError{" +
					"node=" + node +
					", error=" + error +
					'}';
		}
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
					arguments.add(paramNodes.get(i));
				} else {
					arguments.add(NullNode.getInstance());
				}
			}
		}

		public AMethodWithItsArgs(Method method, Set<String> paramNames, List<JsonRpcParam> allNames, ObjectNode paramNodes) {
			this(method);
			collectArgumentsBasedOnName(method, paramNames, allNames, paramNodes);
		}

		private void collectArgumentsBasedOnName(Method method, Set<String> paramNames, List<JsonRpcParam> allNames, ObjectNode paramNodes) {
			int numParameters = method.getParameterTypes().length;
			for (int i = 0; i < numParameters; i++) {
				JsonRpcParam param = allNames.get(i);
				if (param != null && paramNames.contains(param.value())) {
					arguments.add(paramNodes.get(param.value()));
				} else {
					arguments.add(NullNode.getInstance());
				}
			}
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
				if (missingAnnotation(name)) continue;
				String paramName = name.value();
				boolean hasParamName = paramNames.contains(paramName);
				if (hasParamName) nameCount += 1;
				if (hasParamName && isMatchingType(paramNodes.get(paramName), parameterTypes.get(at))) typeCount += 1;
				at += 1;
			}
			this.typeCount = typeCount;
			this.nameCount = nameCount;
		}

		@SuppressWarnings("Convert2streamapi")
		private List<JsonRpcParam> getAnnotatedParameterNames(Method method) {
			List<JsonRpcParam> parameterNames = new ArrayList<>();
			for (List<? extends Annotation> webParamAnnotation : getWebParameterAnnotations(method)) {
				if (!webParamAnnotation.isEmpty()) parameterNames.add(createNewJsonRcpParamType(webParamAnnotation.get(0)));
			}
			for (List<JsonRpcParam> annotation : getJsonRpcParamAnnotations(method)) {
				if (!annotation.isEmpty()) parameterNames.add(annotation.get(0));
			}
			return parameterNames;
		}

		private List<? extends List<? extends Annotation>> getWebParameterAnnotations(Method method) {
			if (WEB_PARAM_ANNOTATION_CLASS == null) return new ArrayList<>();
			return ReflectionUtil.getParameterAnnotations(method, WEB_PARAM_ANNOTATION_CLASS);
		}

		private JsonRpcParam createNewJsonRcpParamType(final Annotation annotation) {
			return new JsonRpcParam() {
				public Class<? extends Annotation> annotationType() {
					return JsonRpcParam.class;
				}

				public String value() {
					try {
						return (String) WEB_PARAM_NAME_METHOD.invoke(annotation);
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

		public int getTypeCount() {
			return typeCount;
		}

		public int getNameCount() {
			return nameCount;
		}

	}
}
