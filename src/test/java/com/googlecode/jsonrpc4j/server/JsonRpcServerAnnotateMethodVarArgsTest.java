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
import jakarta.jws.WebParam;
import org.easymock.EasyMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.RESULT;
import static com.googlecode.jsonrpc4j.util.Util.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(EasyMockExtension.class)
public class JsonRpcServerAnnotateMethodVarArgsTest {

	private ServiceWithVarArgsImpl mockService = new ServiceWithVarArgsImpl();

	private ByteArrayOutputStream byteArrayOutputStream;
	private JsonRpcBasicServer jsonRpcServerAnnotatedMethod;

	@BeforeEach
	public void setup() {
		byteArrayOutputStream = new ByteArrayOutputStream();
		jsonRpcServerAnnotatedMethod = new JsonRpcBasicServer(mapper, mockService, ServiceInterfaceWithCustomMethodNameWithVarArgsAnnotation.class);
	}

	@Test
	public void callMethodWithVarArgParameters() throws Exception {
		Map<String, Object> paramsMap = new HashMap<>();
		paramsMap.put("argOne", "one");
		paramsMap.put("argTwo", 2);
		paramsMap.put("argThree", "three");
		paramsMap.put("argFour", 4);
		paramsMap.put("argFive", (Object) "five");
		paramsMap.put("argSix", 6.0f);
		paramsMap.put("argSeven", 7d);
		Object[] callParams = new Object[paramsMap.size() * 2];
		int i = 0;
		for (Map.Entry<String, Object> entry : paramsMap.entrySet()) {
			callParams[i++] = entry.getKey();
			callParams[i++] = entry.getValue();
		}

		jsonRpcServerAnnotatedMethod.handleRequest(
			messageWithMapParamsStream("varargObjectMethod", callParams),
			byteArrayOutputStream
		);
		JsonNode res = result();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode resultNode = mapper.readTree(res.asText());
		// params order saved during call, but order not guaranteed
		ObjectNode expectedResult = mapper.valueToTree(paramsMap);
		expectedResult.set("argTwo", mapper.valueToTree(2));
		expectedResult.set("argFour", mapper.valueToTree(4));
		assertEquals(expectedResult.toString(), resultNode.toString());
	}

	@Test
	public void callMethodWithVarArgStringAndWebParam() throws Exception {
		testVarargMethodWithParamAnnotation("varargStringMethodWithWebParam");
	}

	@Test
	public void callMethodWithVarArgStringAndJsonRpcParam() throws Exception {
		testVarargMethodWithParamAnnotation("varargStringMethodWithJsonRpcParam");
	}

	@Test
	public void callMethodWithVarArgPrimitive() throws Exception {
		jsonRpcServerAnnotatedMethod.handleRequest(
			messageWithListParamsStream(1, "varargPrimitiveMethod", 1, 2),
			byteArrayOutputStream
		);

		JsonNode res = result();
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode resultNode = (ArrayNode) mapper.readTree(res.asText());

		assertThat(resultNode.get(0).asInt(), is(equalTo(1)));
		assertThat(resultNode.get(1).asInt(), is(equalTo(2)));
	}

	@Test
	public void callMethodWithVarArgPrimitiveAndWebParam() throws Exception {
		testVarargMethodWithPrimitiveParamAnnotation("varargPrimitiveMethodWithWebParam");
	}

	@Test
	public void callMethodWithVarArgPrimitiveAndJsonRpcParam() throws Exception {
		testVarargMethodWithPrimitiveParamAnnotation("varargPrimitiveMethodWithJsonRpcParam");
	}

	@Test
	public void callMethodWithMixedStringVarargAndWebParam() throws Exception {
		testMixedMethod("mixedObjectStringVarargMethodWithWebParam");
	}

	@Test
	public void callMethodWithMixedStringVarargAndJsonRpcParam() throws Exception {
		testMixedMethod("mixedObjectStringVarargMethodWithJsonRpcParam");
	}

	@Test
	public void callMethodWithMixedPrimitiveVarargAndWebParam() throws Exception {
		testMixedMethodWithPrimitive("mixedObjectPrimitiveVarargMethodWithWebParam");
	}

	@Test
	public void callMethodWithMixedPrimitiveVarargAndJsonRpcParam() throws Exception {
		testMixedMethodWithPrimitive("mixedObjectPrimitiveVarargMethodWithJsonRpcParam");
	}

