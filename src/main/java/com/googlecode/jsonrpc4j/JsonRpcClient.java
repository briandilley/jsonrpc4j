/*
The MIT License (MIT)

Copyright (c) 2014 jsonrpc4j

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package com.googlecode.jsonrpc4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * A JSON-RPC client.
 */
public class JsonRpcClient {

	private static final Logger LOGGER = Logger.getLogger(JsonRpcClient.class.getName());

	private static final String JSON_RPC_VERSION = "2.0";

	private final ObjectMapper mapper;
	private final Random random;
	private RequestListener requestListener;
	private ExceptionResolver exceptionResolver = DefaultExceptionResolver.INSTANCE;

	private Map<String,Object> additionalJsonHeaders = new HashMap();

	/**
	 * Creates a client that uses the given {@link ObjectMapper} to
	 * map to and from JSON and Java objects.
	 * @param mapper the {@link ObjectMapper}
	 */
	public JsonRpcClient(ObjectMapper mapper) {
		this.mapper = mapper;
		this.random = new Random(System.currentTimeMillis());
	}

	/**
	 * Creates a client that uses the default {@link ObjectMapper}
	 * to map to and from JSON and Java objects.
	 */
	public JsonRpcClient() {
		this(new ObjectMapper());
	}

	/**
	 * Sets the {@link RequestListener}.
	 * @param requestListener the {@link RequestListener}
	 */
	public void setRequestListener(RequestListener requestListener) {
		this.requestListener = requestListener;
	}

	/**
	 * get the additional request headers of the json-rpc object
	 *
	 * @return the json header map
	 */
	public Map<String, Object> getAdditionalJsonHeaders()
	{
		return additionalJsonHeaders;
	}

	/**
	 * sets the additional request headers of the json-rpc object
	 *
	 */
	public void setAdditionalJsonHeaders(Map<String, Object> additionalJsonHeaders)
	{
		this.additionalJsonHeaders = additionalJsonHeaders;
	}

	/**
	 * Invokes the given method on the remote service
	 * passing the given arguments, a generated id and reads
	 * a response.
	 *
	 * @see #writeRequest(String, Object, OutputStream, String)
	 * @param methodName the method to invoke
	 * @param argument the argument to pass to the method
	 * @param returnType the expected return type
	 * @param ops the {@link OutputStream} to write to
	 * @param ips the {@link InputStream} to read from
	 * @return the returned Object
	 * @throws Throwable on error
	 */
	public Object invokeAndReadResponse(
		String methodName, Object argument, Type returnType,
		OutputStream ops, InputStream ips)
		throws Throwable {
		return invokeAndReadResponse(
			methodName, argument, returnType, ops, ips, random.nextLong() + "");
	}

	/**
	 * Invokes the given method on the remote service
	 * passing the given arguments, a generated id and reads
	 * a response.
	 *
	 * @see #writeRequest(String, Object, OutputStream, String)
	 * @param methodName the method to invoke
	 * @param argument the argument to pass to the method
	 * @param clazz the expected return type
	 * @param ops the {@link OutputStream} to write to
	 * @param ips the {@link InputStream} to read from
	 * @param <T> the expected return type
	 * @return the returned Object
	 * @throws Throwable on error
	 */
	@SuppressWarnings("unchecked")
	public <T> T invokeAndReadResponse(
		String methodName, Object argument, Class<T> clazz,
		OutputStream ops, InputStream ips)
		throws Throwable {
		return (T)invokeAndReadResponse(
			methodName, argument, Type.class.cast(clazz), ops, ips);
	}

	/**
	 * Invokes the given method on the remote service
	 * passing the given arguments and reads a response.
	 *
	 * @see #writeRequest(String, Object, OutputStream, String)
	 * @param methodName the method to invoke
	 * @param argument the argument to pass to the method
	 * @param returnType the expected return type
	 * @param ops the {@link OutputStream} to write to
	 * @param ips the {@link InputStream} to read from
	 * @param id id to send with the JSON-RPC request
	 * @return the returned Object
	 * @throws Throwable if there is an error
	 * 	while reading the response
	 */
	public Object invokeAndReadResponse(
		String methodName, Object argument, Type returnType,
		OutputStream ops, InputStream ips, String id)
		throws Throwable {

		// invoke it
		invoke(methodName, argument, ops, id);

		// read it
		return readResponse(returnType, ips, id);
	}

