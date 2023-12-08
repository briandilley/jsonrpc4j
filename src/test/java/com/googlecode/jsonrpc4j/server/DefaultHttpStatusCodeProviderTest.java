package com.googlecode.jsonrpc4j.server;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import org.easymock.EasyMockExtension;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.InputStream;

import static com.googlecode.jsonrpc4j.util.Util.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class validates the mapping of JSON-RPC result codes to HTTP status codes.
 */
@ExtendWith(EasyMockExtension.class)
public class DefaultHttpStatusCodeProviderTest {
	
	@Mock(type = MockType.NICE)
	private JsonRpcBasicServerTest.ServiceInterface mockService;
	private JsonRpcServer jsonRpcServer;

	@BeforeEach
	public void setUp() throws Exception {
		jsonRpcServer = new JsonRpcServer(mapper, mockService, JsonRpcBasicServerTest.ServiceInterface.class);
	}
	
	@Test
	public void http404ForInvalidRequest() throws Exception {
		assertHttpStatusCodeForJsonRpcRequest(invalidJsonRpcRequestStream(), 404, jsonRpcServer);
	}
	
	public static void assertHttpStatusCodeForJsonRpcRequest(InputStream message, int expectedCode, JsonRpcServer server) throws Exception {
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse res = new MockHttpServletResponse();
		req.setMethod(HttpMethod.POST.name());
		req.setContent(convertInputStreamToByteArray(message));
		server.handle(req, res);
		assertEquals(expectedCode, res.getStatus());
	}
	
	@Test
	public void http400ForParseError() throws Exception {
		assertHttpStatusCodeForJsonRpcRequest(invalidJsonStream(), 400, jsonRpcServer);
	}
	
	@Test
	public void http200ForValidRequest() throws Exception {
		assertHttpStatusCodeForJsonRpcRequest(messageWithListParamsStream(1, "testMethod", param1), 200, jsonRpcServer);
	}
	
	@Test
	public void http404ForNonExistingMethod() throws Exception {
		assertHttpStatusCodeForJsonRpcRequest(messageWithListParamsStream(1, "nonExistingMethod", param1), 404, jsonRpcServer);
	}
	
	@Test
	public void http500ForInvalidMethodParameters() throws Exception {
		assertHttpStatusCodeForJsonRpcRequest(messageWithListParamsStream(1, "testMethod", param1, param2), 500, jsonRpcServer);
	}
	
	@Test
	public void http500ForBulkErrors() throws Exception {
		assertHttpStatusCodeForJsonRpcRequest(
				multiMessageOfStream(
						messageWithListParams(1, "testMethod", param1, param2),
						messageWithListParams(2, "overloadedMethod", intParam1, intParam2)
				), 500, jsonRpcServer);
	}
	
	@Test
	public void http500ForErrorNotHandled() throws Exception {
		JsonRpcServer server = new JsonRpcServer(mapper, mockService, JsonRpcErrorsTest.ServiceInterfaceWithoutAnnotation.class);
		assertHttpStatusCodeForJsonRpcRequest(messageWithListParamsStream(1, "testMethod"), 500, server);
	}
	
}
