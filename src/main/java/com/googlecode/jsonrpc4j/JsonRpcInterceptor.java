package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Interceptor can work with parsed full JSON RPC {@link JsonNode} and with parsed target, method and arguments list.
 * In case you want to work with raw input you can use filters.
 * In case you want to work with your parsed objects you can use aspect on your exported service.
 * <br/><br/>
 * Interceptors could produce {@link RuntimeException} (except preHandleJson!), than JSON RPC standard error will be shown.
 * If exception will thrown in preHandleJson(), <tt>error: {code:-31000, message: "interceptor exception"}</tt> will be generated.
 * @author pavyazankin
 * @since 21/09/2017
 */
public interface JsonRpcInterceptor {
    /**
     * Method to intercept raw JSON RPC json objects. Don't throw exceptions here.
     * If exception will be thrown in preHandleJson(), error: {code:-31000, message: "interceptor exception"} will be generated.
     * @param json raw JSON RPC request json
     */
    void preHandleJson(JsonNode json);

    /**
     * If exception will be thrown in this method, standard JSON RPC error will be generated.
     * @param target target service
     * @param method target method
     * @param params list of params as {@link JsonNode}s
     */
    void preHandle(Object target, Method method, List<JsonNode> params);

    /**
     * If exception will be thrown in this method, standard JSON RPC error will be generated.
     * Even if target method retruns without exception.
     * @param target target service
     * @param method target method
     * @param params list of params as {@link JsonNode}s
     * @param result returned by target service
     */
    void postHandle(Object target, Method method, List<JsonNode> params, JsonNode result);

    /**
     * If exception will be thrown in this method, standard JSON RPC error will be generated.
     * Even if target method retruns without exception.
     * @param json raw JSON RPC response json
     */
    void postHandleJson(JsonNode json);
}
