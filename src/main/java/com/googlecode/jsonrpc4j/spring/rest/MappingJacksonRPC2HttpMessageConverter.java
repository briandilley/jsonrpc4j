package com.googlecode.jsonrpc4j.spring.rest;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * JSON-RPC Message converter for Spring RestTemplate
 */
@SuppressWarnings({"WeakerAccess", "unused"})
class MappingJacksonRPC2HttpMessageConverter extends AbstractHttpMessageConverter<Object> {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	public static final MediaType APPLICATION_JSON_RPC = new MediaType("application", "json-rpc", DEFAULT_CHARSET);

	private ObjectMapper objectMapper;

	private boolean prefixJson = false;

	/**
	 * Construct a new {@code BindingJacksonHttpMessageConverter}.
	 */
	public MappingJacksonRPC2HttpMessageConverter() {
		super(APPLICATION_JSON_RPC);
		objectMapper = new ObjectMapper();
	}

	/**
	 * Construct a new {@code BindingJacksonHttpMessageConverter}.
	 *
	 * @param objectMapper the object mapper for this view
	 */
	public MappingJacksonRPC2HttpMessageConverter(ObjectMapper objectMapper) {
		super(APPLICATION_JSON_RPC);
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.objectMapper = objectMapper;
	}

	/**
	 * Return the underlying {@code ObjectMapper} for this view.
	 *
	 * @return the object mapper for this view
	 */
	public ObjectMapper getObjectMapper() {
		return this.objectMapper;
	}

	/**
	 * Set the {@code ObjectMapper} for this view. If not set, a default
	 * {@link ObjectMapper#ObjectMapper() ObjectMapper} is used.
	 * <p>
	 * Setting a custom-configured {@code ObjectMapper} is one way to take further control of the JSON serialization
	 * process. For example, an extended {@code org.codehaus.jackson.map.SerializerFactory} can be configured that
	 * provides custom serializers for specific types. The other option for refining the serialization process is to use
	 * Jackson's provided annotations on the types to be serialized, in which case a custom-configured ObjectMapper is
	 * unnecessary.
	 *
	 * @param objectMapper the object mapper for this view
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.objectMapper = objectMapper;
	}

	/**
	 * Indicate whether the JSON output by this view should be prefixed with "{} &&". Default is false.
	 * <p>
	 * Prefixing the JSON string in this manner is used to help prevent JSON Hijacking. The prefix renders the string
	 * syntactically invalid as a script so that it cannot be hijacked. This prefix does not affect the evaluation of
	 * JSON, but if JSON validation is performed on the string, the prefix would need to be ignored.
	 *
	 * @param prefixJson whether the JSON should be prefixed
	 */
	public void setPrefixJson(boolean prefixJson) {
		this.prefixJson = prefixJson;
	}

	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {

		if (!JsonNode.class.isAssignableFrom(clazz)) {
			return false;
		}

		if (mediaType == null) {
			return true;
		}

		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			// we can't read multipart
			if (supportedMediaType.includes(mediaType)) {
				return true;
			}
		}
		return false;

	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {

		if (!ObjectNode.class.isAssignableFrom(clazz)) {
			return false;
		}

		if (mediaType == null || MediaType.ALL.equals(mediaType)) {
			return true;
		}

		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			if (supportedMediaType.isCompatibleWith(mediaType)) {
				return true;
			}
		}

		return false;
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canRead/Write instead
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
		throws HttpMessageNotReadableException {

		JavaType javaType = getJavaType(clazz);
		try {
			return this.objectMapper.readValue(inputMessage.getBody(), javaType);
		} catch (IOException ex) {
			throw new HttpMessageNotReadableException(
				"Could not read JSON: " + ex.getMessage(),
				ex,
				inputMessage
			);
		}
	}

	@Override
	protected void writeInternal(Object object, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		final JsonEncoding encoding = getJsonEncoding(outputMessage.getHeaders().getContentType());
		final JsonGenerator jsonGenerator = this.objectMapper.getFactory().createGenerator(outputMessage.getBody(), encoding);
		try {
			if (this.prefixJson) {
				jsonGenerator.writeRaw("{} && ");
			}
			this.objectMapper.writeValue(jsonGenerator, object);
		} catch (IOException ex) {
			throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Determine the JSON encoding to use for the given content type.
	 *
	 * @param contentType the media type as requested by the caller
	 * @return the JSON encoding to use (never <code>null</code>)
	 */
	private JsonEncoding getJsonEncoding(MediaType contentType) {
		if (contentType != null && contentType.getCharset() != null) {
			Charset charset = contentType.getCharset();
			for (JsonEncoding encoding : JsonEncoding.values()) {
				if (charset.name().equals(encoding.getJavaName())) {
					return encoding;
				}
			}
		}
		return JsonEncoding.UTF8;
	}

	/**
	 * Return the Jackson {@link JavaType} for the specified class.
	 * <p>
	 * The default implementation returns {@link ObjectMapper#constructType(java.lang.reflect.Type)}, but this can be
	 * overridden in subclasses, to allow for custom generic collection handling. For instance:
	 * <pre class="code">
	 * protected JavaType getJavaType(Class&lt;?&gt; clazz) { if (List.class.isAssignableFrom(clazz)) { return
	 * objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, MyBean.class); } else { return
	 * super.getJavaType(clazz); } }
	 * </pre>
	 *
	 * @param javaClass the class to return the java type for
	 * @return the java type
	 */
	private JavaType getJavaType(Class<?> javaClass) {
		return objectMapper.constructType(javaClass);
	}

}
