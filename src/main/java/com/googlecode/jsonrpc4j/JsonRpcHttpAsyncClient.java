package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
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
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.ERROR;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.ID;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.JSONRPC;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.METHOD;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.PARAMS;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.RESULT;

/**
 * Implements an asynchronous JSON-RPC 2.0 HTTP client. This class has a
 * dependency on Apache Commons Codec, Apache
 * <p>
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
@SuppressWarnings({"WeakerAccess", "unused"})
public class JsonRpcHttpAsyncClient {
	
	private static final Logger logger = LoggerFactory.getLogger(JsonRpcHttpAsyncClient.class);
	
	private static final AtomicBoolean initialized = new AtomicBoolean();
	private static final AtomicLong nextId = new AtomicLong();
	private static HttpAsyncRequester requester;
	private static BasicNIOConnPool pool;
	private static SSLContext sslContext;
	private final ExceptionResolver exceptionResolver;
	private final Map<String, String> headers = new HashMap<>();
	private final ObjectMapper mapper;
	private final URL serviceUrl;
	
	{
		initialize();
	}
	
	/**
	 * Creates the {@link JsonRpcHttpAsyncClient} bound to the given {@code serviceUrl}.
	 *
	 * @param serviceUrl the service end-point URL
	 */
	public JsonRpcHttpAsyncClient(URL serviceUrl) {
		this(new ObjectMapper(), serviceUrl, new HashMap<String, String>());
	}
	
	/**
	 * Creates the {@link JsonRpcHttpAsyncClient} using the specified {@code ObjectMapper} and bound to the given
	 * {@code serviceUrl}. The headers provided in the {@code headers} map are added to every request
	 * made to the {@code serviceUrl}.
	 *
	 * @param mapper     the {@link ObjectMapper} to use for json&lt;-&gt;java conversion
	 * @param serviceUrl the service end-point URL
	 * @param headers    the headers
	 */
	public JsonRpcHttpAsyncClient(ObjectMapper mapper, URL serviceUrl, Map<String, String> headers) {
		this(mapper, DefaultExceptionResolver.INSTANCE, serviceUrl, headers);
	}

	/**
	 * Creates the {@link JsonRpcHttpAsyncClient} using the specified
	 * {@link ObjectMapper} and {@link ExceptionResolver}, bound to the given
	 * {@code serviceUrl}. The headers provided in the {@code headers} map are
	 * added to every request made to the {@code serviceUrl}.
	 * The {@link ExceptionResolver} can not be null.
	 *
	 * @param mapper     the {@link ObjectMapper} to use for json&lt;-&gt;java conversion
	 * @param exceptionResolver the {@link ExceptionResolver} translating remote exceptions.
	 * @param serviceUrl the service end-point URL
	 * @param headers    the headers
	 */
	public JsonRpcHttpAsyncClient(ObjectMapper mapper, ExceptionResolver exceptionResolver, URL serviceUrl, Map<String, String> headers) {
		this.mapper = mapper;
		this.serviceUrl = serviceUrl;
		this.headers.putAll(headers);
		this.exceptionResolver = exceptionResolver;

		if(this.exceptionResolver == null) {
			throw new IllegalArgumentException("ExceptionResolver can not be null");
		}
	}

	/**
	 * Creates the {@link JsonRpcHttpAsyncClient} bound to the given
	 * {@code serviceUrl}. The headers provided in the {@code headers} map are
	 * added to every request made to the {@code serviceUrl}.
	 *
	 * @param serviceUrl the service end-point URL
	 * @param headers    the headers
	 */
	public JsonRpcHttpAsyncClient(URL serviceUrl, Map<String, String> headers) {
		this(new ObjectMapper(), serviceUrl, headers);
	}
	
	/**
	 * Set the SSLContext to be used to create SSL connections. This method most
	 * be called before the first {@code JsonRpcHttpAsyncClient} is constructed,
	 * otherwise it has no effect.
	 *
	 * @param sslContext the {@code SSLContext to use}
	 */
	public static void setSSLContext(SSLContext sslContext) {
		JsonRpcHttpAsyncClient.sslContext = sslContext;
	}
	
	/**
	 * Invokes the given method with the given arguments and returns
	 * immediately. The {@code Future} object that is returned can be used to
	 * retrieve the result.
	 *
	 * @param methodName the name of the method to invoke
	 * @param argument   the arguments to the method
	 * @return the response {@code Future<T>}
	 */
	public Future<Object> invoke(String methodName, Object argument) {
		return invoke(methodName, argument, Object.class, new HashMap<String, String>());
	}
	
	/**
	 * Invokes the given method with the given arguments and returns
	 * immediately. The {@code extraHeaders} are added to the request. The
	 * {@code Future<T>} object that is returned can be used to retrieve the
	 * result.
	 *
	 * @param methodName   the name of the method to invoke
	 * @param argument     the argument to the method
	 * @param returnType   the return type
	 * @param extraHeaders extra headers to add to the request
	 * @param <T>          the return type
	 * @return the response {@code Future<T>}
	 */
	private <T> Future<T> invoke(String methodName, Object argument, Class<T> returnType, Map<String, String> extraHeaders) {
		return doInvoke(methodName, argument, returnType, extraHeaders, new JsonRpcFuture<T>());
	}
	
	/**
	 * Invokes the given method with the given arguments and invokes the
	 * {@code JsonRpcCallback} with the result cast to the given
	 * {@code returnType}, or null if void. The {@code extraHeaders} are added
	 * to the request.
	 *
	 * @param methodName   the name of the method to invoke
	 * @param argument     the arguments to the method
	 * @param extraHeaders extra headers to add to the request
	 * @param returnType   the return type
	 * @param callback     the {@code JsonRpcCallback}
	 */
	@SuppressWarnings("unchecked")
	private <T> Future<T> doInvoke(String methodName, Object argument, Class<T> returnType, Map<String, String> extraHeaders, JsonRpcCallback<T> callback) {
		
		String path = serviceUrl.getPath() + (serviceUrl.getQuery() != null ? "?" + serviceUrl.getQuery() : "");
		int port = serviceUrl.getPort() != -1 ? serviceUrl.getPort() : serviceUrl.getDefaultPort();
		HttpRequest request = new BasicHttpEntityEnclosingRequest("POST", path);
		
		addHeaders(request, headers);
		addHeaders(request, extraHeaders);
		
		try {
			writeRequest(methodName, argument, request);
		} catch (IOException e) {
			callback.onError(e);
		}
		
		HttpHost target = new HttpHost(serviceUrl.getHost(), port, serviceUrl.getProtocol());
		BasicAsyncRequestProducer asyncRequestProducer = new BasicAsyncRequestProducer(target, request);
		BasicAsyncResponseConsumer asyncResponseConsumer = new BasicAsyncResponseConsumer();
		
		RequestAsyncFuture<T> futureCallback = new RequestAsyncFuture<>(returnType, callback);
		
		BasicHttpContext httpContext = new BasicHttpContext();
		requester.execute(asyncRequestProducer, asyncResponseConsumer, pool, httpContext, futureCallback);
		
		return (callback instanceof JsonRpcFuture ? (Future<T>) callback : null);
	}
	
	/**
	 * Set the request headers.
	 *
	 * @param request the request object
	 * @param headers to be used
	 */
	private void addHeaders(HttpRequest request, Map<String, String> headers) {
		for (Map.Entry<String, String> key : headers.entrySet()) {
			request.addHeader(key.getKey(), key.getValue());
		}
	}
	
	/**
	 * Writes a request.
	 *
	 * @param methodName  the method name
	 * @param arguments   the arguments
	 * @param httpRequest the stream on error
	 */
	private void writeRequest(String methodName, Object arguments, HttpRequest httpRequest) throws IOException {
		
		ObjectNode request = mapper.createObjectNode();
		request.put(ID, nextId.getAndIncrement());
		request.put(JSONRPC, JsonRpcBasicServer.VERSION);
		request.put(METHOD, methodName);
		
		if (arguments != null && arguments.getClass().isArray()) {
			Object[] args = Object[].class.cast(arguments);
			if (args.length > 0) {
				request.set(PARAMS, mapper.valueToTree(Object[].class.cast(arguments)));
			}
		} else if (arguments != null && Collection.class.isInstance(arguments)) {
			if (!Collection.class.cast(arguments).isEmpty()) {
				request.set(PARAMS, mapper.valueToTree(arguments));
			}
		} else if (arguments != null && Map.class.isInstance(arguments)) {
			if (!Map.class.cast(arguments).isEmpty()) {
				request.set(PARAMS, mapper.valueToTree(arguments));
			}
		} else if (arguments != null) {
			request.set(PARAMS, mapper.valueToTree(arguments));
		}
		
		logger.debug("JSON-RPC Request: {}", request);
		
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(512);
		mapper.writeValue(byteArrayOutputStream, request);
		HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) httpRequest;
		
		HttpEntity entity;
		if (entityRequest.getFirstHeader("Content-Type") == null) {
			entity = new ByteArrayEntity(byteArrayOutputStream.toByteArray(), ContentType.APPLICATION_JSON);
		} else {
			entity = new ByteArrayEntity(byteArrayOutputStream.toByteArray());
		}
		entityRequest.setEntity(entity);
	}
	
	/**
	 * Invokes the given method with the given arguments and returns
	 * immediately. The {@code Future<T>} object that is returned can be used to
	 * retrieve the result.
	 *
	 * @param methodName the name of the method to invoke
	 * @param argument   the arguments to the method
	 * @param returnType the return type
	 * @param <T>        the return type
	 * @return the response {@code Future<T>}
	 */
	public <T> Future<T> invoke(String methodName, Object argument, Class<T> returnType) {
		return invoke(methodName, argument, returnType, new HashMap<String, String>());
	}
	
	/**
	 * Invokes the given method with the given arguments and invokes the
	 * {@code JsonRpcCallback} with the result.
	 *
	 * @param methodName the name of the method to invoke
	 * @param argument   the arguments to the method
	 * @param callback   the {@code JsonRpcCallback}
	 */
	public void invoke(String methodName, Object argument, JsonRpcCallback<Object> callback) {
		invoke(methodName, argument, Object.class, new HashMap<String, String>(), callback);
	}
	
	/**
	 * Invokes the given method with the given arguments and invokes the
	 * {@code JsonRpcCallback} with the result cast to the given
	 * {@code returnType}, or null if void. The {@code extraHeaders} are added
	 * to the request.
	 *
	 * @param methodName   the name of the method to invoke
	 * @param argument     the arguments to the method
	 * @param returnType   the return type
	 * @param extraHeaders extra headers to add to the request
	 * @param callback     the {@code JsonRpcCallback}
	 */
	private <T> void invoke(String methodName, Object argument, Class<T> returnType, Map<String, String> extraHeaders, JsonRpcCallback<T> callback) {
		doInvoke(methodName, argument, returnType, extraHeaders, callback);
	}
	
	/**
	 * Invokes the given method with the given arguments and invokes the
	 * {@code JsonRpcCallback} with the result cast to the given
	 * {@code returnType}, or null if void.
	 *
	 * @param methodName the name of the method to invoke
	 * @param argument   the arguments to the method
	 * @param returnType the return type
	 * @param <T>        the return type
	 * @param callback   the {@code JsonRpcCallback}
	 */
	public <T> void invoke(String methodName, Object argument, Class<T> returnType, JsonRpcCallback<T> callback) {
		invoke(methodName, argument, returnType, new HashMap<String, String>(), callback);
	}
	
	/**
	 * Reads a JSON-RPC response from the server. This blocks until a response
	 * is received.
	 *
	 * @param returnType the expected return type
	 * @param ips        the {@link InputStream} to read from
	 * @return the object returned by the JSON-RPC response
	 * @throws Throwable on error
	 */
	private <T> T readResponse(Type returnType, InputStream ips) throws Throwable {
		JsonNode response = mapper.readTree(new NoCloseInputStream(ips));
		logger.debug("JSON-RPC Response: {}", response);
		if (!response.isObject()) {
			throw new JsonRpcClientException(0, "Invalid JSON-RPC response", response);
		}
		ObjectNode jsonObject = ObjectNode.class.cast(response);
		
		if (jsonObject.has(ERROR) && jsonObject.get(ERROR) != null && !jsonObject.get(ERROR).isNull()) {
			throw exceptionResolver.resolveException(jsonObject);
		}
		if (jsonObject.has(RESULT) && !jsonObject.get(RESULT).isNull() && jsonObject.get(RESULT) != null) {
			
			JsonParser returnJsonParser = mapper.treeAsTokens(jsonObject.get(RESULT));
			JavaType returnJavaType = mapper.getTypeFactory().constructType(returnType);
			
			return mapper.readValue(returnJsonParser, returnJavaType);
		}
		return null;
	}
	
	private void initialize() {
		if (initialized.getAndSet(true)) {
			return;
		}
		IOReactorConfig.Builder config = createConfig();
		// params.setParameter(CoreProtocolPNames.USER_AGENT, "jsonrpc4j/1.0");
		final ConnectingIOReactor ioReactor = createIoReactor(config);
		createSslContext();
		int socketBufferSize = Integer.getInteger("com.googlecode.jsonrpc4j.async.socket.buffer", 8 * 1024);
		final ConnectionConfig connectionConfig = ConnectionConfig.custom().setBufferSize(socketBufferSize).build();
		BasicNIOConnFactory nioConnFactory = new BasicNIOConnFactory(sslContext, null, connectionConfig);
		pool = new BasicNIOConnPool(ioReactor, nioConnFactory, Integer.getInteger("com.googlecode.jsonrpc4j.async.connect.timeout", 30000));
		pool.setDefaultMaxPerRoute(Integer.getInteger("com.googlecode.jsonrpc4j.async.max.inflight.route", 500));
		pool.setMaxTotal(Integer.getInteger("com.googlecode.jsonrpc4j.async.max.inflight.total", 500));
		
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					HttpAsyncRequestExecutor protocolHandler = new HttpAsyncRequestExecutor();
					IOEventDispatch ioEventDispatch = new DefaultHttpClientIODispatch(protocolHandler, sslContext, connectionConfig);
					ioReactor.execute(ioEventDispatch);
				} catch (InterruptedIOException ex) {
					System.err.println("Interrupted");
				} catch (IOException e) {
					System.err.println("I/O error: " + e.getMessage());
				}
			}
		}, "jsonrpc4j HTTP IOReactor");
		
		t.setDaemon(true);
		t.start();
		
		HttpProcessor httpProcessor = new ImmutableHttpProcessor(new RequestContent(), new RequestTargetHost(), new RequestConnControl(), new RequestUserAgent(), new RequestExpectContinue(false));
		requester = new HttpAsyncRequester(httpProcessor, new DefaultConnectionReuseStrategy());
	}
	
	private IOReactorConfig.Builder createConfig() {
		IOReactorConfig.Builder config = IOReactorConfig.custom();
		config = config.setSoTimeout(Integer.getInteger("com.googlecode.jsonrpc4j.async.socket.timeout", 30000));
		config = config.setConnectTimeout(Integer.getInteger("com.googlecode.jsonrpc4j.async.connect.timeout", 30000));
		config = config.setTcpNoDelay(Boolean.valueOf(System.getProperty("com.googlecode.jsonrpc4j.async.tcp.nodelay", "true")));
		config = config.setIoThreadCount(Integer.getInteger("com.googlecode.jsonrpc4j.async.reactor.threads", 1));
		return config;
	}
	
	private ConnectingIOReactor createIoReactor(IOReactorConfig.Builder config) {
		final ConnectingIOReactor ioReactor;
		try {
			ioReactor = new DefaultConnectingIOReactor(config.build());
		} catch (IOReactorException e) {
			throw new RuntimeException("Exception initializing asynchronous Apache HTTP Client", e);
		}
		return ioReactor;
	}
	
	private void createSslContext() {
		if (sslContext == null) {
			try {
				sslContext = SSLContext.getDefault();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private static class JsonRpcFuture<T> implements Future<T>, JsonRpcCallback<T> {
		
		private T object;
		private volatile boolean done;
		private ExecutionException exception;

		private final Lock lock = new ReentrantLock();

		private final Condition condition = lock.newCondition();
		
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}
		
		public boolean isCancelled() {
			return false;
		}
		
		public boolean isDone() {
			return done;
		}
		
		public T get() throws InterruptedException,
				ExecutionException {

			lock.lock();
			try {

				while (!done) {
					condition.await();
				}

				if (exception != null) {
					throw exception;
				}

				return object;
			} finally {
				lock.unlock();
			}
		}
		
		public T get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException,
				TimeoutException {

			if (timeout <= 0) {
				throw new TimeoutException();
			}

			long nanos = unit.toNanos(timeout);
			lock.lock();
			try {
				while (!done) {
					if (nanos <= 0) {
						throw new TimeoutException();
					}
					nanos = condition.awaitNanos(nanos);
				}

				if (exception != null) {
					throw exception;
				}
				return object;
			} finally {
				lock.unlock();
			}

		}
		
		public void onComplete(T result) {
			lock.lock();
			try {
				object = result;
				done = true;
				condition.signal();
			} finally {
				lock.unlock();
			}
		}
		
		public void onError(Throwable t) {
			lock.lock();
			try {
				exception = new ExecutionException(t);
				done = true;
				condition.signal();
			} finally {
				lock.unlock();
			}
		}
	}
	
	/**
	 * Private class to handleRequest the HttpResponse callback.
	 *
	 * @param <T>
	 */
	private class RequestAsyncFuture<T> implements FutureCallback<HttpResponse> {
		private final JsonRpcCallback<T> callBack;
		private final Class<T> type;
		
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
}
