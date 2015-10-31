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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReadContext {

	private final InputStream input;
	private final ObjectMapper mapper;

	// private static final WeakHashMap<InputStream, ReadContext> contexts = new WeakHashMap<InputStream, ReadContext>();

	public synchronized static ReadContext getReadContext(
			InputStream ips, ObjectMapper mapper)
			throws JsonParseException,
			IOException {
		ReadContext ret =  null; // contexts.get(ips);
		if (ret==null) {
			ret = new ReadContext(ips, mapper);
		}
		return ret;
	}

	public ReadContext(InputStream ips, ObjectMapper mapper)
			throws JsonParseException, IOException {
		this.input 		= new NoCloseInputStream(ips);
		this.mapper		= mapper;
	}

	public JsonNode nextValue()
			throws IOException {
		return mapper.readValue(input, JsonNode.class);
	}

	public void assertReadable()
			throws StreamEndedException,
			IOException {
		if (input.markSupported()) {
			input.mark(1);
			if (input.read()==-1) {
				throw new StreamEndedException();
			}
			input.reset();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((input == null) ? 0 : input.hashCode());
		result = prime * result + ((mapper == null) ? 0 : mapper.hashCode());
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
		if (input == null) {
			if (other.input != null)
				return false;
		} else if (!input.equals(other.input))
			return false;
		if (mapper == null) {
			if (other.mapper != null)
				return false;
		} else if (!mapper.equals(other.mapper))
			return false;
		return true;
	}

}
