package com.googlecode.jsonrpc4j.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.server.JsonRpcServerAnnotatedParamTest.ServiceInterfaceWithParamNameAnnotation;
import com.googlecode.jsonrpc4j.util.Util;
import jakarta.servlet.http.HttpServletResponse;
import org.easymock.EasyMock;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.METHOD_PARAMS_INVALID;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.*;
import static com.googlecode.jsonrpc4j.server.JsonRpcServerAnnotatedParamTest.METHOD_WITH_DIFFERENT_TYPES;
import static com.googlecode.jsonrpc4j.util.Util.*;
import static org.junit.jupiter.api.Assertions.*;

public abstract class JsonRpcServerBatchTest {
    @Mock(type = MockType.NICE)
    protected JsonRpcServerTest.ServiceInterface mockService;
    protected JsonRpcServer jsonRpcServer;

    @Test
    public void parallelBatchProcessingTest() throws Exception {
        EasyMock.expect(mockService.testMethod("Parameter1")).andReturn("Result1");
        EasyMock.expect(mockService.testMethod("Parameter2")).andReturn("Result2");
        EasyMock.replay(mockService);

        InputStream inputStream = multiMessageOfStream(
                messageWithListParams(1, "testMethod", "Parameter1"),
                messageWithListParams(2, "testMethod", "Parameter2"));

        MockHttpServletResponse response = handleRequest(inputStream);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        JsonNode answer = decodeAnswer(toByteArrayOutputStream(response.getContentAsByteArray()));
        assertTrue(answer instanceof ArrayNode);
        assertEquals("Result1", getFromArrayWithId(answer, 1).get(RESULT).asText());
        assertEquals("Result2", getFromArrayWithId(answer, 2).get(RESULT).asText());
    }

    @Test
    public void parallelBatchProcessingBulkErrorTest() throws Exception {
        EasyMock.expect(mockService.testMethod("Parameter1")).andThrow(new RuntimeException("Error"));
        EasyMock.expect(mockService.testMethod("Parameter2")).andReturn("Result2");
        EasyMock.replay(mockService);

        InputStream inputStream = multiMessageOfStream(
                messageWithListParams(1, "testMethod", "Parameter1"),
                messageWithListParams(2, "testMethod", "Parameter2"));

        MockHttpServletResponse response = handleRequest(inputStream);

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        JsonNode answer = decodeAnswer(toByteArrayOutputStream(response.getContentAsByteArray()));
        assertTrue(answer instanceof ArrayNode);
        assertEquals("Error", getFromArrayWithId(answer, 1).get(ERROR).get(DATA).get(ERROR_MESSAGE).asText());
        assertEquals("Result2", getFromArrayWithId(answer, 2).get(RESULT).asText());
    }

    @Test
    public void parallelBatchWithInvalidRequestInsideShouldPreserveJsonRpcId() throws Exception {
        ServiceInterfaceWithParamNameAnnotation mockServiceWithParams =
            EasyMock.strictMock(ServiceInterfaceWithParamNameAnnotation.class);

        jsonRpcServer = new JsonRpcServer(
            Util.mapper,
            mockServiceWithParams,
            ServiceInterfaceWithParamNameAnnotation.class
        );

        final String validResult = "passResult";
        final String validResultOne = validResult + "1";
        final String validResultThree = validResult + "3";

        EasyMock
            .expect(
                mockServiceWithParams.methodWithDifferentTypes(
                    EasyMock.anyBoolean(),
                    EasyMock.anyDouble(),
                    EasyMock.anyObject(UUID.class)
                )
            )
            .andReturn(validResultOne);
        EasyMock
            .expect(
                mockServiceWithParams.methodWithDifferentTypes(
                    EasyMock.anyBoolean(),
                    EasyMock.anyDouble(),
                    EasyMock.anyObject(UUID.class)
                )
            )
            .andReturn(validResultThree);
        EasyMock.replay(mockServiceWithParams);

        final List<Object> validParams = Arrays.asList(true, 3.14, UUID.randomUUID());
        final String invalidBoolean = "truth";

        InputStream inputStream = multiMessageOfStream(
            messageOfStream(1, METHOD_WITH_DIFFERENT_TYPES, validParams),
            messageOfStream(
                2,
                METHOD_WITH_DIFFERENT_TYPES,
                Arrays.asList(invalidBoolean, 3.14, UUID.randomUUID())
            ),
            messageOfStream(3, METHOD_WITH_DIFFERENT_TYPES, validParams)
        );

        MockHttpServletResponse response = handleRequest(inputStream);

        EasyMock.verify(mockServiceWithParams);

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        JsonNode answer = decodeAnswer(toByteArrayOutputStream(response.getContentAsByteArray()));
        assertTrue(answer instanceof ArrayNode);

        assertEquals(validResultOne, getFromArrayWithId(answer, 1).get(RESULT).asText());
        assertEquals(validResultThree, getFromArrayWithId(answer, 3).get(RESULT).asText());

        JsonNode secondResponse = getFromArrayWithId(answer, 2);
        JsonNode responseId = secondResponse.get(ID);
        assertNotNull(responseId);
        assertTrue(responseId.isInt());
        assertEquals(2, responseId.asInt());

        JsonNode error = secondResponse.get(ERROR);
        assertNotNull(error);

        assertEquals(METHOD_PARAMS_INVALID.code, errorCode(error).asInt());

        String errorMessage = error.get(ERROR_MESSAGE).asText();
        assertTrue(errorMessage.toLowerCase(Locale.ROOT).contains("index"));
        assertTrue(errorMessage.contains("0"));
    }

    private static MockHttpServletRequest createRequest(InputStream inputStream) throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test-post");
        request.setContent(StreamUtils.copyToByteArray(inputStream));
        return request;
    }

    private MockHttpServletResponse handleRequest(InputStream inputStream) throws IOException {
        MockHttpServletRequest request = createRequest(inputStream);
        MockHttpServletResponse response = new MockHttpServletResponse();
        jsonRpcServer.handle(request, response);
        return response;
    }
}
