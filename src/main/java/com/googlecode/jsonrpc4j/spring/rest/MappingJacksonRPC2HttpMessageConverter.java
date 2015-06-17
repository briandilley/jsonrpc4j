/*
 * The MIT License
 *
 * Copyright (c) 2014 jsonrpc4j
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jsonrpc4j.spring.rest;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.Charset;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.Assert;

/**
 *
 * @author toha
 */

public class MappingJacksonRPC2HttpMessageConverter extends AbstractHttpMessageConverter<Object>
{
	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    public static final MediaType APPLICATION_JSON_RPC = new MediaType("application", "json-rpc", DEFAULT_CHARSET);
    
    
	private ObjectMapper objectMapper;

	private boolean prefixJson = false;

	/**
	 * Construct a new {@code BindingJacksonHttpMessageConverter}.
	 */
	public MappingJacksonRPC2HttpMessageConverter () {
		super(APPLICATION_JSON_RPC);
        objectMapper = new ObjectMapper();
	}

	/**
	 * Construct a new {@code BindingJacksonHttpMessageConverter}.
	 */
	public MappingJacksonRPC2HttpMessageConverter (ObjectMapper objectMapper) {
		super(APPLICATION_JSON_RPC);
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
        this.objectMapper = objectMapper;
	}
    
	/**
	 * Set the {@code ObjectMapper} for this view. If not set, a default
	 * {@link ObjectMapper#ObjectMapper() ObjectMapper} is used.
	 * <p>Setting a custom-configured {@code ObjectMapper} is one way to take further control of the JSON
	 * serialization process. For example, an extended {@link org.codehaus.jackson.map.SerializerFactory}
	 * can be configured that provides custom serializers for specific types. The other option for refining
	 * the serialization process is to use Jackson's provided annotations on the types to be serialized,
	 * in which case a custom-configured ObjectMapper is unnecessary.
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.objectMapper = objectMapper;
	}

	/**
	 * Return the underlying {@code ObjectMapper} for this view.
	 */
	public ObjectMapper getObjectMapper() {
		return this.objectMapper;
	}

	/**
	 * Indicate whether the JSON output by this view should be prefixed with "{} &&". Default is false.
	 * <p>Prefixing the JSON string in this manner is used to help prevent JSON Hijacking.
	 * The prefix renders the string syntactically invalid as a script so that it cannot be hijacked.
	 * This prefix does not affect the evaluation of JSON, but if JSON validation is performed on the
	 * string, the prefix would need to be ignored.
	 */
	public void setPrefixJson(boolean prefixJson) {
		this.prefixJson = prefixJson;
	}


	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
        
        if (!ObjectNode.class.isAssignableFrom(clazz)) {
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
			throws IOException, HttpMessageNotReadableException {

		JavaType javaType = getJavaType(clazz);
		try {
			return this.objectMapper.readValue(inputMessage.getBody(), javaType);
		}
		catch (IOException ex) {
			throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected void writeInternal(Object object, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		JsonEncoding encoding = getJsonEncoding(outputMessage.getHeaders().getContentType());
		JsonGenerator jsonGenerator =
				this.objectMapper.getJsonFactory().createJsonGenerator(outputMessage.getBody(), encoding);
		try {
			if (this.prefixJson) {
				jsonGenerator.writeRaw("{} && ");
			}
			this.objectMapper.writeValue(jsonGenerator, object);
		}
		catch (IOException ex) {
			throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
		}
	}


	/**
	 * Return the Jackson {@link JavaType} for the specified class.
	 * <p>The default implementation returns {@link ObjectMapper#constructType(java.lang.reflect.Type)},
	 * but this can be overridden in subclasses, to allow for custom generic collection handling.
	 * For instance:
	 * <pre class="code">
	 * protected JavaType getJavaType(Class&lt;?&gt; clazz) {
	 *   if (List.class.isAssignableFrom(clazz)) {
	 *     return objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, MyBean.class);
	 *   } else {
	 *     return super.getJavaType(clazz);
	 *   }
	 * }
	 * </pre>
	 * @param clazz the class to return the java type for
	 * @return the java type
	 */
	protected JavaType getJavaType(Class<?> clazz) {
		return objectMapper.constructType(clazz);
	}

	/**
	 * Determine the JSON encoding to use for the given content type.
	 * @param contentType the media type as requested by the caller
	 * @return the JSON encoding to use (never <code>null</code>)
	 */
	protected JsonEncoding getJsonEncoding(MediaType contentType) {
		if (contentType != null && contentType.getCharSet() != null) {
			Charset charset = contentType.getCharSet();
			for (JsonEncoding encoding : JsonEncoding.values()) {
				if (charset.name().equals(encoding.getJavaName())) {
					return encoding;
				}
			}
		}
		return JsonEncoding.UTF8;
	}
    
}
