package com.googlecode.jsonrpc4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.pool.BasicNIOConnFactory;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.nio.protocol.BasicAsyncRequestProducer;
import org.apache.http.nio.protocol.BasicAsyncResponseConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestExecutor;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Implements an asynchronous JSON-RPC 2.0 HTTP client. This class has a
 * dependency on Apache Commons Codec, Apache
 * 
 * Because this implementation uses an HTTP request pool, timeouts are
 * controlled at a global level, rather than per-request.
 * <p>
 * The following JVM system properties control the behavior of the pool:
 * <ul>
 * <li>com.googlecode.jsonrpc4j.async.socket.timeout - overall socket idle
 * (keep-alive) timeout in milliseconds, default is 30 seconds</li>
 * <li>com.googlecode.jsonrpc4j.async.connect.timeout - socket connect timeout
 * in milliseconds, default is 30 seconds</li>
 * <li>com.googlecode.jsonrpc4j.async.socket.buffer - socket buffer size in
 * bytes, default is 8Kb (8192 bytes)</li>
 * <li>com.googlecode.jsonrpc4j.async.tcp.nodelay - true to use TCP_NODELAY,
 * false to disable, default is true
 * <li>com.googlecode.jsonrpc4j.async.max.inflight.route - maximum number of
 * in-flight requests per route (unique URL, minus query string), default is 500
 * </li>
 * <li>com.googlecode.jsonrpc4j.async.max.inflight.total - maximum number of
 * total in-flight requests (across all providers), default is 500</li>
 * <li>com.googlecode.jsonrpc4j.async.reactor.threads - number of asynchronous
 * IO reactor threads, default is 2 (more than sufficient for most clients)</li>
 * </ul>
 * 
 * @author Brett Wooldridge
 */
public class JsonRpcHttpAsyncClient {
	private static final Logger LOGGER = Logger
			.getLogger(JsonRpcHttpAsyncClient.class.getName());

	private static final String JSON_RPC_VERSION = "2.0";

	private static HttpAsyncRequester requester;
	private static BasicNIOConnPool pool;
	private static SSLContext sslContext;

	private static AtomicBoolean initialized = new AtomicBoolean();

	private static AtomicLong nextId = new AtomicLong();

	private ExceptionResolver exceptionResolver = DefaultExceptionResolver.INSTANCE;
	private Map<String, String> headers = new HashMap<String, String>();
	private ObjectMapper mapper;
	private URL serviceUrl;

	// Instance initializer
	{
		initialize();
	}

	/**
	 * Creates the {@link JsonRpcHttpAsyncClient} bound to the given
	 * {@code serviceUrl}.
	 * 
	 * @param serviceUrl
	 *            the service end-point URL
	 */
	public JsonRpcHttpAsyncClient(URL serviceUrl) {
		this(new ObjectMapper(), serviceUrl, new HashMap<String, String>());
	}

	/**
	 * Creates the {@link JsonRpcHttpAsyncClient} bound to the given
	 * {@code serviceUrl}. The headers provided in the {@code headers} map are
	 * added to every request made to the {@code serviceUrl}.
	 * 
	 * @param serviceUrl
	 *            the service end-point URL
	 * @param headers
	 *            the headers
	 */
	public JsonRpcHttpAsyncClient(URL serviceUrl, Map<String, String> headers) {
		this(new ObjectMapper(), serviceUrl, headers);
	}

	/**
	 * Creates the {@link JsonRpcHttpAsyncClient} using the specified
	 * {@code ObjectMapper} and bound to the given {@code serviceUrl}. The
	 * headers provided in the {@code headers} map are added to every request
	 * made to the {@code serviceUrl}.
	 * 
	 * @param mapper
	 *            the {@link ObjectMapper} to use for json<->java conversion
	 * @param serviceUrl
	 *            the service end-point URL
	 * @param headers
	 *            the headers
	 */
	public JsonRpcHttpAsyncClient(ObjectMapper mapper, URL serviceUrl,
			Map<String, String> headers) {
		this.mapper = mapper;
		this.serviceUrl = serviceUrl;
		this.headers.putAll(headers);
	}

