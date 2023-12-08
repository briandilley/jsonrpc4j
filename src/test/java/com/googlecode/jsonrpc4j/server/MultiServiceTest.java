package com.googlecode.jsonrpc4j.server;

import com.googlecode.jsonrpc4j.JsonRpcMultiServer;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import org.easymock.EasyMock;
import org.easymock.EasyMockExtension;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayOutputStream;

import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.RESULT;
import static com.googlecode.jsonrpc4j.util.Util.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(EasyMockExtension.class)
public class MultiServiceTest {
	
	private static final String serviceName = "Test";
	@Mock(type = MockType.NICE)
	private ServiceInterfaceWithParamNameAnnotation mockService;
	private JsonRpcMultiServer multiServer;
	private ByteArrayOutputStream byteArrayOutputStream;

    @BeforeEach
	public void setup() {
		multiServer = new JsonRpcMultiServer();
		multiServer.addService(serviceName, mockService, ServiceInterfaceWithParamNameAnnotation.class);
		byteArrayOutputStream = new ByteArrayOutputStream();
	}
	
	@Test
	public void callMethodExactNumberOfParametersNamed() throws Exception {
		EasyMock.expect(mockService.testMethod(param2)).andReturn("success");
		EasyMock.replay(mockService);
		
		multiServer.handleRequest(messageWithMapParamsStream(serviceName + JsonRpcMultiServer.DEFAULT_SEPARATOR + "testMethod", param1, param2), byteArrayOutputStream);
		
		assertEquals("success", decodeAnswer(byteArrayOutputStream).get(RESULT).textValue());
	}
	
	public interface ServiceInterfaceWithParamNameAnnotation {
		String testMethod(@JsonRpcParam("param1") String param1);
	}
	
}