	/**
	 * Invokes the given method on the remote service
	 * passing the given arguments and reads a response.
	 *
	 * @see #writeRequest(String, Object, OutputStream, String)
	 * @param methodName the method to invoke
	 * @param argument the argument to pass to the method
	 * @param clazz the expected return type
	 * @param ops the {@link OutputStream} to write to
	 * @param ips the {@link InputStream} to read from
	 * @param id id to send with the JSON-RPC request
	 * @param <T> the expected return type
	 * @return the returned Object
	 * @throws Throwable if there is an error
	 * 	while reading the response
	 */
	@SuppressWarnings("unchecked")
	public <T> T invokeAndReadResponse(
		String methodName, Object argument, Class<T> clazz,
		OutputStream ops, InputStream ips, String id)
		throws Throwable {
		return (T)invokeAndReadResponse(
			methodName, argument, Type.class.cast(clazz), ops, ips, id);
	}

	/**
	 * Invokes the given method on the remote service passing
	 * the given argument.  An id is generated automatically.  To read
	 * the response {@link #readResponse(Type, InputStream)}  must be
	 * subsequently called.
	 *
	 * @see #writeRequest(String, Object, OutputStream, String)
	 * @param methodName the method to invoke
	 * @param argument the arguments to pass to the method
	 * @param ops the {@link OutputStream} to write to
	 * @throws IOException on error
	 */
	public void invoke(
		String methodName, Object argument, OutputStream ops)
		throws IOException {
		invoke(methodName, argument, ops, random.nextLong()+"");
	}

	/**
	 * Invokes the given method on the remote service passing
	 * the given argument.  To read the response
	 * {@link #readResponse(Type, InputStream)}  must be subsequently
	 * called.
	 *
	 * @see #writeRequest(String, Object, OutputStream, String)
	 * @param methodName the method to invoke
	 * @param argument the argument to pass to the method
	 * @param ops the {@link OutputStream} to write to
	 * @param id the request id
	 * @throws IOException on error
	 */
	public void invoke(
		String methodName, Object argument, OutputStream ops, String id)
		throws IOException {
		writeRequest(methodName, argument, ops, id);
		ops.flush();
	}

	/**
	 * Invokes the given method on the remote service passing
	 * the given argument without reading or expecting a return
	 * response.
	 *
	 * @see #writeRequest(String, Object, OutputStream, String)
	 * @param methodName the method to invoke
	 * @param argument the argument to pass to the method
	 * @param ops the {@link OutputStream} to write to
	 * @throws IOException on error
	 */
	public void invokeNotification(
		String methodName, Object argument, OutputStream ops)
		throws IOException {
		writeRequest(methodName, argument, ops, null);
		ops.flush();
	}

	/**
	 * Reads a JSON-PRC response from the server.  This blocks until
	 * a response is received.
	 *
	 * @param clazz the expected return type
	 * @param ips the {@link InputStream} to read from
	 * @param <T> the expected return type
	 * @return the object returned by the JSON-RPC response
	 * @throws Throwable on error
	 */
	@SuppressWarnings("unchecked")
	public <T> T readResponse(Class<T> clazz, InputStream ips)
		throws Throwable {
		return (T)readResponse((Type)clazz, ips);
	}

	/**
	 * Reads a JSON-PRC response from the server.  This blocks until
	 * a response is received. If an id is given, responses that do
	 * not correspond, are disregarded.
	 *
	 * @param clazz the expected return type
	 * @param ips the {@link InputStream} to read from
	 * @param id The id used to compare the response with
	 * @param <T> the expected return type
	 * @return the object returned by the JSON-RPC response
	 * @throws Throwable on error
	 */
	@SuppressWarnings("unchecked")
	public <T> T readResponse(Class<T> clazz, InputStream ips, String id)
		throws Throwable {
		return (T)readResponse((Type)clazz, ips, id);
	}

	/**
	 * Reads a JSON-PRC response from the server.  This blocks until
	 * a response is received.
	 *
	 * @param returnType the expected return type
	 * @param ips the {@link InputStream} to read from
	 * @return the object returned by the JSON-RPC response
	 * @throws Throwable on error
	 */
	public Object readResponse(Type returnType, InputStream ips)
		throws Throwable {

		return readResponse(returnType, ips, null);
	}

