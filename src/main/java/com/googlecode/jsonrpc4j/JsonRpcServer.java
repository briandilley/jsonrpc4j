package com.googlecode.jsonrpc4j;

import static com.googlecode.jsonrpc4j.ReflectionUtil.findMethods;
import static com.googlecode.jsonrpc4j.ReflectionUtil.getParameterAnnotations;
import static com.googlecode.jsonrpc4j.ReflectionUtil.getParameterTypes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.googlecode.jsonrpc4j.ErrorResolver.JsonError;

/**
 * A JSON-RPC request server reads JSON-RPC requests from an
 * input stream and writes responses to an output stream.
 */
public class JsonRpcServer {

	private static final Logger LOGGER = Logger.getLogger(JsonRpcServer.class.getName());

	public static final String JSONRPC_RESPONSE_CONTENT_TYPE = "application/json-rpc";

	public static final ErrorResolver DEFAULT_ERRROR_RESOLVER
		= new MultipleErrorResolver(AnnotationsErrorResolver.INSTANCE, DefaultErrorResolver.INSTANCE);

	private static Class<?> WEBPARAM_ANNOTATION_CLASS;
	private static Method WEBPARAM_NAME_METHOD;

	private boolean backwardsComaptible		= true;
	private boolean rethrowExceptions 		= false;
	private boolean allowExtraParams 		= false;
	private boolean allowLessParams			= false;
	private ErrorResolver errorResolver	= null;
	private ObjectMapper mapper;
	private Object handler;
	private Class<?> remoteInterface;
	private Level exceptionLogLevel = Level.WARNING;

	static {
		ClassLoader classLoader = JsonRpcServer.class.getClassLoader();
		try {
			WEBPARAM_ANNOTATION_CLASS = classLoader.loadClass("javax.jws.WebParam");
			WEBPARAM_NAME_METHOD  = WEBPARAM_ANNOTATION_CLASS.getMethod("name");
		} catch (Exception e) {
			// Must be Java 1.5
		}
	}

	/**
	 * Creates the server with the given {@link ObjectMapper} delegating
	 * all calls to the given {@code handler} {@link Object} but only
	 * methods available on the {@code remoteInterface}.
	 *
	 * @param mapper the {@link ObjectMapper}
	 * @param handler the {@code handler}
	 * @param remoteInterface the interface
	 */
	public JsonRpcServer(
		ObjectMapper mapper, Object handler, Class<?> remoteInterface) {
		this.mapper				= mapper;
		this.handler 			= handler;
		this.remoteInterface	= remoteInterface;
	}

	/**
	 * Creates the server with the given {@link ObjectMapper} delegating
	 * all calls to the given {@code handler}.
	 *
	 * @param mapper the {@link ObjectMapper}
	 * @param handler the {@code handler}
	 */
	public JsonRpcServer(ObjectMapper mapper, Object handler) {
		this(mapper, handler, null);
	}

	/**
	 * Creates the server with a default {@link ObjectMapper} delegating
	 * all calls to the given {@code handler} {@link Object} but only
	 * methods available on the {@code remoteInterface}.
	 *
	 * @param handler the {@code handler}
	 * @param remoteInterface the interface
	 */
	public JsonRpcServer(Object handler, Class<?> remoteInterface) {
		this(new ObjectMapper(), handler, null);
	}

	/**
	 * Creates the server with a default {@link ObjectMapper} delegating
	 * all calls to the given {@code handler}.
	 *
	 * @param handler the {@code handler}
	 */
	public JsonRpcServer(Object handler) {
		this(new ObjectMapper(), handler, null);
	}

	/**
	 * Handles a portlet request.
	 *
	 * @param request the {@link ResourceRequest}
	 * @param response the {@link ResourceResponse}
	 * @throws IOException on error
	 */
	public void handle(ResourceRequest request, ResourceResponse response)
		throws IOException {
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.log(Level.FINE, "Handing ResourceRequest "+request.getMethod());
		}

		// set response type
		response.setContentType(JSONRPC_RESPONSE_CONTENT_TYPE);

		// setup streams
		InputStream input 	= null;
		OutputStream output	= response.getPortletOutputStream();