	/**
	 * Set the SSLContext to be used to create SSL connections. This method most
	 * be called before the first {@code JsonRpcHttpAsyncClient} is constructed,
	 * otherwise it has no effect.
	 * 
	 * @param sslContext
	 *            the {@code SSLContext to use}
	 */
	public static void setSSLContext(SSLContext sslContext) {
		JsonRpcHttpAsyncClient.sslContext = sslContext;
	}

	/**
	 * Invokes the given method with the given arguments and returns
	 * immediately. The {@code Future} object that is returned can be used to
	 * retrieve the result.
	 * 
	 * @param methodName
	 *            the name of the method to invoke
	 * @param argument
	 *            the arguments to the method
	 * @return the response {@code Future<T>}
	 */
	public Future<Object> invoke(String methodName, Object argument) {
		return invoke(methodName, argument, Object.class,
				new HashMap<String, String>());
	}

	/**
	 * Invokes the given method with the given arguments and returns
	 * immediately. The {@code Future<T>} object that is returned can be used to
	 * retrieve the result.
	 * 
	 * @param methodName
	 *            the name of the method to invoke
	 * @param argument
	 *            the arguments to the method
	 * @param returnType
	 *            the return type
	 * @return the response {@code Future<T>}
	 */
	public <T> Future<T> invoke(String methodName, Object argument,
			Class<T> returnType) {
		return invoke(methodName, argument, returnType,
				new HashMap<String, String>());
	}

	/**
	 * Invokes the given method with the given arguments and returns
	 * immediately. The {@code extraHeaders} are added to the request. The
	 * {@code Future<T>} object that is returned can be used to retrieve the
	 * result.
	 * 
	 * @param methodName
	 *            the name of the method to invoke
	 * @param arguments
	 *            the arguments to the method
	 * @param returnType
	 *            the return type
	 * @param extraHeaders
	 *            extra headers to add to the request
	 * @return the response {@code Future<T>}
	 */
	public <T> Future<T> invoke(String methodName, Object argument,
			Class<T> returnType, Map<String, String> extraHeaders) {
		return doInvoke(methodName, argument, returnType, extraHeaders,
				new JsonRpcFuture<T>());
	}

	/**
	 * Invokes the given method with the given arguments and invokes the
	 * {@code JsonRpcCallback} with the result.
	 * 
	 * @param methodName
	 *            the name of the method to invoke
	 * @param argument
	 *            the arguments to the method
	 * @param callback
	 *            the {@code JsonRpcCallback}
	 */
	public void invoke(String methodName, Object argument,
			JsonRpcCallback<Object> callback) {
		invoke(methodName, argument, Object.class,
				new HashMap<String, String>(), callback);
	}

	/**
	 * Invokes the given method with the given arguments and invokes the
	 * {@code JsonRpcCallback} with the result cast to the given
	 * {@code returnType}, or null if void.
	 * 
	 * @param methodName
	 *            the name of the method to invoke
	 * @param argument
	 *            the arguments to the method
	 * @param returnType
	 *            the return type
	 * @param callback
	 *            the {@code JsonRpcCallback}
	 */
	public <T> void invoke(String methodName, Object argument,
			Class<T> returnType, JsonRpcCallback<T> callback) {
		invoke(methodName, argument, returnType, new HashMap<String, String>(),
				callback);
	}

	/**
	 * Invokes the given method with the given arguments and invokes the
	 * {@code JsonRpcCallback} with the result cast to the given
	 * {@code returnType}, or null if void. The {@code extraHeaders} are added
	 * to the request.
	 * 
	 * @param methodName
	 *            the name of the method to invoke
	 * @param arguments
	 *            the arguments to the method
	 * @param returnType
	 *            the return type
	 * @param extraHeaders
	 *            extra headers to add to the request
	 * @param callback
	 *            the {@code JsonRpcCallback}
	 */
	private <T> void invoke(String methodName, Object argument,
			Class<T> returnType, Map<String, String> extraHeaders,
			JsonRpcCallback<T> callback) {
		doInvoke(methodName, argument, returnType, extraHeaders, callback);
	}