	private void testVarargMethodWithParamAnnotation(String methodName) throws Exception {
		String[] strings = new String[]{
			"foo",
			"bar"
		};

		Object[] args = new Object[] {
			"strings",
			strings
		};

		ArrayNode resultNode = callMethod(methodName, args);
		assertThat(resultNode.get(0).asText(), is(equalTo(strings[0])));
		assertThat(resultNode.get(1).asText(), is(equalTo(strings[1])));

		args = new Object[] {
			"strings",
			strings[0]
		};

		resultNode = callMethod(methodName, args);
		assertThat(resultNode.get(0).asText(), is(equalTo(strings[0])));
	}

	private void testVarargMethodWithPrimitiveParamAnnotation(String methodName) throws Exception {
		int[] ints = new int[]{
			1,
			2
		};

		Object[] args = new Object[] {
			"ints",
			ints
		};

		ArrayNode resultNode = callMethod(methodName, args);
		assertThat(resultNode.get(0).asInt(), is(equalTo(ints[0])));
		assertThat(resultNode.get(1).asInt(), is(equalTo(ints[1])));

		args = new Object[] {
			"ints",
			ints[0]
		};

		resultNode = callMethod(methodName, args);
		assertThat(resultNode.get(0).asInt(), is(equalTo(ints[0])));
	}

	@SuppressWarnings("unchecked")
	private void testMixedMethod(String methodName) throws Exception {
		Map<String, String> map = new HashMap<>();
		map.put("test", "value");

		String[] strings = new String[]{
			"foo",
			"bar"
		};

		Object[] args = new Object[] {
			"object",
			map,
			"strings",
			strings
		};

		ArrayNode resultNode = callMethod(methodName, args);
		Map<String, String> outputMap = mapper.convertValue(resultNode.get(0), Map.class);
		assertThat(outputMap, is(equalTo(map)));
		String[] outputStrings = mapper.convertValue(resultNode.get(1), String[].class);
		assertThat(outputStrings[0], is(equalTo(strings[0])));
		assertThat(outputStrings[1], is(equalTo(strings[1])));

		// test single item, non-array varargs

		args = new Object[] {
			"object",
			map,
			"strings",
			strings[0]
		};

		resultNode = callMethod(methodName, args);
		outputMap = mapper.convertValue(resultNode.get(0), Map.class);
		assertThat(outputMap, is(equalTo(map)));
		outputStrings = mapper.convertValue(resultNode.get(1), String[].class);
		assertThat(outputStrings[0], is(equalTo(strings[0])));
	}

	@SuppressWarnings("unchecked")
	private void testMixedMethodWithPrimitive(String methodName) throws Exception {
		Map<String, String> map = new HashMap<>();
		map.put("test", "value");

		int[] ints = new int[]{
			1,
			2
		};

		Object[] args = new Object[] {
			"object",
			map,
			"ints",
			ints
		};

		ArrayNode resultNode = callMethod(methodName, args);
		Map<String, String> outputMap = mapper.convertValue(resultNode.get(0), Map.class);
		assertThat(outputMap, is(equalTo(map)));
		int[] outputInts = mapper.convertValue(resultNode.get(1), int[].class);
		assertThat(outputInts[0], is(ints[0]));
		assertThat(outputInts[1], is(ints[1]));

		// test single item, non-array varargs

		args = new Object[]{
			"object",
			map,
			"ints",
			ints[0]
		};

		resultNode = callMethod(methodName, args);
		assertThat(outputMap, is(equalTo(map)));
		outputInts = mapper.convertValue(resultNode.get(1), int[].class);
		assertThat(outputInts[0], is(ints[0]));
	}

	private ArrayNode callMethod(String methodName, Object[] args) throws Exception {
		jsonRpcServerAnnotatedMethod.handleRequest(
			messageWithMapParamsStream(methodName, args),
			byteArrayOutputStream
		);

		JsonNode res = result();
		ObjectMapper mapper = new ObjectMapper();
		return (ArrayNode) mapper.readTree(res.asText());
	}

	private JsonNode result() throws IOException {
		return decodeAnswer(byteArrayOutputStream).get(RESULT);
	}

	public static class ServiceWithVarArgsImpl implements ServiceInterfaceWithCustomMethodNameWithVarArgsAnnotation {
		@Override
		public String customMethod() {
			throw new AssertionError("this is not expected method");
		}

		@Override
		public String customMethod2(String stringParam1) {
			throw new AssertionError("this is not expected method");
		}

