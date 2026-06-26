package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
 * Implements an asynchronous JSON-RPC 2.0 HTTP client.
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
	private static CloseableHttpAsyncClient httpClient;
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
	 * @param mapper            the {@link ObjectMapper} to use for json&lt;-&gt;java conversion
	 * @param exceptionResolver the {@link ExceptionResolver} translating remote exceptions.
	 * @param serviceUrl        the service end-point URL
	 * @param headers           the headers
	 */
	public JsonRpcHttpAsyncClient(ObjectMapper mapper, ExceptionResolver exceptionResolver, URL serviceUrl, Map<String, String> headers) {
		this.mapper = mapper;
		this.serviceUrl = serviceUrl;
		this.headers.putAll(headers);
		this.exceptionResolver = exceptionResolver;

		if (this.exceptionResolver == null) {
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
	 * Set the SSLContext to be used to create SSL connections. This method must
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

		String url = serviceUrl.toExternalForm();

		byte[] requestBody;
		try {
			requestBody = writeRequest(methodName, argument);
		} catch (IOException e) {
			callback.onError(e);
			return (callback instanceof JsonRpcFuture ? (Future<T>) callback : null);
		}

		SimpleRequestBuilder requestBuilder = SimpleRequestBuilder.post(url)
				.setBody(requestBody, ContentType.APPLICATION_JSON);

		for (Map.Entry<String, String> entry : headers.entrySet()) {
			requestBuilder.addHeader(entry.getKey(), entry.getValue());
		}
		for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
			requestBuilder.addHeader(entry.getKey(), entry.getValue());
		}

		SimpleHttpRequest request = requestBuilder.build();

		httpClient.execute(request, new FutureCallback<SimpleHttpResponse>() {
			@Override
			public void completed(SimpleHttpResponse response) {
				try {
					int statusCode = response.getCode();
					if (statusCode == 200) {
						byte[] body = response.getBodyBytes();
						InputStream stream = new ByteArrayInputStream(body);
						callback.onComplete(returnType.cast(readResponse(returnType, stream)));
					} else {
						callback.onError(new RuntimeException("Unexpected response code: " + statusCode));
					}
				} catch (Throwable t) {
					callback.onError(t);
				}
			}

			@Override
			public void failed(Exception ex) {
				callback.onError(ex);
			}

			@Override
			public void cancelled() {
				callback.onError(new RuntimeException("HTTP Request was cancelled"));
			}
		});

		return (callback instanceof JsonRpcFuture ? (Future<T>) callback : null);
	}

	/**
	 * Writes a request and returns the serialized bytes.
	 *
	 * @param methodName the method name
	 * @param arguments  the arguments
	 * @return serialized JSON-RPC request body
	 */
	private byte[] writeRequest(String methodName, Object arguments) throws IOException {

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
		return byteArrayOutputStream.toByteArray();
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
	 * Reads a JSON-RPC response from the server.
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

		createSslContext();

		int socketTimeout = Integer.getInteger("com.googlecode.jsonrpc4j.async.socket.timeout", 30000);
		int connectTimeout = Integer.getInteger("com.googlecode.jsonrpc4j.async.connect.timeout", 30000);
		boolean tcpNoDelay = Boolean.parseBoolean(System.getProperty("com.googlecode.jsonrpc4j.async.tcp.nodelay", "true"));
		int ioThreadCount = Integer.getInteger("com.googlecode.jsonrpc4j.async.reactor.threads", 1);
		int maxPerRoute = Integer.getInteger("com.googlecode.jsonrpc4j.async.max.inflight.route", 500);
		int maxTotal = Integer.getInteger("com.googlecode.jsonrpc4j.async.max.inflight.total", 500);
		int socketBufferSize = Integer.getInteger("com.googlecode.jsonrpc4j.async.socket.buffer", 8 * 1024);

		PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
				.setTlsStrategy(new DefaultClientTlsStrategy(sslContext))
				.setDefaultTlsConfig(TlsConfig.custom()
						.setVersionPolicy(HttpVersionPolicy.NEGOTIATE)
						.build())
				.setDefaultConnectionConfig(ConnectionConfig.custom()
						.setConnectTimeout(Timeout.ofMilliseconds(connectTimeout))
						.setSocketTimeout(Timeout.ofMilliseconds(socketTimeout))
						.build())
				.setMaxConnPerRoute(maxPerRoute)
				.setMaxConnTotal(maxTotal)
				.build();

		IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
				.setSoTimeout(Timeout.ofMilliseconds(socketTimeout))
				.setTcpNoDelay(tcpNoDelay)
				.setIoThreadCount(ioThreadCount)
				.setSndBufSize(socketBufferSize)
				.setRcvBufSize(socketBufferSize)
				.build();

		httpClient = HttpAsyncClients.custom()
				.setConnectionManager(connectionManager)
				.setIOReactorConfig(ioReactorConfig)
				.build();

		httpClient.start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (httpClient != null) {
				httpClient.close(CloseMode.GRACEFUL);
			}
		}, "jsonrpc4j-async-shutdown"));
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

		public T get() throws InterruptedException, ExecutionException {
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
				throws InterruptedException, ExecutionException, TimeoutException {
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
}