	/**
	 * Invokes the given method with the given arguments and invokes the
	 * {@code JsonRpcCallback} with the result cast to the given
	 * {@code returnType}, or null if void. The {@code extraHeaders} are added
	 * to the request.
	 * 
	 * @param methodName
	 *            the name of the method to invoke
	 * @param arguments
	 *            the arguments to the method
	 * @param extraHeaders
	 *            extra headers to add to the request
	 * @param returnType
	 *            the return type
	 * @param callback
	 *            the {@code JsonRpcCallback}
	 */
	@SuppressWarnings("unchecked")
	private <T> Future<T> doInvoke(String methodName, Object argument,
			Class<T> returnType, Map<String, String> extraHeaders,
			JsonRpcCallback<T> callback) {

		String path = serviceUrl.getPath()
				+ (serviceUrl.getQuery() != null ? "?" + serviceUrl.getQuery()
						: "");
		int port = serviceUrl.getPort() != -1 ? serviceUrl.getPort()
				: serviceUrl.getDefaultPort();

		// create the HttpRequest
		HttpRequest request = new BasicHttpEntityEnclosingRequest("POST", path);

		addHeaders(request, headers);
		addHeaders(request, extraHeaders);

		// create the JSON payload
		try {
			writeRequest(methodName, argument, request);
		} catch (IOException e) {
			callback.onError(e);
		}

		HttpHost target = new HttpHost(serviceUrl.getHost(), port,
				serviceUrl.getProtocol());
		BasicAsyncRequestProducer asyncRequestProducer = new BasicAsyncRequestProducer(
				target, request);
		BasicAsyncResponseConsumer asyncResponseConsumer = new BasicAsyncResponseConsumer();

		RequestAsyncFuture<T> futureCallback = new RequestAsyncFuture<T>(
				returnType, callback);

		BasicHttpContext httpContext = new BasicHttpContext();
		requester.execute(asyncRequestProducer, asyncResponseConsumer, pool,
				httpContext, futureCallback);

		return (callback instanceof JsonRpcFuture ? (Future<T>) callback : null);
	}

	/**
	 * Writes a request.
	 * 
	 * @param methodName
	 *            the method name
	 * @param arguments
	 *            the arguments
	 * @param ops
	 *            the stream
	 * @param id
	 *            the optional id
	 * @throws IOException
	 *             on error
	 */
	private void writeRequest(String methodName, Object arguments,
			HttpRequest httpRequest) throws IOException {

		// create the request
		ObjectNode request = mapper.createObjectNode();

		request.put("id", nextId.getAndIncrement());

		// add protocol and method
		request.put("jsonrpc", JSON_RPC_VERSION);
		request.put("method", methodName);

		// object array args
		if (arguments != null && arguments.getClass().isArray()) {
			Object[] args = Object[].class.cast(arguments);
			if (args.length > 0) {
				request.put("params",
						mapper.valueToTree(Object[].class.cast(arguments)));
			}

			// collection args
		} else if (arguments != null && Collection.class.isInstance(arguments)) {
			if (!Collection.class.cast(arguments).isEmpty()) {
				request.put("params", mapper.valueToTree(arguments));
			}

			// map args
		} else if (arguments != null && Map.class.isInstance(arguments)) {
			if (!Map.class.cast(arguments).isEmpty()) {
				request.put("params", mapper.valueToTree(arguments));
			}

			// other args
		} else if (arguments != null) {
			request.put("params", mapper.valueToTree(arguments));
		}

		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.log(Level.FINE, "JSON-PRC Request: " + request.toString());
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
		mapper.writeValue(baos, request);

		HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) httpRequest;

