package com.github.briandilley.jsonrpc4j.beanvalidation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.metadata.BeanDescriptor;
import jakarta.validation.metadata.MethodDescriptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.ErrorResolver.JsonError;
import com.googlecode.jsonrpc4j.JsonRpcInterceptor;
import com.googlecode.jsonrpc4j.JsonRpcServerException;

/**
 * Validates requests and responses using Jakarta Bean Validation
 */
public class BeanValidationJsonRpcInterceptor implements JsonRpcInterceptor {

    private final ValidatorFactory validatorFactory;

    public BeanValidationJsonRpcInterceptor(ValidatorFactory validatorFactory) {
        this.validatorFactory = validatorFactory;
    }

    public BeanValidationJsonRpcInterceptor() {
        this.validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @Override
    public void preHandleJson(JsonNode json) {
        // noop
    }

    @Override
    public void preHandle(Object target, Method method, List<JsonNode> params) {
        // noop
    }

    @Override
    public void preHandle(
        Object target,
        Method method,
        JsonNode paramsJsonNode, List<JsonNode> jsonParams,
        List<Object> deserializedParams,
        List<String> deserializedParamsNames
    ) {
        Validator validator = this.validatorFactory.getValidator();
        BeanDescriptor beanDescriptor = validator.getConstraintsForClass(target.getClass());
        MethodDescriptor methodDescriptor = beanDescriptor.getConstraintsForMethod(
            method.getName(),
            method.getParameterTypes()
        );
        if (methodDescriptor == null || !methodDescriptor.hasConstrainedParameters()) {
            return;
        }

        Set<ConstraintViolation<Object>> constraintViolations;
        try {
            constraintViolations = validator
                .forExecutables()
                .validateParameters(target, method, deserializedParams.toArray());
        } catch (Exception e) {
            throw new JsonRpcServerException(
                JsonError.INTERNAL_ERROR.code,
                JsonError.INTERNAL_ERROR.message,
                null,
                new IllegalStateException(
                    "Failed to validate method parameters with bean validation",
                    e
                )
            );
        }

        if (!constraintViolations.isEmpty()) {
            handleMethodParametersConstraintViolations(
                target,
                method,
                jsonParams,
                paramsJsonNode,
                deserializedParams,
                deserializedParamsNames,
                constraintViolations
            );
        }
    }

    protected static void handleMethodParametersConstraintViolations(
        Object target,
        Method method,
        List<JsonNode> jsonParams,
        JsonNode paramsJsonNode,
        List<Object> deserializedParams,
        List<String> deserializedParamsNames,
        Set<ConstraintViolation<Object>> constraintViolations
    ) {
        List<ValidationError> validationErrors = new ArrayList<>(constraintViolations.size());
        for (ConstraintViolation<Object> violation : constraintViolations) {
            ValidationError validationError = createValidationError(
                method,
                paramsJsonNode,
                deserializedParamsNames,
                violation
            );
            validationErrors.add(validationError);
        }

        throw new JsonRpcServerException(
            JsonError.METHOD_PARAMS_INVALID.code,
            JsonError.METHOD_PARAMS_INVALID.message,
            new ErrorData(validationErrors),
            new ConstraintViolationException(constraintViolations)
        );
    }

    private static ValidationError createValidationError(
        Method method,
        JsonNode paramsJsonNode,
        List<String> deserializedParamsNames,
        ConstraintViolation<Object> violation
    ) {
        Iterator<Path.Node> pathIterator = violation.getPropertyPath().iterator();

        readAndCheckMethodName(pathIterator);
        int paramIndex = readAndCheckParameterAndGetIndex(pathIterator);

        String paramName = null;
        if (paramsJsonNode.isObject() && paramIndex < deserializedParamsNames.size()) {
            paramName = deserializedParamsNames.get(paramIndex);
        }

        StringBuilder jsonPointer = new StringBuilder();
        if (paramName != null) {
            jsonPointer
                .append('/')
                .append(paramName);
        } else if (
            paramsJsonNode.isArray()
                && method.isVarArgs()
                && method.getParameterCount() == 1
                && pathIterator.hasNext()
        ) {
            // skip index addition into json pointer here for varargs,
            // this will be added later based on the argument position in varargs array
            paramIndex = getParameterIndexInVarargsArray(
                violation.getPropertyPath().iterator()
            );
        } else {
            jsonPointer
                .append('/')
                .append(paramIndex);
        }

        appendRemainingPath(jsonPointer, pathIterator);

        return new ValidationError(
            paramName == null ? paramIndex : null,
            paramName,
            violation.getMessage(),
            jsonPointer.toString()
        );
    }

    private static int getParameterIndexInVarargsArray(Iterator<Path.Node> pathIterator) {
        readAndCheckMethodName(pathIterator);
        readAndCheckParameterAndGetIndex(pathIterator);

        Path.Node invalidObjectNode = pathIterator.next();

        if (invalidObjectNode.isInIterable()) {
            Integer arrayIndex = invalidObjectNode.getIndex();
            if (arrayIndex != null) {
                return arrayIndex;
            }
        }

        throw new JsonRpcServerException(
            JsonError.INTERNAL_ERROR.code,
            JsonError.INTERNAL_ERROR.message,
            null,
            new IllegalStateException(
                "Failed to get invalid object index in varargs array"
            )
        );
    }

    private static void appendRemainingPath(
        StringBuilder jsonPointer,
        Iterator<Path.Node> pathIterator
    ) {
        while (pathIterator.hasNext()) {
            final Path.Node nextNode = pathIterator.next();
            final ElementKind kind = nextNode.getKind();

            final String nodeName;
            if (
                nextNode.isInIterable()
                    && (
                        ElementKind.PROPERTY.equals(kind)
                            || ElementKind.CONTAINER_ELEMENT.equals(kind)
                )
            ) {
                Object mapKey = nextNode.getKey();
                Integer arrayIndex = nextNode.getIndex();
                if (mapKey != null) {
                    // Note: mapKey may be a user input,
                    // but has already been validated by the JSON parser,
                    // and this should be a valid JSON object field name.
                    // Object field names cannot have different types other that String.
                    if (ElementKind.PROPERTY.equals(kind)) {
                        nodeName = mapKey + "/" + nextNode.getName();
                    } else {
                        nodeName = mapKey.toString();
                    }
                } else if (arrayIndex != null) {
                    if (ElementKind.PROPERTY.equals(kind)) {
                        nodeName = arrayIndex + "/" + nextNode.getName();
                    } else {
                        nodeName = String.valueOf(arrayIndex);
                    }
                } else {
                    throw new JsonRpcServerException(
                        JsonError.INTERNAL_ERROR.code,
                        JsonError.INTERNAL_ERROR.message,
                        null,
                        new IllegalStateException(
                            "Found invalid " + ConstraintViolation.class.getSimpleName()
                                + ": collection member has neither an index nor a key"
                        )
                    );
                }
            } else {
                nodeName = nextNode.getName();
            }

            jsonPointer
                .append('/')
                .append(nodeName);
        }
    }

    private static void readAndCheckMethodName(Iterator<Path.Node> pathIterator) {
        Path.Node methodNode = pathIterator.next();
        if (!ElementKind.METHOD.equals(methodNode.getKind())) {
            throw new IllegalStateException(
                "method node expected, got : " + methodNode.getKind()
            );
        }
    }

    private static int readAndCheckParameterAndGetIndex(Iterator<Path.Node> pathIterator) {
        if (!pathIterator.hasNext()) {
            throw new IllegalStateException("parameter node missing");
        }
        Path.Node parameterNode = pathIterator.next();
        if (!ElementKind.PARAMETER.equals(parameterNode.getKind())) {
            throw new IllegalStateException(
                "parameter node expected, got : " + parameterNode.getKind()
            );
        }
        return parameterNode.as(Path.ParameterNode.class).getParameterIndex();
    }

    @Override
    public void postHandle(Object target, Method method, List<JsonNode> params, JsonNode result) {
        // noop
    }

    @Override
    public void postHandle(
        Object target,
        Method method,
        List<JsonNode> jsonParams,
        JsonNode paramsJsonNode,
        List<Object> deserializedParams,
        List<String> detectedParamNames,
        Object result
    ) {
        Validator validator = this.validatorFactory.getValidator();
        BeanDescriptor beanDescriptor = validator.getConstraintsForClass(target.getClass());
        MethodDescriptor methodDescriptor = beanDescriptor.getConstraintsForMethod(
            method.getName(),
            method.getParameterTypes()
        );
        if (methodDescriptor == null || !methodDescriptor.hasConstrainedReturnValue()) {
            return;
        }

        Set<ConstraintViolation<Object>> constraintViolations;
        try {
            constraintViolations = validator
                .forExecutables()
                .validateReturnValue(target, method, result);
        } catch (Exception e) {
            throw new JsonRpcServerException(
                JsonError.INTERNAL_ERROR.code,
                JsonError.INTERNAL_ERROR.message,
                null,
                new IllegalStateException(
                    "Failed to validate response object with bean validation",
                    e
                )
            );
        }

        if (!constraintViolations.isEmpty()) {
            handleResponseObjectConstraintViolations(constraintViolations);
        }
    }

    protected void handleResponseObjectConstraintViolations(
        Set<ConstraintViolation<Object>> constraintViolations
    ) {
        throw new JsonRpcServerException(
            JsonError.INTERNAL_ERROR.code,
            JsonError.INTERNAL_ERROR.message,
            null,
            new ConstraintViolationException(constraintViolations)
        );
    }

    @Override
    public void postHandleJson(JsonNode json) {
        // noop
    }

    protected static final class ErrorData {
        private final List<ValidationError> errors;

        protected ErrorData(List<ValidationError> errors) {
            this.errors = errors;
        }

        public List<ValidationError> getErrors() {
            return errors;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected static final class ValidationError {
        /**
         * Java method parameter index
         */
        private final Integer paramIndex;

        /**
         * Parameter name from the @JsonRpcParam or similar annotation.
         */
        private final String paramName;
        private final String detail;
        /**
         * RFC6901 JSON pointer
         */
        private final String jsonPointer;

        protected ValidationError(
            Integer paramIndex,
            String paramName,
            String detail,
            String jsonPointer
        ) {
            this.paramIndex = paramIndex;
            this.paramName = paramName;
            this.detail = detail;
            this.jsonPointer = jsonPointer;
        }

        public Integer getParamIndex() {
            return paramIndex;
        }

        public String getParamName() {
            return paramName;
        }

        public String getDetail() {
            return detail;
        }

        public String getJsonPointer() {
            return jsonPointer;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ValidationError)) {
                return false;
            }
            ValidationError that = (ValidationError) o;
            return Objects.equals(paramIndex, that.paramIndex)
                && Objects.equals(paramName, that.paramName)
                && Objects.equals(detail, that.detail)
                && Objects.equals(jsonPointer, that.jsonPointer);
        }

        @Override
        public int hashCode() {
            return Objects.hash(paramIndex, paramName, detail, jsonPointer);
        }
    }
}
