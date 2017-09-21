package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author pavyazankin
 * @since 21/09/2017
 */
public interface JsonRpcInterceptor {
    void preHandleJson(JsonNode json);
    void preHandle(Object target, Method method, List<JsonNode> params);
    void postHandle(Object target, Method method, List<JsonNode> params, JsonNode result);
    void postHandleJson(JsonNode json);
}
