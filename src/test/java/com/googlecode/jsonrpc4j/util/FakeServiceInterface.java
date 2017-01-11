package com.googlecode.jsonrpc4j.util;

import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings("unused")
public interface FakeServiceInterface {
	void doSomething();
	
	int returnPrimitiveInt(int arg);
	
	CustomClass returnCustomClass(int arg1, String arg2);
	
	void throwSomeException(String message);
	
	class CustomClass {
		
		public final int integer;
		public final String string;
		public final Collection<String> list = new ArrayList<>();
		
		public CustomClass() {
			this(0, "");
		}
		
		CustomClass(final int integer, final String string) {
			this.integer = integer;
			this.string = string;
		}
	}
}
