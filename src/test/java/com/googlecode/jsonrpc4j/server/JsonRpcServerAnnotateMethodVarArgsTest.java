package com.googlecode.jsonrpc4j.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.VarArgsUtil;
import org.easymock.EasyMockRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.jws.WebParam;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.RESULT;
import static com.googlecode.jsonrpc4j.util.Util.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(EasyMockRunner.class)
public class JsonRpcServerAnnotateMethodVarArgsTest {

	private ServiceWithVarArgsImpl mockService = new ServiceWithVarArgsImpl();
	
	private ByteArrayOutputStream byteArrayOutputStream;
	private JsonRpcBasicServer jsonRpcServerAnnotatedMethod;
	
	@Before
	public void setup() {
		byteArrayOutputStream = new ByteArrayOutputStream();
		jsonRpcServerAnnotatedMethod = new JsonRpcBasicServer(mapper, mockService, ServiceInterfaceWithCustomMethodNameWithVarArgsAnnotation.class);
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
        expectedResult.set("argTwo", mapper.valueToTree(2));
        expectedResult.set("argFour", mapper.valueToTree(4));
        Assert.assertEquals(expectedResult.toString(), resultNode.toString());
    }

	@Test
	public void callMethodWithVarArgStringAndWebParam() throws Exception {
		testVarargMethodWithParamAnnotation("testMethodVarargStringWebParam");
	}

	@Test
	public void callMethodWithVarArgStringAndJsonRpcParam() throws Exception {
		testVarargMethodWithParamAnnotation("testMethodVarargStringJsonRpcParam");
	}

	@Test
	public void callMethodWithVarArgPrimitive() throws Exception {
		jsonRpcServerAnnotatedMethod.handleRequest(
			messageWithListParamsStream(1, "testMethodVarargPrimitive", 1, 2),
			byteArrayOutputStream
		);

		JsonNode res = result();
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode resultNode = (ArrayNode) mapper.readTree(res.asText());

		Assert.assertThat(resultNode.get(0).asInt(), is(equalTo(1)));
		Assert.assertThat(resultNode.get(1).asInt(), is(equalTo(2)));
	}

	@Test
	public void callMethodWithVarArgPrimitiveAndWebParam() throws Exception {
		testVarargMethodWithPrimitiveParamAnnotation("testMethodVarargPrimitiveWebParam");
	}

	@Test
	public void callMethodWithVarArgPrimitiveAndJsonRpcParam() throws Exception {
		testVarargMethodWithPrimitiveParamAnnotation("testMethodVarargPrimitiveJsonRpcParam");
	}

	private void testVarargMethodWithParamAnnotation(String methodName) throws Exception {
		String[] strings = new String[] {
			"foo",
			"bar"
		};

		Object[] args = {
			"strings",
			strings
		};

		jsonRpcServerAnnotatedMethod.handleRequest(
			messageWithMapParamsStream(methodName, args),
			byteArrayOutputStream
		);

		JsonNode res = result();
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode resultNode = (ArrayNode) mapper.readTree(res.asText());

		Assert.assertThat(resultNode.get(0).asText(), is(equalTo("foo")));
		Assert.assertThat(resultNode.get(1).asText(), is(equalTo("bar")));
	}

	private void testVarargMethodWithPrimitiveParamAnnotation(String methodName) throws Exception {
		int[] ints = new int[] {
			1,
			2
		};

		Object[] args = {
			"ints",
			ints
		};

		jsonRpcServerAnnotatedMethod.handleRequest(
			messageWithMapParamsStream(methodName, args),
			byteArrayOutputStream
		);

		JsonNode res = result();
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode resultNode = (ArrayNode) mapper.readTree(res.asText());

		Assert.assertThat(resultNode.get(0).asInt(), is(equalTo(1)));
		Assert.assertThat(resultNode.get(1).asInt(), is(equalTo(2)));
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

		@Override
		public String testMethodVarargPrimitive(int... ints) {
			try {
				return new ObjectMapper().writeValueAsString(ints);
			} catch (JsonProcessingException e) {
				Assert.fail(e.getMessage());
				return null;
			}
		}

		@Override
		public String testMethodVarargPrimitiveWebParam(int... ints) {
			try {
				return new ObjectMapper().writeValueAsString(ints);
			} catch (JsonProcessingException e) {
				Assert.fail(e.getMessage());
				return null;
			}
		}

		@Override
		public String testMethodVarargPrimitiveJsonRpcParam(int... ints) {
			try {
				return new ObjectMapper().writeValueAsString(ints);
			} catch (JsonProcessingException e) {
				Assert.fail(e.getMessage());
				return null;
			}
		}

		@Override
		public String testMethodVarargWebParam(String... strings) {
			try {
				return new ObjectMapper().writeValueAsString(strings);
			} catch (JsonProcessingException e) {
				Assert.fail(e.getMessage());
				return null;
			}
		}

		@Override
		public String testMethodVarargJsonRpcParam(String... strings) {
			try {
				return new ObjectMapper().writeValueAsString(strings);
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

        @JsonRpcMethod("testMethodVarargPrimitive")
		String testMethodVarargPrimitive(int... ints);

        @JsonRpcMethod("testMethodVarargPrimitiveWebParam")
		String testMethodVarargPrimitiveWebParam(@WebParam(name = "ints") int... ints);

		@JsonRpcMethod("testMethodVarargPrimitiveJsonRpcParam")
		String testMethodVarargPrimitiveJsonRpcParam(@JsonRpcParam("ints") int... ints);

        @JsonRpcMethod("testMethodVarargStringWebParam")
		String testMethodVarargWebParam(@WebParam(name = "strings") String... strings);

        @JsonRpcMethod("testMethodVarargStringJsonRpcParam")
		String testMethodVarargJsonRpcParam(@JsonRpcParam("strings") String... strings);
	}
}
