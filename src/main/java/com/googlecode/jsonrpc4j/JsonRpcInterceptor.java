package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Interceptor can work with parsed full JSON RPC {@link JsonNode} and with parsed target, method and arguments list.
 * In case you want to work with raw input you can use filters.
 * In case you want to work with your parsed objects you can use aspect on your exported service.
 * <p>
 * Interceptors could throw {@link RuntimeException}, in this case JSON RPC standard error will be shown, except preHandleJson.
 * In case preHandleJson throws exception, it will generate HTTP code 400 "Bad Request" with empty body. So be careful.
 * @author pavyazankin
 * @since 21/09/2017
 */
public interface JsonRpcInterceptor {

    /**
     * Method to intercept raw JSON RPC json objects. Don't throw exceptions here.
     * In case preHandleJson throws exception, it will be HTTP code 400 "Bad Request" with empty body. So be careful.
     * @param json raw JSON RPC request json
     */
    void preHandleJson(JsonNode json);

    /**
     * If exception will be thrown in this method, standard JSON RPC error will be generated.
     * <p><b>Example</b>
     * <pre>
     * {
     *      "jsonrpc":"2.0",
     *      "id":0,
     *      "error":{
     *          "code":-32001,
     *          "message":"123",
     *          "data":{
     *              "exceptionTypeName":"java.lang.RuntimeException",
     *              "message":"123"
     *          }
     *      }
     * }
     * </pre>
     * <p>
     * For changing exception handling custom {@link ErrorResolver} could be generated.
     * </p>
     * @param target target service
     * @param method target method
     * @param params list of params as {@link JsonNode}s
     */
    void preHandle(Object target, Method method, List<JsonNode> params);

    /**
     * If exception will be thrown in this method, standard JSON RPC error will be generated. Example in preHandle
     * Even if target method retruns without exception.
     * @param target target service
     * @param method target method
     * @param params list of params as {@link JsonNode}s
     * @param result returned by target service
     */
    void postHandle(Object target, Method method, List<JsonNode> params, JsonNode result);

    /**
     * If exception will be thrown in this method, standard JSON RPC error will be generated. Example in preHandle
     * Even if target method retruns without exception.
     * @param json raw JSON RPC response json
     */
    void postHandleJson(JsonNode json);
}
