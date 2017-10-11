package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

@SuppressWarnings("WeakerAccess")
public class ReadContext {
	
	private final InputStream input;
	private final ObjectMapper mapper;
	
	private ReadContext(InputStream input, ObjectMapper mapper) {
		this.input = new NoCloseInputStream(input);
		this.mapper = mapper;
	}
	
	public static ReadContext getReadContext(InputStream input, ObjectMapper mapper) {
		return new ReadContext(input, mapper);
	}
	
	public JsonNode nextValue() throws IOException {
		return mapper.readValue(input, JsonNode.class);
	}
	
	public void assertReadable() throws IOException {
		if (input.markSupported()) {
			input.mark(1);
			if (input.read() == -1) {
				throw new StreamEndedException();
			}
			input.reset();
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + input.hashCode();
		result = prime * result + (mapper == null ? 0 : mapper.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReadContext other = (ReadContext) obj;
		if (!input.equals(other.input))
			return false;
		if (mapper == null) {
			if (other.mapper != null)
				return false;
		} else if (!mapper.equals(other.mapper))
			return false;
		return true;
	}
	
}