		// POST
		if (request.getMethod().equals("POST")) {
			input = request.getPortletInputStream();

		// GET
		} else if (request.getMethod().equals("GET")) {
			input = createInputStream(
				request.getParameter("method"),
				request.getParameter("id"),
				request.getParameter("params"));

		// invalid request
		} else {
			throw new IOException(
				"Invalid request method, only POST and GET is supported");
		}

		// service the request
		handle(input, output);
	}

	/**
	 * Handles a servlet request.
	 *
	 * @param request the {@link HttpServletRequest}
	 * @param response the {@link HttpServletResponse}
	 * @throws IOException on error
	 */
	public void handle(HttpServletRequest request, HttpServletResponse response)
		throws IOException {
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.log(Level.FINE, "Handing HttpServletRequest "+request.getMethod());
		}

		// set response type
		response.setContentType(JSONRPC_RESPONSE_CONTENT_TYPE);

		// setup streams
		InputStream input 	= null;
		OutputStream output	= response.getOutputStream();

		// POST
		if (request.getMethod().equals("POST")) {
			input = request.getInputStream();

		// GET
		} else if (request.getMethod().equals("GET")) {
			input = createInputStream(
				request.getParameter("method"),
				request.getParameter("id"),
				request.getParameter("params"));

		// invalid request
		} else {
			throw new IOException(
				"Invalid request method, only POST and GET is supported");
		}

		// service the request
		handle(input, output);
	}

	/**
	 * Handles a single request from the given {@link InputStream},
	 * that is to say that a single {@link JsonNode} is read from
	 * the stream and treated as a JSON-RPC request.  All responses
	 * are written to the given {@link OutputStream}.
	 *
	 * @param ips the {@link InputStream}
	 * @param ops the {@link OutputStream}
	 * @throws IOException on error
	 */
	public void handle(InputStream ips, OutputStream ops)
		throws IOException {
		JsonNode jsonNode = null;
		try {
			jsonNode = mapper.readTree(new NoCloseInputStream(ips));
		} catch (JsonParseException e) {
			writeAndFlushValue(ops, createErrorResponse(
				"jsonrpc", "null", -32700, "Parse error", null));
			return;
		}
		handleNode(jsonNode, ops);
	}

	/**
	 * Returns parameters into an {@link InputStream} of JSON data.
	 *
	 * @param method the method
	 * @param id the id
	 * @param params the base64 encoded params
	 * @return the {@link InputStream}
	 * @throws IOException on error
	 */
	protected static InputStream createInputStream(String method, String id, String params)
		throws IOException {

		// decode parameters
		String decodedParams = URLDecoder.decode(
			new String(Base64.decode(params)), "UTF-8");

		// create request
		String request = new StringBuilder()
			.append("{ ")
			.append("\"id\": \"").append(id).append("\", ")
			.append("\"method\": \"").append(method).append("\", ")
			.append("\"params\": ").append(decodedParams).append(" ")
			.append("}")
			.toString();

		// turn into InputStream
		return new ByteArrayInputStream(request.getBytes());
	}

	/**
	 * Returns the handler's class or interfaces.  The variable serviceName
	 * is ignored in this class.
	 *
	 * @param serviceName the optional name of a service
	 * @return the class
	 */
	protected Class<?>[] getHandlerInterfaces(String serviceName) {
		if (remoteInterface != null) {
			return new Class<?>[] {remoteInterface};
		} else if (Proxy.isProxyClass(handler.getClass())) {
			return handler.getClass().getInterfaces();
		} else {
			return new Class<?>[] {handler.getClass()};
		}
	}

	
	/**
	 * Handles the given {@link JsonNode} and writes the
	 * responses to the given {@link OutputStream}.
	 *
	 * @param node the {@link JsonNode}
	 * @param ops the {@link OutputStream}
	 * @throws IOException on error
	 */
	public void handleNode(JsonNode node, OutputStream ops)
		throws IOException {

		// handle objects
		if (node.isObject()) {
			handleObject(ObjectNode.class.cast(node), ops);

		// handle arrays
		} else if (node.isArray()) {
			handleArray(ArrayNode.class.cast(node), ops);

		// bail on bad data
		} else {
			this.writeAndFlushValue(
				ops, this.createErrorResponse(
				"2.0", "null", -32600, "Invalid Request", null));
		}
	}

	/**
	 * Handles the given {@link ArrayNode} and writes the
	 * responses to the given {@link OutputStream}.
	 *
	 * @param node the {@link JsonNode}
	 * @param ops the {@link OutputStream}
	 * @throws IOException on error
	 */
	public void handleArray(ArrayNode node, OutputStream ops)
		throws IOException {
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.log(Level.FINE, "Handing "+node.size()+" requests");
		}

		// loop through each array element
		ops.write('[');
		for (int i=0; i<node.size(); i++) {
			handleNode(node.get(i), ops);
			if (i != node.size() - 1) ops.write(','); 
		}
		ops.write(']');
	}

	/**
	 * Handles the given {@link ObjectNode} and writes the
	 * responses to the given {@link OutputStream}.
	 *
	 * @param node the {@link JsonNode}
	 * @param ops the {@link OutputStream}
	 * @throws IOException on error
	 */
	public void handleObject(ObjectNode node, OutputStream ops)
		throws IOException {
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.log(Level.FINE, "Request: "+node.toString());
		}

		// validate request
		if (!backwardsComaptible && !node.has("jsonrpc") || !node.has("method")) {
			writeAndFlushValue(ops, createErrorResponse(
				"2.0", "null", -32600, "Invalid Request", null));
			return;
		}

		// get nodes
		JsonNode jsonPrcNode	= node.get("jsonrpc");
		JsonNode methodNode		= node.get("method");
		JsonNode idNode 		= node.get("id");
		JsonNode paramsNode		= node.get("params");

		// get node values
		String jsonRpc		= (jsonPrcNode!=null && !jsonPrcNode.isNull()) ? jsonPrcNode.asText() : "2.0";
		String methodName	= getMethodName(methodNode);
		String serviceName  = getServiceName(methodNode);
		Object id			= parseId(idNode);

		// find methods
		Set<Method> methods = new HashSet<Method>();
		methods.addAll(findMethods(getHandlerInterfaces(serviceName), methodName));
		if (methods.isEmpty()) {
			writeAndFlushValue(ops, createErrorResponse(
				jsonRpc, id, -32601, "Method not found", null));
			return;
		}

		// choose a method
		MethodAndArgs methodArgs = findBestMethodByParamsNode(methods, paramsNode);
		if (methodArgs==null) {
			writeAndFlushValue(ops, createErrorResponse(
				jsonRpc, id, -32602, "Invalid method parameters", null));
			return;
		}

		// invoke the method
		JsonNode result = null;
		Throwable thrown = null;
		try {
			result = invoke(getHandler(serviceName), methodArgs.method, methodArgs.arguments);
		} catch (Throwable e) {
			thrown = e;
		}

		// respond if it's not a notification request
		if (id!=null) {

			// attempt to resolve the error
			JsonError error = null;
			if (thrown!=null) {

				// get cause of exception
				Throwable e = thrown;
				if (InvocationTargetException.class.isInstance(e)) {
					e = InvocationTargetException.class.cast(e).getTargetException();
				}

				// resolve error
				if (errorResolver!=null) {
					error = errorResolver.resolveError(
						e, methodArgs.method, methodArgs.arguments);
				} else {
					error = DEFAULT_ERRROR_RESOLVER.resolveError(
						e, methodArgs.method, methodArgs.arguments);
				}

				// make sure we have a JsonError
				if (error==null) {
					error = new JsonError(
						0, e.getMessage(), e.getClass().getName());
				}
			}

			// the resoponse object
			ObjectNode response = null;

			// build error
			if (error!=null) {
				response = createErrorResponse(
					jsonRpc, id, error.getCode(), error.getMessage(), error.getData());

			// build success
			} else {
				response = createSuccessResponse(jsonRpc, id, result);
			}

			// write it
			writeAndFlushValue(ops, response);
		}

		// log and potentially re-throw errors
		if (thrown!=null) {
			if (LOGGER.isLoggable(exceptionLogLevel)) {
				LOGGER.log(exceptionLogLevel, "Error in JSON-RPC Service", thrown);
			}
			if (rethrowExceptions) {
				throw new RuntimeException(thrown);
			}
		}
	}

	/**
	 * Get the service name from the methodNode.  In this class, it is always
	 * <code>null</code>.  Subclasses may parse the methodNode for service name.
	 *
	 * @param methodNode the JsonNode for the method
	 * @return the name of the service, or <code>null</code>
	 */
	protected String getServiceName(JsonNode methodNode) {
		return null;
	}

	/**
	 * Get the method name from the methodNode.
	 *
	 * @param methodNode the JsonNode for the method
	 * @return the name of the method that should be invoked
	 */
	protected String getMethodName(JsonNode methodNode) {
		return (methodNode!=null && !methodNode.isNull()) ? methodNode.asText() : null;
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
	 * @param an optional service name used to locate the target object
	 *  to invoke the Method on
	 * @param m the method to invoke
	 * @param params the params to pass to the method
	 * @return the return value (or null if no return)
	 * @throws IOException on error
	 * @throws IllegalAccessException on error
	 * @throws InvocationTargetException on error
	 */
	protected JsonNode invoke(Object target, Method m, List<JsonNode> params)
		throws IOException,
		IllegalAccessException,
		InvocationTargetException {

		// debug log
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.log(Level.FINE, "Invoking method: "+m.getName());
		}

		// convert the parameters
		Object[] convertedParams = new Object[params.size()];
		Type[] parameterTypes = m.getGenericParameterTypes();
		
		for (int i=0; i<parameterTypes.length; i++) {
		    JsonParser paramJsonParser = mapper.treeAsTokens(params.get(i));
		    JavaType paramJavaType = TypeFactory.defaultInstance().constructType(parameterTypes[i]);
			convertedParams[i] = mapper.readValue(paramJsonParser, paramJavaType);
		}

		// invoke the method
		Object result = m.invoke(target, convertedParams);
		return (m.getGenericReturnType()!=null) ? mapper.valueToTree(result) : null;
	}

	/**
	 * Convenience method for creating an error response.
	 *
	 * @param jsonRpc the jsonrpc string
	 * @param id the id
	 * @param code the error code
	 * @param message the error message
	 * @param data the error data (if any)
	 * @return the error response
	 */
	protected ObjectNode createErrorResponse(
		String jsonRpc, Object id, int code, String message, Object data) {
		ObjectNode response = mapper.createObjectNode();
		ObjectNode error = mapper.createObjectNode();
		error.put("code", code);
		error.put("message", message);
		if (data!=null) {
			error.put("data",  mapper.valueToTree(data));
		}
		response.put("jsonrpc", jsonRpc);
		if (Integer.class.isInstance(id)) {
			response.put("id", Integer.class.cast(id).intValue());
		} else if (Long.class.isInstance(id)) {
			response.put("id", Long.class.cast(id).longValue());
		} else if (Float.class.isInstance(id)) {
			response.put("id", Float.class.cast(id).floatValue());
		} else if (Double.class.isInstance(id)) {
			response.put("id", Double.class.cast(id).doubleValue());
		} else if (BigDecimal.class.isInstance(id)) {
			response.put("id", BigDecimal.class.cast(id));
		} else {
			response.put("id", String.class.cast(id));
		}
		response.put("error", error);
		return response;
	}

	/**
	 * Creates a sucess response.
	 * @param jsonRpc
	 * @param id
	 * @param result
	 * @return
	 */
	protected ObjectNode createSuccessResponse(String jsonRpc, Object id, JsonNode result) {
		ObjectNode response = mapper.createObjectNode();
		response.put("jsonrpc", jsonRpc);
		if (Integer.class.isInstance(id)) {
			response.put("id", Integer.class.cast(id).intValue());
		} else if (Long.class.isInstance(id)) {
			response.put("id", Long.class.cast(id).longValue());
		} else if (Float.class.isInstance(id)) {
			response.put("id", Float.class.cast(id).floatValue());
		} else if (Double.class.isInstance(id)) {
			response.put("id", Double.class.cast(id).doubleValue());
		} else if (BigDecimal.class.isInstance(id)) {
			response.put("id", BigDecimal.class.cast(id));
		} else {
			response.put("id", String.class.cast(id));
		}
		response.put("result", result);
		return response;
	}

	/**
	 * Finds the {@link Method} from the supplied {@link Set} that
	 * best matches the rest of the arguments supplied and returns
	 * it as a {@link MethodAndArgs} class.
	 *
	 * @param methods the {@link Method}s
	 * @param paramsNode the {@link JsonNode} passed as the parameters
	 * @return the {@link MethodAndArgs}
	 */
	private MethodAndArgs findBestMethodByParamsNode(Set<Method> methods, JsonNode paramsNode) {

		// no parameters
		if (paramsNode==null || paramsNode.isNull()) {
			return findBestMethodUsingParamIndexes(methods, 0, null);

		// array parameters
		} else if (paramsNode.isArray()) {
			return findBestMethodUsingParamIndexes(methods, paramsNode.size(), ArrayNode.class.cast(paramsNode));

		// named parameters
		} else if (paramsNode.isObject()) {
			Set<String> fieldNames = new HashSet<String>();
			Iterator<String> itr=paramsNode.fieldNames();
			while (itr.hasNext()) {
				fieldNames.add(itr.next());
			}
			return findBestMethodUsingParamNames(methods, fieldNames, ObjectNode.class.cast(paramsNode));

		}

		// unknown params node type
		throw new IllegalArgumentException("Unknown params node type: "+paramsNode.toString());
	}

	/**
	 * Finds the {@link Method} from the supplied {@link Set} that
	 * best matches the rest of the arguments supplied and returns
	 * it as a {@link MethodAndArgs} class.
	 *
	 * @param methods the {@link Method}s
	 * @param paramCount the number of expect parameters
	 * @param paramNodes the parameters for matching types
	 * @return the {@link MethodAndArgs}
	 */
	private MethodAndArgs findBestMethodUsingParamIndexes(
		Set<Method> methods, int paramCount, ArrayNode paramNodes) {

		// get param count
		int numParams = paramNodes!=null && !paramNodes.isNull()
			? paramNodes.size() : 0;

		// determine param count
		int bestParamNumDiff		= Integer.MAX_VALUE;
		Set<Method> matchedMethods	= new HashSet<Method>();

		// check every method
		for (Method method : methods) {

			// get parameter types
			Class<?>[] paramTypes = method.getParameterTypes();
			int paramNumDiff = paramTypes.length-paramCount;

			// we've already found a better match
			if (Math.abs(paramNumDiff)>Math.abs(bestParamNumDiff)) {
				continue;

			// we don't allow extra params
			} else if (
				!allowExtraParams && paramNumDiff<0
				|| !allowLessParams && paramNumDiff>0) {
				continue;

			// check the parameters
			} else {
				if (Math.abs(paramNumDiff)<Math.abs(bestParamNumDiff)) {
					matchedMethods.clear();
				}
				matchedMethods.add(method);
				bestParamNumDiff = paramNumDiff;
				continue;
			}
		}

		// bail early
		if (matchedMethods.isEmpty()) {
			return null;
		}

		// now narrow it down to the best method
		// based on argument types
		Method bestMethod = null;
		if (matchedMethods.size()==1 || numParams==0) {
			bestMethod = matchedMethods.iterator().next();

		} else {

			// check the matching methods for
			// matching parameter types
			int mostMatches	= -1;
			for (Method method : matchedMethods) {
				List<Class<?>> parameterTypes = getParameterTypes(method);
				int numMatches = 0;
				for (int i=0; i<parameterTypes.size() && i<numParams; i++) {
					if (isMatchingType(paramNodes.get(i), parameterTypes.get(i))) {
						numMatches++;
					}
				}
				if (numMatches>mostMatches) {
					mostMatches = numMatches;
					bestMethod = method;
				}
			}
		}

		// create return
		MethodAndArgs ret = new MethodAndArgs();
		ret.method = bestMethod;

		// now fill arguments
		int numParameters = bestMethod.getParameterTypes().length;
		for (int i=0; i<numParameters; i++) {
			if (i<numParams) {
				ret.arguments.add(paramNodes.get(i));
			} else {
				ret.arguments.add(NullNode.getInstance());
			}
		}

		// return the method
		return ret;
	}

	/**
	 * Finds the {@link Method} from the supplied {@link Set} that
	 * best matches the rest of the arguments supplied and returns
	 * it as a {@link MethodAndArgs} class.
	 *
	 * @param methods the {@link Method}s
	 * @param paramNames the parameter names
	 * @param paramNodes the parameters for matching types
	 * @return the {@link MethodAndArgs}
	 */
	@SuppressWarnings("deprecation")
	private MethodAndArgs findBestMethodUsingParamNames(
		Set<Method> methods, Set<String> paramNames, ObjectNode paramNodes) {

		// determine param count
		int maxMatchingParams 				= -1;
		int maxMatchingParamTypes			= -1;
		Method bestMethod 					= null;
		List<JsonRpcParam> bestAnnotations	= null;

		for (Method method : methods) {

			// get parameter types
			List<Class<?>> parameterTypes = getParameterTypes(method);

			// bail early if possible
			if (!allowExtraParams && paramNames.size()>parameterTypes.size()) {
				continue;
			} else if (!allowLessParams && paramNames.size()<parameterTypes.size()) {
				continue;
			}

			// list of params
			List<JsonRpcParam> annotations = new ArrayList<JsonRpcParam>();

			// try the deprecated parameter first
			List<List<JsonRpcParamName>> depMethodAnnotations = getParameterAnnotations(method, JsonRpcParamName.class);
			for (List<JsonRpcParamName> annots : depMethodAnnotations) {
				if (annots.size()>0) {
					final JsonRpcParamName annotation = annots.get(0);
					annotations.add(new JsonRpcParam() {
						public Class<? extends Annotation> annotationType() {
							return JsonRpcParam.class;
						}
						public String value() {
							return annotation.value();
						}
					});
				} else {
					annots.add(null);
				}
			}

			@SuppressWarnings("unchecked")
			List<List<Annotation>> jaxwsAnnotations = WEBPARAM_ANNOTATION_CLASS != null
				? getParameterAnnotations(method, (Class<Annotation>) WEBPARAM_ANNOTATION_CLASS)
				: new ArrayList<List<Annotation>>();
			for (List<Annotation> annots : jaxwsAnnotations) {
				if (annots.size()>0) {
					final Annotation annotation = annots.get(0);
					annotations.add(new JsonRpcParam() {
						public Class<? extends Annotation> annotationType() {
							return JsonRpcParam.class;
						}
						public String value() {
							try {
								return (String) WEBPARAM_NAME_METHOD.invoke(annotation);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						}
					});
				} else {
					annots.add(null);
				}
			}

			// now try the non-deprecated parameters
			List<List<JsonRpcParam>> methodAnnotations = getParameterAnnotations(method, JsonRpcParam.class);
			for (List<JsonRpcParam> annots : methodAnnotations) {
				if (annots.size()>0) {
					annotations.add(annots.get(0));
				} else {
					annots.add(null);
				}
			}

			// count the matching params for this method
			int numMatchingParamTypes = 0;
			int numMatchingParams = 0;
			for (int i=0; i<annotations.size(); i++) {

				// skip parameters that didn't have an annotation
				JsonRpcParam annotation	= annotations.get(i);
				if (annotation==null) {
					continue;
				}

				// check for a match
				String paramName			= annotation.value();
				boolean hasParamName 		= paramNames.contains(paramName);

				if (hasParamName && isMatchingType(paramNodes.get(paramName), parameterTypes.get(i))) {
					numMatchingParamTypes++;
					numMatchingParams++;

				} else if (hasParamName) {
					numMatchingParams++;

				}
			}

			// check for exact param matches
			// bail early if possible
			if (!allowExtraParams && numMatchingParams>parameterTypes.size()) {
				continue;
			} else if (!allowLessParams && numMatchingParams<parameterTypes.size()) {
				continue;
			}

			// better match
			if (numMatchingParams>maxMatchingParams
				|| (numMatchingParams==maxMatchingParams && numMatchingParamTypes>maxMatchingParamTypes)) {
				bestMethod 				= method;
				maxMatchingParams 		= numMatchingParams;
				maxMatchingParamTypes 	= numMatchingParamTypes;
				bestAnnotations 		= annotations;
			}
		}

		// bail early
		if (bestMethod==null) {
			return null;
		}

		// create return
		MethodAndArgs ret = new MethodAndArgs();
		ret.method = bestMethod;

		// now fill arguments
		int numParameters = bestMethod.getParameterTypes().length;
		for (int i=0; i<numParameters; i++) {
			JsonRpcParam param = bestAnnotations.get(i);
			if (param!=null && paramNames.contains(param.value())) {
				ret.arguments.add(paramNodes.get(param.value()));
			} else {
				ret.arguments.add(NullNode.getInstance());
			}
		}

		// return the method
		return ret;
	}

	/**
	 * Determines whether or not the given {@link JsonNode} matches
	 * the given type.  This method is limitted to a few java types
	 * only and shouldn't be used to determine with great accuracy
	 * whether or not the types match.
	 *
	 * @param node the {@link JsonNode}
	 * @param type the {@link Class}
	 * @return true if the types match, false otherwise
	 */
	private boolean isMatchingType(JsonNode node, Class<?> type) {

		if (node.isNull()) {
			return true;

		} else if (node.isTextual()) {
			return String.class.isAssignableFrom(type);

		} else if (node.isNumber()) {
			return Number.class.isAssignableFrom(type)
				|| short.class.isAssignableFrom(type)
				|| int.class.isAssignableFrom(type)
				|| long.class.isAssignableFrom(type)
				|| float.class.isAssignableFrom(type)
				|| double.class.isAssignableFrom(type);

		} else if (node.isArray() && type.isArray()) {
			return (node.size()>0)
				? isMatchingType(node.get(0), type.getComponentType())
				: false;

		} else if (node.isArray()) {
			return type.isArray() || Collection.class.isAssignableFrom(type);

		} else if (node.isBinary()) {
			return byte[].class.isAssignableFrom(type)
				|| Byte[].class.isAssignableFrom(type)
				|| char[].class.isAssignableFrom(type)
				|| Character[].class.isAssignableFrom(type);

		} else if (node.isBoolean()) {
			return boolean.class.isAssignableFrom(type)
				|| Boolean.class.isAssignableFrom(type);

		} else if (node.isObject() || node.isPojo()) {
			return !type.isPrimitive()
				&& !String.class.isAssignableFrom(type)
				&& !Number.class.isAssignableFrom(type)
				&& !Boolean.class.isAssignableFrom(type);
		}

		// not sure if it's a matching type
		return false;
	}

	/**
	 * Writes and flushes a value to the given {@link OutputStream}
	 * and prevents Jackson from closing it.
	 * @param ops the {@link OutputStream}
	 * @param value the value to write
	 * @throws IOException on error
	 */
	private void writeAndFlushValue(OutputStream ops, Object value)
		throws IOException {
		mapper.writeValue(new NoCloseOutputStream(ops), value);
		ops.flush();
	}

	/**
	 * Simple inner class for the {@code findXXX} methods.
	 */
	private static class MethodAndArgs {
		private Method method = null;
		private List<JsonNode> arguments = new ArrayList<JsonNode>();
	}

	/**
	 * Parses an ID.
	 * @param node
	 * @return
	 */
	private Object parseId(JsonNode node) {
		if (node==null || node.isNull()) {
			return null;
		} else if (node.isDouble()) {
			return node.asDouble();
		} else if (node.isFloatingPointNumber()) {
			return node.asDouble();
		} else if (node.isInt()) {
			return node.asInt();
		} else if (node.isIntegralNumber()) {
			return node.asInt();
		} else if (node.isLong()) {
			return node.asLong();
		} else if (node.isTextual()) {
			return node.asText();
		}
		throw new IllegalArgumentException("Unknown id type");
	}

	/**
	 * Sets whether or not the server should be backwards
	 * compatible to JSON-RPC 1.0.  This only includes the
	 * omission of the jsonrpc property on the request object,
	 * not the class hinting.
	 *
	 * @param backwardsComaptible the backwardsComaptible to set
	 */
	public void setBackwardsComaptible(boolean backwardsComaptible) {
		this.backwardsComaptible = backwardsComaptible;
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
	 * @param exceptionLogLevel the exceptionLogLevel to set
	 */
	public void setExceptionLogLevel(Level exceptionLogLevel) {
		this.exceptionLogLevel = exceptionLogLevel;
	}

}
