package com.googlecode.jsonrpc4j.server;

import com.googlecode.jsonrpc4j.HttpStatusCodeProvider;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.BULK_ERROR;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.ERROR_NOT_HANDLED;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.INTERNAL_ERROR;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.INVALID_REQUEST;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.METHOD_NOT_FOUND;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.METHOD_PARAMS_INVALID;
import static com.googlecode.jsonrpc4j.ErrorResolver.JsonError.PARSE_ERROR;
import static com.googlecode.jsonrpc4j.server.DefaultHttpStatusCodeProviderTest.assertHttpStatusCodeForJsonRpcRequest;
import static com.googlecode.jsonrpc4j.util.Util.intParam1;
import static com.googlecode.jsonrpc4j.util.Util.intParam2;
import static com.googlecode.jsonrpc4j.util.Util.invalidJsonRpcRequestStream;
import static com.googlecode.jsonrpc4j.util.Util.invalidJsonStream;
import static com.googlecode.jsonrpc4j.util.Util.mapper;
import static com.googlecode.jsonrpc4j.util.Util.messageWithListParams;
import static com.googlecode.jsonrpc4j.util.Util.messageWithListParamsStream;
import static com.googlecode.jsonrpc4j.util.Util.multiMessageOfStream;
import static com.googlecode.jsonrpc4j.util.Util.param1;
import static com.googlecode.jsonrpc4j.util.Util.param2;

/**
 * These tests validate the functionality of a custom HttpStatusCodeProvider implementation.
 */
@RunWith(EasyMockRunner.class)
public class HttpStatusCodeProviderTest {
	
	@Mock(type = MockType.NICE)
	private JsonRpcBasicServerTest.ServiceInterface mockService;
	private JsonRpcServer jsonRpcServer;
	private HttpStatusCodeProvider httpStatusCodeProvider;
	
	@Before
	public void setUp() throws Exception {
		jsonRpcServer = new JsonRpcServer(mapper, mockService, JsonRpcBasicServerTest.ServiceInterface.class);
		httpStatusCodeProvider = new HttpStatusCodeProvider() {
			@Override
			public int getHttpStatusCode(int resultCode) {
				if (resultCode == PARSE_ERROR.code) {
					return 1002;
				} else if (resultCode == INVALID_REQUEST.code) {
					return 1001;
				} else if (resultCode == METHOD_NOT_FOUND.code) {
					return 1003;
				} else if (resultCode == METHOD_PARAMS_INVALID.code) {
					return 1004;
				} else if (resultCode == INTERNAL_ERROR.code) {
					return 1007;
				} else if (resultCode == ERROR_NOT_HANDLED.code) {
					return 1006;
				} else if (resultCode == BULK_ERROR.code) {
					return 1005;
				} else {
					return 1000;
				}
			}
			
			@Override
			public Integer getJsonRpcCode(int httpStatusCode) {
				return null;
			}
		};
		
		jsonRpcServer.setHttpStatusCodeProvider(httpStatusCodeProvider);
	}
	
	@Test
	public void http1001ForInvalidRequest() throws Exception {
		assertHttpStatusCodeForJsonRpcRequest(invalidJsonRpcRequestStream(), 1003, jsonRpcServer);
	}
	
	@Test
	public void http1002ForParseError() throws Exception {
		assertHttpStatusCodeForJsonRpcRequest(invalidJsonStream(), 1002, jsonRpcServer);
	}
	
	@Test
	public void http1000ForValidRequest() throws Exception {
		assertHttpStatusCodeForJsonRpcRequest(messageWithListParamsStream(1, "testMethod", param1), 1000, jsonRpcServer);
	}
	
	@Test
	public void http1003ForNonExistingMethod() throws Exception {
		assertHttpStatusCodeForJsonRpcRequest(messageWithListParamsStream(1, "nonExistingMethod", param1), 1003, jsonRpcServer);
	}
	
	@Test
	public void http1004ForInvalidMethodParameters() throws Exception {
		assertHttpStatusCodeForJsonRpcRequest(messageWithListParamsStream(1, "testMethod", param1, param2), 1004, jsonRpcServer);
	}
	
	@Test
	public void http1005ForBulkErrors() throws Exception {
		assertHttpStatusCodeForJsonRpcRequest(
				multiMessageOfStream(
						messageWithListParams(1, "testMethod", param1, param2),
						messageWithListParams(2, "overloadedMethod", intParam1, intParam2)
				), 1005, jsonRpcServer);
	}
	
	@Test
	public void http1006ForErrorNotHandled() throws Exception {
		JsonRpcServer server = new JsonRpcServer(mapper, mockService, JsonRpcErrorsTest.ServiceInterfaceWithoutAnnotation.class);
		server.setHttpStatusCodeProvider(httpStatusCodeProvider);
		assertHttpStatusCodeForJsonRpcRequest(messageWithListParamsStream(1, "testMethod"), 1006, server);
	}
	
}