	/**
	 * Reads a JSON-PRC response from the server.  This blocks until
	 * a response is received. If an id is given, responses that do
	 * not correspond, are disregarded.
	 *
	 * @param returnType the expected return type
	 * @param ips the {@link InputStream} to read from
	 * @param id The id used to compare the response with.
	 * @return the object returned by the JSON-RPC response
	 * @throws Throwable on error
	 */
	public Object readResponse(Type returnType, InputStream ips, String id)
		throws Throwable {

		// get node iterator
		ReadContext ctx = ReadContext.getReadContext(ips, mapper);

		// read the response
		ctx.assertReadable();
		JsonNode response = ctx.nextValue();
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.log(Level.FINE, "JSON-PRC Response: {0}", response.toString());
		}

		// bail on invalid response
		if (!response.isObject()) {
			throw new JsonRpcClientException(0, "Invalid JSON-RPC response", response);
		}
		ObjectNode jsonObject = ObjectNode.class.cast(response);

		if(id != null) {
			while(!jsonObject.has("id") ||
				jsonObject.get("id") == null ||
				!jsonObject.get("id").asText().equals(id)) {
				response = ctx.nextValue();

				if (!response.isObject()) {
					throw new JsonRpcClientException(0, "Invalid JSON-RPC response", response);
				}
				jsonObject = ObjectNode.class.cast(response);
			}
		}

		// show to listener
		if (this.requestListener!=null) {
			this.requestListener.onBeforeResponseProcessed(this, jsonObject);
		}

		// detect errors
		if (jsonObject.has("error")
				&& jsonObject.get("error")!=null
				&& !jsonObject.get("error").isNull()) {

			// resolve and throw the exception
			if (exceptionResolver==null) {
				throw DefaultExceptionResolver.INSTANCE.resolveException(jsonObject);
			} else {
				throw exceptionResolver.resolveException(jsonObject);
			}
		}

		// convert it to a return object
		if (jsonObject.has("result")
			&& !jsonObject.get("result").isNull()
			&& jsonObject.get("result")!=null) {
			if (returnType==null) {
				LOGGER.warning(
					"Server returned result but returnType is null");
				return null;
			}
			
			JsonParser returnJsonParser = mapper.treeAsTokens(jsonObject.get("result"));
			JavaType returnJavaType = TypeFactory.defaultInstance().constructType(returnType);
			
			return mapper.readValue(returnJsonParser, returnJavaType);
		}

