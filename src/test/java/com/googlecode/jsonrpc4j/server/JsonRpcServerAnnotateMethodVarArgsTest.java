package com.googlecode.jsonrpc4j.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.VarArgsUtil;
import org.easymock.EasyMockRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.RESULT;
import static com.googlecode.jsonrpc4j.util.Util.*;

@RunWith(EasyMockRunner.class)
public class JsonRpcServerAnnotateMethodVarArgsTest {

	private ServiceWithVarArgsImpl mockService = new ServiceWithVarArgsImpl();
	
	private ByteArrayOutputStream byteArrayOutputStream;
	private JsonRpcBasicServer jsonRpcServerAnnotatedMethod;
	
	@Before
	public void setup() {
		byteArrayOutputStream = new ByteArrayOutputStream();
		jsonRpcServerAnnotatedMethod = new JsonRpcBasicServer(mapper, mockService, ServiceWithVarArgsImpl.class);
	}

	@Test
	public void callMethodWithVarArgParameters() throws Exception {
	    Map<String,Object> paramsMap = new HashMap<>();
        paramsMap.put("argOne","one");
        paramsMap.put("argTwo",2);
        paramsMap.put("argThree","three");
        paramsMap.put("argFour", 4);
        paramsMap.put("argFive", (Object)"five");
        paramsMap.put("argSix", 6.0f);
        paramsMap.put("argSeven", 7d);
        Object[] callParams = new Object[paramsMap.size() * 2];
        int i=0;
        for (Map.Entry<String,Object> entry : paramsMap.entrySet()) {
            callParams[i++] = entry.getKey();
            callParams[i++] = entry.getValue();
        }

        jsonRpcServerAnnotatedMethod.handleRequest(
                messageWithMapParamsStream("testMethodVararg", callParams),
				byteArrayOutputStream
		);
        JsonNode res = result();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode resultNode = mapper.readTree(res.asText());
        // params order saved during call, but order not guaranteed
        ObjectNode expectedResult = mapper.valueToTree(paramsMap);
        // all json numbers will be mapped to double
        expectedResult.set("argTwo", mapper.valueToTree(2d));
        expectedResult.set("argFour", mapper.valueToTree(4d));
        Assert.assertEquals(expectedResult.toString(), resultNode.toString());
    }

    private JsonNode result() throws IOException {
        return decodeAnswer(byteArrayOutputStream).get(RESULT);
    }

	public class ServiceWithVarArgsImpl implements ServiceInterfaceWithCustomMethodNameWithVarArgsAnnotation {

		@Override
		public String customMethod() {
			throw new AssertionError("this is not expected method");
		}

		@Override
		public String customMethod2(String stringParam1) {
            throw new AssertionError("this is not expected method");
		}

		@Override
		public String testMethodVararg(Object... params) {
            Map<String, Object> map = VarArgsUtil.convertArgs(params);
            try {
                return new ObjectMapper().writeValueAsString(map);
            } catch (JsonProcessingException e) {
                Assert.fail(e.getMessage());
                return null;
            }
        }
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