		@Override
		public String varargObjectMethod(Object... params) {
			Map<String, Object> map = VarArgsUtil.convertArgs(params);

			try {
				return new ObjectMapper().writeValueAsString(map);
			} catch (JsonProcessingException e) {
				return null;
			}
		}

		@Override
		public String varargPrimitiveMethod(int... ints) {
			return writeInts(ints);
		}

		@Override
		public String varargPrimitiveMethodWithWebParam(int... ints) {
			return writeInts(ints);
		}

		@Override
		public String varargPrimitiveMethodWithJsonRpcParam(int... ints) {
			return writeInts(ints);
		}

		@Override
		public String varargStringMethodWithWebParam(String... strings) {
			return writeStrings(strings);
		}

		@Override
		public String varargStringMethodWithJsonRpcParam(String... strings) {
			return writeStrings(strings);
		}

		@Override
		public String mixedObjectStringVarargMethodWithWebParam(Object test, String... strings) {
			Object[] obj = new Object[]{
				test,
				strings
			};

			return writeObjects(obj);
		}

		@Override
		public String mixedObjectStringVarargMethodWithJsonRpcParam(Object test, String... strings) {
			Object[] obj = new Object[]{
				test,
				strings
			};

			return writeObjects(obj);
		}

		@Override
		public String mixedObjectPrimitiveVarargMethodWithWebParam(Object test, int... ints) {
			Object[] obj = new Object[]{
				test,
				ints
			};

			return writeObjects(obj);
		}

		@Override
		public String mixedObjectPrimitiveVarargMethodWithJsonRpcParam(Object test, int... ints) {
			Object[] obj = new Object[]{
				test,
				ints
			};

			return writeObjects(obj);
		}

		private String writeInts(int[] ints) {
			Integer[] boxed = new Integer[ints.length];

			for (int i = 0; i < ints.length; i++) {
				boxed[i] = ints[i];
			}

			return writeObjects(boxed);
		}

		private String writeStrings(String[] strings) {
			return writeObjects(strings);
		}

		private String writeObjects(Object[] objects) {
			try {
				return new ObjectMapper().writeValueAsString(objects);
			} catch (JsonProcessingException e) {
				return null;
			}
		}
	}

	public interface ServiceInterfaceWithCustomMethodNameWithVarArgsAnnotation {
		@JsonRpcMethod("Test.custom")
		String customMethod();

		@JsonRpcMethod("Test.custom2")
		String customMethod2(String stringParam1);

		@JsonRpcMethod("varargObjectMethod")
		String varargObjectMethod(Object... params);

		@JsonRpcMethod("varargPrimitiveMethod")
		String varargPrimitiveMethod(int... ints);

		@JsonRpcMethod("varargPrimitiveMethodWithWebParam")
		String varargPrimitiveMethodWithWebParam(@WebParam(name = "ints") int... ints);

		@JsonRpcMethod("varargPrimitiveMethodWithJsonRpcParam")
		String varargPrimitiveMethodWithJsonRpcParam(@JsonRpcParam("ints") int... ints);

		@JsonRpcMethod("varargStringMethodWithWebParam")
		String varargStringMethodWithWebParam(@WebParam(name = "strings") String... strings);

		@JsonRpcMethod("varargStringMethodWithJsonRpcParam")
		String varargStringMethodWithJsonRpcParam(@JsonRpcParam("strings") String... strings);

		@JsonRpcMethod("mixedObjectStringVarargMethodWithWebParam")
		String mixedObjectStringVarargMethodWithWebParam(@WebParam(name = "object") Object test,
														 @WebParam(name = "strings") String... strings);

		@JsonRpcMethod("mixedObjectStringVarargMethodWithJsonRpcParam")
		String mixedObjectStringVarargMethodWithJsonRpcParam(@JsonRpcParam("object") Object test,
															 @JsonRpcParam("strings") String... strings);

		@JsonRpcMethod("mixedObjectPrimitiveVarargMethodWithWebParam")
		String mixedObjectPrimitiveVarargMethodWithWebParam(@WebParam(name = "object") Object test,
															@WebParam(name = "ints") int... ints);

		@JsonRpcMethod("mixedObjectPrimitiveVarargMethodWithJsonRpcParam")
		String mixedObjectPrimitiveVarargMethodWithJsonRpcParam(@JsonRpcParam("object") Object test,
																@JsonRpcParam("ints") int... ints);
	}
}