		HttpEntity entity;
		if (entityRequest.getFirstHeader("Content-Type") == null) {
			// Set default content type if none is set.
			entity = new ByteArrayEntity(baos.toByteArray(),
					ContentType.APPLICATION_JSON);
		} else {
			entity = new ByteArrayEntity(baos.toByteArray());
		}

		entityRequest.setEntity(entity);
	}

	/**
	 * Reads a JSON-PRC response from the server. This blocks until a response
	 * is received.
	 * 
	 * @param returnType
	 *            the expected return type
	 * @param ips
	 *            the {@link InputStream} to read from
	 * @return the object returned by the JSON-RPC response
	 * @throws Throwable
	 *             on error
	 */
	private <T> T readResponse(Type returnType, InputStream ips)
			throws Throwable {

		// read the response
		JsonNode response = mapper.readTree(new NoCloseInputStream(ips));
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.log(Level.FINE, "JSON-PRC Response: " + response.toString());
		}

		// bail on invalid response
		if (!response.isObject()) {
			throw new JsonRpcClientException(0, "Invalid JSON-RPC response",
					response);
		}
		ObjectNode jsonObject = ObjectNode.class.cast(response);

		// detect errors
		if (jsonObject.has("error") && jsonObject.get("error") != null
				&& !jsonObject.get("error").isNull()) {

			// resolve and throw the exception
			if (exceptionResolver == null) {
				throw DefaultExceptionResolver.INSTANCE
						.resolveException(jsonObject);
			} else {
				throw exceptionResolver.resolveException(jsonObject);
			}
		}

		// convert it to a return object
		if (jsonObject.has("result") && !jsonObject.get("result").isNull()
				&& jsonObject.get("result") != null) {

			JsonParser returnJsonParser = mapper.treeAsTokens(jsonObject
					.get("result"));
			JavaType returnJavaType = TypeFactory.defaultInstance()
					.constructType(returnType);

			return mapper.readValue(returnJsonParser, returnJavaType);
		}

		// no return type
		return null;
	}

	/**
	 * Set the request headers.
	 * 
	 * @param request
	 *            the request object
	 * @param headers
	 */
	private void addHeaders(HttpRequest request, Map<String, String> headers) {
		for (String key : headers.keySet()) {
			request.addHeader(key, headers.get(key));
		}
	}

	private void initialize() {
		if (initialized.getAndSet(true)) {
			return;
		}

		// HTTP parameters for the client
		final HttpParams params = new BasicHttpParams();
		params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, Integer
				.getInteger("com.googlecode.jsonrpc4j.async.socket.timeout",
						30000));
		params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, Integer
				.getInteger("com.googlecode.jsonrpc4j.async.connect.timeout",
						30000));
		params.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, Integer
				.getInteger("com.googlecode.jsonrpc4j.async.socket.buffer",
						8 * 1024));
		params.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, Boolean
				.valueOf(System.getProperty(
						"com.googlecode.jsonrpc4j.async.tcp.nodelay", "true")));
		params.setParameter(CoreProtocolPNames.USER_AGENT, "jsonrpc4j/1.0");

		// Create client-side I/O reactor
		final ConnectingIOReactor ioReactor;
		try {
			IOReactorConfig config = new IOReactorConfig();
			config.setIoThreadCount(Integer.getInteger(
					"com.googlecode.jsonrpc4j.async.reactor.threads", 1));
			ioReactor = new DefaultConnectingIOReactor(config);
		} catch (IOReactorException e) {
			throw new RuntimeException(
					"Exception initializing asynchronous Apache HTTP Client", e);
		}

		// Create a default SSLSetupHandler that accepts any certificate
		if (sslContext == null) {
			try {
				sslContext = SSLContext.getDefault();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		// Create HTTP connection pool
		BasicNIOConnFactory nioConnFactory = new BasicNIOConnFactory(
				sslContext, null, params);
		pool = new BasicNIOConnPool(ioReactor, nioConnFactory, params);
		// Limit total number of connections to 500 by default
		pool.setDefaultMaxPerRoute(Integer.getInteger(
				"com.googlecode.jsonrpc4j.async.max.inflight.route", 500));
		pool.setMaxTotal(Integer.getInteger(
				"com.googlecode.jsonrpc4j.async.max.inflight.total", 500));

		// Run the I/O reactor in a separate thread
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					// Create client-side HTTP protocol handler
					HttpAsyncRequestExecutor protocolHandler = new HttpAsyncRequestExecutor();
					// Create client-side I/O event dispatch
					IOEventDispatch ioEventDispatch = new DefaultHttpClientIODispatch(
							protocolHandler, sslContext, params);
					// Ready to go!
					ioReactor.execute(ioEventDispatch);
				} catch (InterruptedIOException ex) {
					System.err.println("Interrupted");
				} catch (IOException e) {
					System.err.println("I/O error: " + e.getMessage());
				}
			}

		}, "jsonrpc4j HTTP IOReactor");
		// Start the client thread
		t.setDaemon(true);
		t.start();

		// Create HTTP protocol processing chain
		HttpProcessor httpproc = new ImmutableHttpProcessor(
				new HttpRequestInterceptor[] {
						// Use standard client-side protocol interceptors
						new RequestContent(), new RequestTargetHost(),
						new RequestConnControl(), new RequestUserAgent(),
						new RequestExpectContinue() });

		// Create HTTP requester
		requester = new HttpAsyncRequester(httpproc,
				new DefaultConnectionReuseStrategy(), params);
	}

	/**
	 * Private class to handle the HttpResponse callback.
	 * 
	 * @param <T>
	 */
	private class RequestAsyncFuture<T> implements FutureCallback<HttpResponse> {
		private JsonRpcCallback<T> callBack;
		private Class<T> type;

		RequestAsyncFuture(Class<T> type, JsonRpcCallback<T> callBack) {
			this.type = type;
			this.callBack = callBack;
		}

		public void completed(final HttpResponse response) {
			try {
				StatusLine statusLine = response.getStatusLine();
				int statusCode = statusLine.getStatusCode();

				InputStream stream;
				if (statusCode == 200) {
					HttpEntity entity = response.getEntity();
					try {
						stream = entity.getContent();
					} catch (Exception e) {
						failed(e);
						return;
					}

					callBack.onComplete(type.cast(readResponse(type, stream)));
				} else {
					callBack.onError(new RuntimeException(
							"Unexpected response code: " + statusCode));
				}
			} catch (Throwable t) {
				callBack.onError(t);
			}
		}

		public void failed(final Exception ex) {
			callBack.onError(ex);
		}

		public void cancelled() {
			callBack.onError(new RuntimeException("HTTP Request was cancelled"));
		}
	}

	private class JsonRpcFuture<T> implements Future<T>, JsonRpcCallback<T> {

		private T object;
		private boolean done;
		private ExecutionException exception;

		// ---------------------------------------------------------------
		// Future methods
		// ---------------------------------------------------------------

		public synchronized boolean isDone() {
			return done;
		}

		public synchronized T get() throws InterruptedException,
				ExecutionException {
			while (!done) {
				this.wait();
			}

			if (exception != null) {
				throw exception;
			}

			return object;
		}

		public synchronized T get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException,
				TimeoutException {
			while (!done) {
				this.wait(unit.toMillis(timeout));
			}

			if (exception != null) {
				throw exception;
			}

			return object;
		}

		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		public boolean isCancelled() {
			return false;
		}

		// ---------------------------------------------------------------
		// JsonRpcCallback methods
		// ---------------------------------------------------------------

		public synchronized void onComplete(T result) {
			object = result;
			done = true;
			this.notify();
		}

		public synchronized void onError(Throwable t) {
			exception = new ExecutionException(t);
			done = true;
			this.notify();
		}
	}
}
