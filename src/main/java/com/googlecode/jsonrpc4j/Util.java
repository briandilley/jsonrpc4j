package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.node.ObjectNode;

class Util {

	static boolean hasNonNullObjectData(final ObjectNode node, final String key) {
		return hasNonNullData(node, key) && node.get(key).isObject();
	}

	static boolean hasNonNullData(final ObjectNode node, final String key) {
		return node.has(key) && !node.get(key).isNull();
	}

	static boolean hasNonNullTextualData(final ObjectNode node, final String key) {
		return hasNonNullData(node, key) && node.get(key).isTextual();
	}
}
