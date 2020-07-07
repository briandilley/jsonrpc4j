package com.googlecode.jsonrpc4j.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import org.easymock.EasyMock;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.DATA;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.ERROR;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.ERROR_MESSAGE;
import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.RESULT;
import static com.googlecode.jsonrpc4j.util.Util.decodeAnswer;
import static com.googlecode.jsonrpc4j.util.Util.getFromArrayWithId;
import static com.googlecode.jsonrpc4j.util.Util.messageWithListParams;
import static com.googlecode.jsonrpc4j.util.Util.multiMessageOfStream;
import static com.googlecode.jsonrpc4j.util.Util.toByteArrayOutputStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test-post");
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setContent(StreamUtils.copyToByteArray(inputStream));

        jsonRpcServer.handle(request, response);

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

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test-post");
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setContent(StreamUtils.copyToByteArray(inputStream));

        jsonRpcServer.handle(request, response);

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        JsonNode answer = decodeAnswer(toByteArrayOutputStream(response.getContentAsByteArray()));
        assertTrue(answer instanceof ArrayNode);
        assertEquals("Error", getFromArrayWithId(answer, 1).get(ERROR).get(DATA).get(ERROR_MESSAGE).asText());
        assertEquals("Result2", getFromArrayWithId(answer, 2).get(RESULT).asText());
    }
}