		// no return type
		return null;
	}

	/**
	 * Writes a JSON-RPC request to the given {@link OutputStream}.
	 * If the value passed for argument is null then the {@code params}
	 * property is ommitted from the JSON-RPC request.  If the argument
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
	 * @param argument the method argument
	 * @param ops the {@link OutputStream} to write to
	 * @param id the request id
	 * @throws IOException on error
	 */
	public void writeRequest(
		String methodName, Object argument, OutputStream ops, String id)
		throws IOException {
		internalWriteRequest(methodName, argument, ops, id);
	}


	public ObjectNode createRequest(String methodName, Object argument) {
		return internalCreateRequest(methodName, argument, "" + random.nextLong());
	}

	public ObjectNode createRequest(String methodName, Object argument, String id)  {
		return internalCreateRequest(methodName, argument, id);
	}

	/**
	 * Writes a JSON-RPC notification to the given
	 * {@link OutputStream}.
	 * 
	 * @see #writeRequest(String, Object, OutputStream, String)
	 * @param methodName the method to invoke
	 * @param argument the method argument
	 * @param ops the {@link OutputStream} to write to
	 * @throws IOException on error
	 */
	public void writeNotification(
		String methodName, Object argument, OutputStream ops)
		throws IOException {
		internalWriteRequest(methodName, argument, ops, null);
	}

	/**
	 * Writes a request.
	 * @param methodName the method name
	 * @param arguments the arguments
	 * @param ops the stream
	 * @param id the optional id
	 * @throws IOException on error
	 */
	private void internalWriteRequest(
		String methodName, Object arguments, OutputStream ops, String id)
		throws IOException {
		
		// create the request
		final ObjectNode request =  internalCreateRequest(methodName, arguments, id); 
		// post the json data;
		writeAndFlushValue(ops, request);
	}


	/**
	 * Creates RPC request.
	 * @param methodName the method name
	 * @param arguments the arguments
	 * @param ops the stream
	 * @param id the optional id
	 * @return 
	 *	Jackson request object
	 * @throws IOException on error
	 */
	private ObjectNode internalCreateRequest(String methodName, Object arguments, String id) {
		
		// create the request
		final ObjectNode request = mapper.createObjectNode();
		
		// add id
		if (id!=null) { request.put("id", id); }
		
		// add protocol and method
		request.put("jsonrpc", JSON_RPC_VERSION);
		request.put("method", methodName);
		
		// object array args
		if (arguments!=null && arguments.getClass().isArray()) {
			Object[] args = Object[].class.cast(arguments);
			if (args.length > 0) {
				// serialize every param for itself so jackson can determine
				// right serializer
				ArrayNode paramsNode = new ArrayNode(mapper.getNodeFactory());
				for (Object arg : args) {
					JsonNode argNode = mapper.valueToTree(arg);
					paramsNode.add(argNode);
				}
				request.set("params", paramsNode);
			}
		
		// collection args
		} else if (arguments!=null && Collection.class.isInstance(arguments)) {
			Collection<?> args = Collection.class.cast(arguments);
			if (!args.isEmpty()) {
				// serialize every param for itself so jackson can determine
				// right serializer
				ArrayNode paramsNode = new ArrayNode(mapper.getNodeFactory());
				for (Object arg : args) {
					JsonNode argNode = mapper.valueToTree(arg);
					paramsNode.add(argNode);
				}
				request.set("params", paramsNode);
			}
			
		// map args
		} else if (arguments != null && Map.class.isInstance(arguments)) {
			if (!Map.class.cast(arguments).isEmpty()) {
				request.set("params", mapper.valueToTree(arguments));
			}

		// other args
		} else if (arguments != null) {
			request.set("params", mapper.valueToTree(arguments));
		}

		// inject additional json headers
		if(this.additionalJsonHeaders!=null && this.additionalJsonHeaders.size()>0)
		{
			for(String headerString : this.additionalJsonHeaders.keySet())
			{
				request.set(headerString, mapper.valueToTree(this.additionalJsonHeaders.get(headerString)));
			}
		}

		// show to listener
		if (this.requestListener!=null) {
			this.requestListener.onBeforeRequestSent(this, request);
		}

		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.log(Level.FINE, "JSON-PRC Request: {0}", request.toString());
		}

		return request;
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


	// Suppose than jsonObject is single and containts valid id :)
	protected Object readResponse(Type returnType, ObjectNode jsonObject)
		throws Throwable {
		return readResponse(returnType, jsonObject, null);
	}

	// Suppose than jsonObject is single and containts valid id :)
	protected Object readResponse(Type returnType, ObjectNode jsonObject, String id)
		throws Throwable {

		if (!jsonObject.isObject()) 
			throw new JsonRpcClientException(0, "Invalid JSON-RPC response", jsonObject);


		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.log(Level.FINE, "JSON-PRC Response: {0}", jsonObject.toString());
		}

		// show to listener
		if (this.requestListener!=null) {
			this.requestListener.onBeforeResponseProcessed(this, jsonObject);
		}

		// detect errors
		if (jsonObject.has("error") && jsonObject.get("error") != null && !jsonObject.get("error").isNull()) {
			// resolve and throw the exception
			if (exceptionResolver == null) {
				throw DefaultExceptionResolver.INSTANCE.resolveException(jsonObject);
			} else {
				throw exceptionResolver.resolveException(jsonObject);
			}
		}

		// convert it to a return object
		if (jsonObject.has("result")
			&& !jsonObject.get("result").isNull()
			&& jsonObject.get("result") != null) {
			if (returnType == null) {
				LOGGER.warning(
					"Server returned result but returnType is null");
				return null;
			}
			
			JsonParser returnJsonParser = mapper.treeAsTokens(jsonObject.get("result"));
			JavaType returnJavaType = TypeFactory.defaultInstance().constructType(returnType);
			
			return mapper.readValue(returnJsonParser, returnJavaType);
		}

		// no return type
		return null;
	}

	/**
	 * Returns the {@link ObjectMapper} that the client
	 * is using for JSON marshalling.
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
	 *
	 */
	public interface RequestListener {

		/**
		 * Called before a request is sent to the
		 * server end-point.  Modifications can be
		 * made to the request before it's sent.
		 * @param client the {@link JsonRpcClient}
		 * @param request the request {@link ObjectNode}
		 */
		void onBeforeRequestSent(JsonRpcClient client, ObjectNode request);

		/**
		 * Called after a response has been returned and
		 * successfully parsed but before it has been
		 * processed and turned into java objects.
		 * @param client the {@link JsonRpcClient}
		 * @param response the response {@link ObjectNode}
		 */
		void onBeforeResponseProcessed(JsonRpcClient client, ObjectNode response);
	}

}
