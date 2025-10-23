package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * Determines whether or not the given {@link JsonNode} matches
     * the given type.  This method is limited to a few java types
     * only and shouldn't be used to determine with great accuracy
     * whether or not the types match.
     *
     * @param node the {@link JsonNode}
     * @param type the {@link Class}
     * @return true if the types match, false otherwise
     */
    @SuppressWarnings("SimplifiableIfStatement")
    static boolean isMatchingType(JsonNode node, Class<?> type) {
        if (node.isNull()) {
            return true;
        }
        if (node.isTextual()) {
            return String.class.isAssignableFrom(type);
        }
        if (node.isNumber()) {
            return isNumericAssignable(type);
        }
        if (node.isArray() && type.isArray()) {
            return !node.isEmpty() && isMatchingType(node.get(0), type.getComponentType());
        }
        if (node.isArray()) {
            return type.isArray() || Collection.class.isAssignableFrom(type);
        }
        if (node.isBinary()) {
            return byteOrCharAssignable(type);
        }
        if (node.isBoolean()) {
            return boolean.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type);
        }
        if (node.isObject() || node.isPojo()) {
            return !type.isPrimitive() && !String.class.isAssignableFrom(type) &&
                    !Number.class.isAssignableFrom(type) && !Boolean.class.isAssignableFrom(type);
        }
        return false;
    }

    private static boolean byteOrCharAssignable(Class<?> type) {
        return byte[].class.isAssignableFrom(type) || Byte[].class.isAssignableFrom(type) ||
                char[].class.isAssignableFrom(type) || Character[].class.isAssignableFrom(type);
    }

    private static boolean isNumericAssignable(Class<?> type) {
        return Number.class.isAssignableFrom(type) || short.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)
                || long.class.isAssignableFrom(type) || float.class.isAssignableFrom(type) || double.class.isAssignableFrom(type);
    }

    static Object parseId(JsonNode node) {
        if (isNullNodeOrValue(node)) {
            return null;
        }
        if (node.isDouble()) {
            return node.asDouble();
        }
        if (node.isFloatingPointNumber()) {
            return node.asDouble();
        }
        if (node.isInt()) {
            return node.asInt();
        }
        if (node.isLong()) {
            return node.asLong();
        }
        //TODO(donequis): consider parsing bigints
        if (node.isIntegralNumber()) {
            return node.asInt();
        }
        if (node.isTextual()) {
            return node.asText();
        }
        throw new IllegalArgumentException("Unknown id type");
    }

    static boolean isNullNodeOrValue(JsonNode node) {
        return node == null || node.isNull();
    }

    static Set<String> collectFieldNames(JsonNode paramsNode) {
        Set<String> fieldNames = new LinkedHashSet<>();
        Iterator<String> itr = paramsNode.fieldNames();
        while (itr.hasNext()) {
            fieldNames.add(itr.next());
        }
        return fieldNames;
    }
}
