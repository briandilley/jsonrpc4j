package com.googlecode.jsonrpc4j.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.RESULT;
import static com.googlecode.jsonrpc4j.util.Util.*;

@RunWith(EasyMockRunner.class)
public class JsonRpcServerAnnotateMethodVarArgsTest {

	@Mock(type = MockType.NICE)
	private ServiceInterfaceWithCustomMethodNameWithVarArgsAnnotation mockService;
	
	private ByteArrayOutputStream byteArrayOutputStream;
	private JsonRpcBasicServer jsonRpcServerAnnotatedMethod;
	
	@Before
	public void setup() {
		byteArrayOutputStream = new ByteArrayOutputStream();
		jsonRpcServerAnnotatedMethod = new JsonRpcBasicServer(mapper, mockService, ServiceInterfaceWithCustomMethodNameWithVarArgsAnnotation.class);
	}

	@Test
	public void callMethodWithVarArgParameters() throws Exception {
		jsonRpcServerAnnotatedMethod.handleRequest(
                messageWithMapParamsStream(
                        "testMethodVararg",
                        "argOne","one",
                        "argTwo",2,
                        "argThree","three",
                        "argFour", 4,
                        "argFive", (Object)"five",
                        "argSix", 6.0f,
                        "argSeven", 7d
                ),
				byteArrayOutputStream
		);

        System.out.println("res: "+result());

	}

    private JsonNode result() throws IOException {
        return decodeAnswer(byteArrayOutputStream).get(RESULT);
    }

	
	public interface ServiceInterfaceWithCustomMethodNameWithVarArgsAnnotation {
		@JsonRpcMethod("Test.custom")
		String customMethod();
		
		@JsonRpcMethod("Test.custom2")
		String customMethod2(String stringParam1);

        @JsonRpcMethod("testMethodVararg")
        String testMethodVararg(Object... params);
	}
	
}
