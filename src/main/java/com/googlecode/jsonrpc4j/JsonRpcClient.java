package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.ERROR;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.ID;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.JSONRPC;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.METHOD;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.PARAMS;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.RESULT;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.VERSION;
import static com.googlecode.jsonrpc4j.Util.hasNonNullData;

/**
 * A JSON-RPC client.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class JsonRpcClient {
	
	// Toha: to use same logger in extension classes
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private final ObjectMapper mapper;
	private final Random random;
	private RequestListener requestListener;
	private RequestIDGenerator requestIDGenerator;
	private ExceptionResolver exceptionResolver;
	private Map<String, Object> additionalJsonContent = new HashMap<>();
	
	/**
	 * Creates a client that uses the default {@link ObjectMapper}
	 * to map to and from JSON and Java objects.
	 */
	public JsonRpcClient() {
		this(new ObjectMapper());
	}

	/**
	 * Creates a client that uses the given {@link ObjectMapper} to
	 * map to and from JSON and Java objects.
	 *
	 * @param mapper the {@link ObjectMapper}
	 */
	public JsonRpcClient(ObjectMapper mapper) {
		this(mapper, DefaultExceptionResolver.INSTANCE);
	}

	/**
	 * Creates a client that uses the given {@link ObjectMapper} and
	 * {@link ExceptionResolver } to map to and from JSON and Java objects.
	 *
	 * @param mapper            the {@link ObjectMapper}
	 * @param exceptionResolver the {@link ExceptionResolver}
	 */
	public JsonRpcClient(ObjectMapper mapper, ExceptionResolver exceptionResolver) {
		this.mapper = mapper;
		this.random = new Random(System.currentTimeMillis());
		this.requestIDGenerator = new RandomRequestIDGenerator();
		this.exceptionResolver = exceptionResolver;
	}

	public Map<String, Object> getAdditionalJsonContent() {
		return additionalJsonContent;
	}
	
	public void setAdditionalJsonContent(Map<String, Object> additionalJsonContent) {
		this.additionalJsonContent = additionalJsonContent;
	}
	
	/**
	 * Sets the {@link RequestListener}.
	 *
	 * @param requestListener the {@link RequestListener}
	 */
	public void setRequestListener(RequestListener requestListener) {
		this.requestListener = requestListener;
	}
	
	/**
	 * Set the {@link RequestIDGenerator}
	 *
	 * @param requestIDGenerator the {@link RequestIDGenerator}
	 */
	public void setRequestIDGenerator(RequestIDGenerator requestIDGenerator) {
		this.requestIDGenerator = requestIDGenerator;
	}
	
	/**
	 * Invokes the given method on the remote service
	 * passing the given arguments, a generated id and reads
	 * a response.
	 *
	 * @param methodName the method to invoke
	 * @param argument   the argument to pass to the method
	 * @param clazz      the expected return type
	 * @param output     the {@link OutputStream} to write to
	 * @param input      the {@link InputStream} to read from
	 * @param <T>        the expected return type
	 * @return the returned Object
	 * @throws Throwable on error
	 * @see #writeRequest(String, Object, OutputStream, String)
	 */
	@SuppressWarnings("unchecked")
	public <T> T invokeAndReadResponse(String methodName, Object argument, Class<T> clazz, OutputStream output, InputStream input) throws Throwable {
		return (T) invokeAndReadResponse(methodName, argument, Type.class.cast(clazz), output, input);
	}
	
	/**
	 * Invokes the given method on the remote service
	 * passing the given arguments, a generated id and reads
	 * a response.
	 *
	 * @param methodName the method to invoke
	 * @param argument   the argument to pass to the method
	 * @param returnType the expected return type
	 * @param output     the {@link OutputStream} to write to
	 * @param input      the {@link InputStream} to read from
	 * @return the returned Object
	 * @throws Throwable on error
	 * @see #writeRequest(String, Object, OutputStream, String)
	 */
	public Object invokeAndReadResponse(String methodName, Object argument, Type returnType, OutputStream output, InputStream input) throws Throwable {
		return invokeAndReadResponse(methodName, argument, returnType, output, input, this.requestIDGenerator.generateID());
	}
	
	/**
	 * Invokes the given method on the remote service
	 * passing the given arguments and reads a response.
	 *
	 * @param methodName the method to invoke
	 * @param argument   the argument to pass to the method
	 * @param returnType the expected return type
	 * @param output     the {@link OutputStream} to write to
	 * @param input      the {@link InputStream} to read from
	 * @param id         id to send with the JSON-RPC request
	 * @return the returned Object
	 * @throws Throwable if there is an error
	 *                   while reading the response
	 * @see #writeRequest(String, Object, OutputStream, String)
	 */
	private Object invokeAndReadResponse(String methodName, Object argument, Type returnType, OutputStream output, InputStream input, String id) throws Throwable {
		invoke(methodName, argument, output, id);
		return readResponse(returnType, input, id);
	}
	
	/**
	 * Invokes the given method on the remote service passing
	 * the given argument.  To read the response
	 * {@link #readResponse(Type, InputStream)}  must be subsequently
	 * called.
	 *
	 * @param methodName the method to invoke
	 * @param argument   the argument to pass to the method
	 * @param output     the {@link OutputStream} to write to
	 * @param id         the request id
	 * @throws IOException on error
	 * @see #writeRequest(String, Object, OutputStream, String)
	 */
	private void invoke(String methodName, Object argument, OutputStream output, String id) throws IOException {
		writeRequest(methodName, argument, output, id);
		output.flush();
	}
	
	/**
	 * Reads a JSON-RPC response from the server.  This blocks until
	 * a response is received. If an id is given, responses that do
	 * not correspond, are disregarded.
	 *
	 * @param returnType the expected return type
	 * @param input      the {@link InputStream} to read from
	 * @param id         The id used to compare the response with.
	 * @return the object returned by the JSON-RPC response
	 * @throws Throwable on error
	 */
	private Object readResponse(Type returnType, InputStream input, String id) throws Throwable {
		
		ReadContext context = ReadContext.getReadContext(input, mapper);
		ObjectNode jsonObject = getValidResponse(id, context);
		notifyAnswerListener(jsonObject);
		handleErrorResponse(jsonObject);
		
		if (hasResult(jsonObject)) {
			if (isReturnTypeInvalid(returnType)) {
				return null;
			}
			return constructResponseObject(returnType, jsonObject);
		}
		
		// no return type
		return null;
	}
	
	/**
	 * Writes a JSON-RPC request to the given {@link OutputStream}.
	 * If the value passed for argument is null then the {@code params}
	 * property is omitted from the JSON-RPC request.  If the argument
	 * is not not null then it is used as the value of the {@code params}
	 * property.  This means that if a POJO is passed as the argument
	 * that it's properties will be used as the param, ie:
	 * <pre>
	 * class Person {
	 *   String firstName;
	 *   String lastName;
	 * }
	 * </pre>
	 * becomes:
	 * <pre>
	 * 	"params" : {
	 * 		"firstName" : ..;
	 * 		"lastName" : ..;
	 * }
	 * </pre>
	 * The same would be true of a {@link Map} containing the keys
	 * {@code firstName} and {@code lastName}.  If the argument passed
	 * in implements the {@link Collection} interface or is an array
	 * then the values are used as indexed parameters in the order that
	 * they appear in the {@link Collection} or array.
	 *
	 * @param methodName the method to invoke
	 * @param argument   the method argument
	 * @param output     the {@link OutputStream} to write to
	 * @param id         the request id
	 * @throws IOException on error
	 */
	private void writeRequest(String methodName, Object argument, OutputStream output, String id) throws IOException {
		internalWriteRequest(methodName, argument, output, id);
	}
	
	private ObjectNode getValidResponse(String id, ReadContext context) throws IOException {
		JsonNode response = readResponseNode(context);
		raiseExceptionIfNotValidResponseObject(response);
		ObjectNode jsonObject = ObjectNode.class.cast(response);
		
		if (id != null) {
			while (isIdValueNotCorrect(id, jsonObject)) {
				response = context.nextValue();
				raiseExceptionIfNotValidResponseObject(response);
				jsonObject = ObjectNode.class.cast(response);
			}
		}
		return jsonObject;
	}
	
	private void notifyAnswerListener(ObjectNode jsonObject) {
		if (this.requestListener != null) {
			this.requestListener.onBeforeResponseProcessed(this, jsonObject);
		}
	}
	
	protected void handleErrorResponse(ObjectNode jsonObject) throws Throwable {
		if (hasError(jsonObject)) {
			// resolve and throw the exception
			if (exceptionResolver == null) {
				throw DefaultExceptionResolver.INSTANCE.resolveException(jsonObject);
			} else {
				throw exceptionResolver.resolveException(jsonObject);
			}
		}
	}
	
	private boolean hasResult(ObjectNode jsonObject) {
		return hasNonNullData(jsonObject, RESULT);
	}
	
	private boolean isReturnTypeInvalid(Type returnType) {
		if (returnType == null || returnType == Void.class) {
			logger.warn("Server returned result but returnType is null");
			return true;
		}
		return false;
	}
	
	private Object constructResponseObject(Type returnType, ObjectNode jsonObject) throws IOException {
		JsonParser returnJsonParser = mapper.treeAsTokens(jsonObject.get(RESULT));
		JavaType returnJavaType = mapper.getTypeFactory().constructType(returnType);
		return mapper.readValue(returnJsonParser, returnJavaType);
	}
	
	/**
	 * Writes a request.
	 *
	 * @param methodName the method name
	 * @param arguments  the arguments
	 * @param output     the stream
	 * @param id         the optional id
	 * @throws IOException on error
	 */
	private void internalWriteRequest(String methodName, Object arguments, OutputStream output, String id) throws IOException {
		final ObjectNode request = internalCreateRequest(methodName, arguments, id);
		logger.debug("Request {}", request);
		writeAndFlushValue(output, request);
	}
	
	private JsonNode readResponseNode(ReadContext context) throws IOException {
		context.assertReadable();
		JsonNode response = context.nextValue();
		logger.debug("JSON-RPC Response: {}", response);
		return response;
	}
	
	private void raiseExceptionIfNotValidResponseObject(JsonNode response) {
		if (isInvalidResponse(response)) {
			throw new JsonRpcClientException(0, "Invalid JSON-RPC response", response);
		}
	}
	
	private boolean isIdValueNotCorrect(String id, ObjectNode jsonObject) {
		return !jsonObject.has(ID) || jsonObject.get(ID) == null || !jsonObject.get(ID).asText().equals(id);
	}
	
	protected boolean hasError(ObjectNode jsonObject) {
		return jsonObject.has(ERROR) && jsonObject.get(ERROR) != null && !jsonObject.get(ERROR).isNull();
	}
	
	/**
	 * Creates RPC request.
	 *
	 * @param methodName the method name
	 * @param arguments  the arguments
	 * @param id         the optional id
	 * @return Jackson request object
	 */
	private ObjectNode internalCreateRequest(String methodName, Object arguments, String id) {
		final ObjectNode request = mapper.createObjectNode();
		addId(id, request);
		addProtocolAndMethod(methodName, request);
		addParameters(arguments, request);
		addAdditionalHeaders(request);
		notifyBeforeRequestListener(request);
		return request;
	}
	
	/**
	 * Writes and flushes a value to the given {@link OutputStream}
	 * and prevents Jackson from closing it.
	 *
	 * @param output the {@link OutputStream}
	 * @param value  the value to write
	 * @throws IOException on error
	 */
	private void writeAndFlushValue(OutputStream output, Object value) throws IOException {
		mapper.writeValue(new NoCloseOutputStream(output), value);
		output.flush();
	}
	
	private boolean isInvalidResponse(JsonNode response) {
		return !response.isObject();
	}
	
	private void addId(String id, ObjectNode request) {
		if (id != null) {
			request.put(ID, id);
		}
	}
	
	private void addProtocolAndMethod(String methodName, ObjectNode request) {
		request.put(JSONRPC, VERSION);
		request.put(METHOD, methodName);
	}
	
	private void addParameters(Object arguments, ObjectNode request) {
		// object array args
		if (isArrayArguments(arguments)) {
			addArrayArguments(arguments, request);
			// collection args
		} else if (isCollectionArguments(arguments)) {
			addCollectionArguments(arguments, request);
			// map args
		} else if (isMapArguments(arguments)) {
			addMapArguments(arguments, request);
			// other args
		} else if (arguments != null) {
			request.set(PARAMS, mapper.valueToTree(arguments));
		}
	}
	
	private void addAdditionalHeaders(ObjectNode request) {
		for (Map.Entry<String, Object> entry : additionalJsonContent.entrySet()) {
			request.set(entry.getKey(), mapper.valueToTree(entry.getValue()));
		}
	}
	
	private void notifyBeforeRequestListener(ObjectNode request) {
		if (this.requestListener != null) {
			this.requestListener.onBeforeRequestSent(this, request);
		}
	}
	
	private boolean isArrayArguments(Object arguments) {
		return arguments != null && arguments.getClass().isArray();
	}
	
	private void addArrayArguments(Object arguments, ObjectNode request) {
		Object[] args = Object[].class.cast(arguments);
		if (args.length > 0) {
			// serialize every param for itself so jackson can determine right serializer
			ArrayNode paramsNode = new ArrayNode(mapper.getNodeFactory());
			for (Object arg : args) {
				JsonNode argNode = mapper.valueToTree(arg);
				paramsNode.add(argNode);
			}
			request.set(PARAMS, paramsNode);
		}
	}
	
	private boolean isCollectionArguments(Object arguments) {
		return arguments != null && Collection.class.isInstance(arguments);
	}
	
	private void addCollectionArguments(Object arguments, ObjectNode request) {
		Collection<?> args = Collection.class.cast(arguments);
		if (!args.isEmpty()) {
			// serialize every param for itself so jackson can determine right serializer
			ArrayNode paramsNode = new ArrayNode(mapper.getNodeFactory());
			for (Object arg : args) {
				JsonNode argNode = mapper.valueToTree(arg);
				paramsNode.add(argNode);
			}
			request.set(PARAMS, paramsNode);
		}
	}
	
	private boolean isMapArguments(Object arguments) {
		return arguments != null && Map.class.isInstance(arguments);
	}
	
	private void addMapArguments(Object arguments, ObjectNode request) {
		if (!Map.class.cast(arguments).isEmpty()) {
			request.set(PARAMS, mapper.valueToTree(arguments));
		}
	}
	
	private String generateRandomId() {
		return Integer.toString(random.nextInt(Integer.MAX_VALUE));
	}
	
	/**
	 * Invokes the given method on the remote service
	 * passing the given arguments and reads a response.
	 *
	 * @param methodName the method to invoke
	 * @param argument   the argument to pass to the method
	 * @param clazz      the expected return type
	 * @param output     the {@link OutputStream} to write to
	 * @param input      the {@link InputStream} to read from
	 * @param id         id to send with the JSON-RPC request
	 * @param <T>        the expected return type
	 * @return the returned Object
	 * @throws Throwable if there is an error
	 *                   while reading the response
	 * @see #writeRequest(String, Object, OutputStream, String)
	 */
	@SuppressWarnings("unchecked")
	public <T> T invokeAndReadResponse(String methodName, Object argument, Class<T> clazz, OutputStream output, InputStream input, String id) throws Throwable {
		return (T) invokeAndReadResponse(methodName, argument, Type.class.cast(clazz), output, input, id);
	}
	
	/**
	 * Invokes the given method on the remote service passing
	 * the given argument.  An id is generated automatically.  To read
	 * the response {@link #readResponse(Type, InputStream)}  must be
	 * subsequently called.
	 *
	 * @param methodName the method to invoke
	 * @param argument   the arguments to pass to the method
	 * @param output     the {@link OutputStream} to write to
	 * @throws IOException on error
	 * @see #writeRequest(String, Object, OutputStream, String)
	 */
	public void invoke(String methodName, Object argument, OutputStream output) throws IOException {
		invoke(methodName, argument, output, this.requestIDGenerator.generateID());
	}
	
	/**
	 * Invokes the given method on the remote service passing
	 * the given argument without reading or expecting a return
	 * response.
	 *
	 * @param methodName the method to invoke
	 * @param argument   the argument to pass to the method
	 * @param output     the {@link OutputStream} to write to
	 * @throws IOException on error
	 * @see #writeRequest(String, Object, OutputStream, String)
	 */
	public void invokeNotification(String methodName, Object argument, OutputStream output) throws IOException {
		writeRequest(methodName, argument, output, null);
		output.flush();
	}
	
	/**
	 * Reads a JSON-RPC response from the server.  This blocks until
	 * a response is received.
	 *
	 * @param clazz the expected return type
	 * @param input the {@link InputStream} to read from
	 * @param <T>   the expected return type
	 * @return the object returned by the JSON-RPC response
	 * @throws Throwable on error
	 */
	@SuppressWarnings("unchecked")
	public <T> T readResponse(Class<T> clazz, InputStream input) throws Throwable {
		return (T) readResponse((Type) clazz, input);
	}
	
	/**
	 * Reads a JSON-RPC response from the server.  This blocks until
	 * a response is received.
	 *
	 * @param returnType the expected return type
	 * @param input      the {@link InputStream} to read from
	 * @return the object returned by the JSON-RPC response
	 * @throws Throwable on error
	 */
	public Object readResponse(Type returnType, InputStream input) throws Throwable {
		return readResponse(returnType, input, null);
	}
	
	/**
	 * Reads a JSON-RPC response from the server.  This blocks until
	 * a response is received. If an id is given, responses that do
	 * not correspond, are disregarded.
	 *
	 * @param clazz the expected return type
	 * @param input the {@link InputStream} to read from
	 * @param id    The id used to compare the response with
	 * @param <T>   the expected return type
	 * @return the object returned by the JSON-RPC response
	 * @throws Throwable on error
	 */
	@SuppressWarnings("unchecked")
	public <T> T readResponse(Class<T> clazz, InputStream input, String id) throws Throwable {
		return (T) readResponse((Type) clazz, input, id);
	}
	
	protected ObjectNode createRequest(String methodName, Object argument) {
		return internalCreateRequest(methodName, argument, this.requestIDGenerator.generateID());
	}
	
	public ObjectNode createRequest(String methodName, Object argument, String id) {
		return internalCreateRequest(methodName, argument, id);
	}
	
	/**
	 * Writes a JSON-RPC notification to the given
	 * {@link OutputStream}.
	 *
	 * @param methodName the method to invoke
	 * @param argument   the method argument
	 * @param output     the {@link OutputStream} to write to
	 * @throws IOException on error
	 * @see #writeRequest(String, Object, OutputStream, String)
	 */
	public void writeNotification(String methodName, Object argument, OutputStream output) throws IOException {
		internalWriteRequest(methodName, argument, output, null);
	}
	
	// Suppose than jsonObject is single and contains valid id :)
	protected Object readResponse(Type returnType, JsonNode jsonObject) throws Throwable {
		return readResponse(returnType, jsonObject, null);
	}
	
	// Suppose than jsonObject is single and contains valid id :)
	private Object readResponse(Type returnType, JsonNode jsonNode, String id) throws Throwable {
		raiseExceptionIfNotValidResponseObject(jsonNode);
		final ObjectNode jsonObject = ObjectNode.class.cast(jsonNode);
		notifyAnswerListener(jsonObject);
		handleErrorResponse(jsonObject);
		if (hasResult(jsonObject)) {
			if (isReturnTypeInvalid(returnType)) {
				return null;
			}
			return constructResponseObject(returnType, jsonObject);
		}
		return null;
	}
	
	/**
	 * Returns the {@link ObjectMapper} that the client
	 * is using for JSON marshalling.
	 *
	 * @return the {@link ObjectMapper}
	 */
	public ObjectMapper getObjectMapper() {
		return mapper;
	}
	
	/**
	 * @param exceptionResolver the exceptionResolver to set
	 */
	public void setExceptionResolver(ExceptionResolver exceptionResolver) {
		this.exceptionResolver = exceptionResolver;
	}
	
	/**
	 * Provides access to the jackson {@link ObjectNode}s
	 * that represent the JSON-RPC requests and responses.
	 */
	public interface RequestListener {
		
		/**
		 * Called before a request is sent to the
		 * server end-point.  Modifications can be
		 * made to the request before it's sent.
		 *
		 * @param client  the {@link JsonRpcClient}
		 * @param request the request {@link ObjectNode}
		 */
		void onBeforeRequestSent(JsonRpcClient client, ObjectNode request);
		
		/**
		 * Called after a response has been returned and
		 * successfully parsed but before it has been
		 * processed and turned into java objects.
		 *
		 * @param client   the {@link JsonRpcClient}
		 * @param response the response {@link ObjectNode}
		 */
		void onBeforeResponseProcessed(JsonRpcClient client, ObjectNode response);
	}
	
	/**
	 * Default generator which returns random generated request ID.
	 */
	class RandomRequestIDGenerator implements RequestIDGenerator {
		
		@Override
		public String generateID() {
			return generateRandomId();
		}
	}
}
