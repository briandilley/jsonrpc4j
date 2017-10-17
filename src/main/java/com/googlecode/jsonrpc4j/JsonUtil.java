package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public abstract class JsonUtil {
	private static final Map<Class<? extends JsonNode>, Class<? extends Number>> numericNodesMap = new IdentityHashMap<>(7);

	static {
		numericNodesMap.put(BigIntegerNode.class, BigInteger.class);
		numericNodesMap.put(DecimalNode.class, BigDecimal.class);
		numericNodesMap.put(DoubleNode.class, Double.class);
		numericNodesMap.put(FloatNode.class, Float.class);
		numericNodesMap.put(IntNode.class, Integer.class);
		numericNodesMap.put(LongNode.class, Long.class);
		numericNodesMap.put(ShortNode.class, Short.class);
	}

	public static Class<? extends Number> getJavaTypeForNumericJsonType(Class<? extends NumericNode> node) {
		return numericNodesMap.get(node);
	}

	public static Class<? extends Number> getJavaTypeForNumericJsonType(NumericNode node) {
		return getJavaTypeForNumericJsonType(node.getClass());
	}

	public static Class getJavaTypeForJsonType(JsonNode node) {
		JsonNodeType jsonType = node.getNodeType();

		switch (jsonType) {
			case ARRAY:
				return List.class;
			case BINARY:
				return Object.class;
			case BOOLEAN:
				return Boolean.class;
			case MISSING:
				return Object.class;
			case NULL:
				return Object.class;
			case NUMBER:
				return getJavaTypeForNumericJsonType((NumericNode) node);
			case OBJECT:
				return Object.class;
			case POJO:
				return Object.class;
			case STRING:
				return String.class;
			default:
				return Object.class;
		}
	}
}