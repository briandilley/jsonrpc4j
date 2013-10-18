package com.googlecode.jsonrpc4j;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for JsonRpcServer
 * 
 * @author Hans J??rgen Hoel (hansjorgen.hoel@nhst.no)
 *
 */
public class JsonRpcServerTest {

	private static final String JSON_ENCODING = "UTF-8";

	private ObjectMapper mapper;
	private ByteArrayOutputStream baos;

	private JsonRpcServer jsonRpcServer;

	private JsonRpcServer jsonRpcServerAnnotatedParam;

	@Before
	public void setup() {
		mapper = new ObjectMapper();
		baos = new ByteArrayOutputStream();
		jsonRpcServer = new JsonRpcServer(mapper, new Service(), ServiceInterface.class);
		jsonRpcServerAnnotatedParam = new JsonRpcServer(mapper, new Service(), ServiceInterfaceWithParamNameAnnotaion.class);
	}

    @Test
    public void receiveJsonRpcNotification() throws Exception {
        jsonRpcServer.handle(new ClassPathResource("jsonRpcServerNotificationTest.json").getInputStream(), baos);
        assertEquals(0, baos.size());
    }
	
	
	/////
	/// INDEXED PARAMETER TESTS BELOW
	/////
	
	@Test
	public void callMethodWithTooFewParameters() throws Exception {		
		jsonRpcServer.handle(new ClassPathResource("jsonRpcServerTooFewParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);
		
		// Invalid parameters
		assertEquals(-32602, json.get("error").get("code").intValue());		
	}
	
	@Test
	public void callMethodExactNumberOfParameters() throws Exception {
		jsonRpcServer.handle(new ClassPathResource("jsonRpcServerExactParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);
		
		assertEquals("success", json.get("result").textValue());
	}
	
	@Test
	public void callMethodWithExtraParameter() throws Exception {
		jsonRpcServer.handle(new ClassPathResource("jsonRpcServerExtraParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);
		
		// Invalid parameters
		assertEquals(-32602, json.get("error").get("code").intValue());
	}
	
	@Test
	public void callMethodWithTooFewParametersAllowOn() throws Exception {
		jsonRpcServer.setAllowLessParams(true);
		jsonRpcServer.handle(new ClassPathResource("jsonRpcServerTooFewParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);
		
		// Invalid parameters
		assertEquals("success", json.get("result").textValue());
	}
	
	@Test
	public void callMethodExactNumberOfParametersAllowOn() throws Exception {
		jsonRpcServer.setAllowExtraParams(true);
		jsonRpcServer.handle(new ClassPathResource("jsonRpcServerExactParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);
		
		assertEquals("success", json.get("result").textValue());
	}
	
	@Test
	public void callMethodWithExtraParameterAllowOn() throws Exception {
		jsonRpcServer.setAllowExtraParams(true);
		jsonRpcServer.handle(new ClassPathResource("jsonRpcServerExtraParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		assertEquals("success", json.get("result").textValue());
	}
	
	@Test
	public void callOverloadedMethodNoParams() throws Exception {
		jsonRpcServer.handle(new ClassPathResource("jsonRpcServerOverLoadedMethodNoParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		assertEquals("noParam", json.get("result").textValue());
	}
	
	@Test
	public void callOverloadedMethodOneStringParam() throws Exception {
		jsonRpcServer.handle(new ClassPathResource("jsonRpcServerOverLoadedMethodOneStringParamTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		assertEquals("stringParam1", json.get("result").textValue());
	}
	
	@Test
	public void callOverloadedMethodOneIntParam() throws Exception {
		jsonRpcServer.handle(new ClassPathResource("jsonRpcServerOverLoadedMethodOneIntParamTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		System.out.println("RESPONSE: "+response);
		JsonNode json = mapper.readTree(response);

		assertEquals("intParam1", json.get("result").textValue());
	}
	
	@Test
	public void callOverloadedMethodTwoStringParams() throws Exception {
		jsonRpcServer.handle(new ClassPathResource("jsonRpcServerOverLoadedMethodTwoStringParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		assertEquals("stringParam1, stringParam2", json.get("result").textValue());
	}
	
	@Test
	public void callOverloadedMethodTwoIntParams() throws Exception {
		jsonRpcServer.handle(new ClassPathResource("jsonRpcServerOverLoadedMethodTwoIntParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		assertEquals("intParam1, intParam2", json.get("result").textValue());
	}
	
	@Test
	public void callOverloadedMethodExtraParams() throws Exception {
		jsonRpcServer.setAllowExtraParams(true);
		jsonRpcServer.handle(new ClassPathResource("jsonRpcServerOverLoadedMethodExtraParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		assertEquals("stringParam1, stringParam2", json.get("result").textValue());
	}
	
	@Test
	public void callOverloadedMethodExtraParamsAllowOn() throws Exception {
		jsonRpcServer.setAllowExtraParams(true);
		jsonRpcServer.handle(new ClassPathResource("jsonRpcServerOverLoadedMethodExtraParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		assertEquals("stringParam1, stringParam2", json.get("result").textValue());
	}
	
	
	/////
	/// NAMED PARAMETER TESTS BELOW
	/////

	
	@Test
	public void callMethodWithTooFewParametersNamed() throws Exception {
		jsonRpcServerAnnotatedParam.handle(new ClassPathResource("jsonRpcServerTooFewParamsNamedTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);
		
		// Invalid parameters
		assertEquals(-32602, json.get("error").get("code").intValue());		
	}
	
	@Test
	public void callMethodExactNumberOfParametersNamed() throws Exception {
		jsonRpcServerAnnotatedParam.handle(new ClassPathResource("jsonRpcServerExactParamsNamedTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);
		
		assertEquals("success", json.get("result").textValue());
	}
	
	@Test
	public void callMethodWithExtraParameterNamed() throws Exception {
		jsonRpcServerAnnotatedParam.handle(new ClassPathResource("jsonRpcServerExtraParamsNamedTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);
		
		// Method not found
		assertEquals(-32602, json.get("error").get("code").intValue());
	}
	
	@Test
	public void callMethodWithTooFewParametersNamedAllowOn() throws Exception {
		jsonRpcServerAnnotatedParam.setAllowExtraParams(true);
		jsonRpcServerAnnotatedParam.handle(new ClassPathResource("jsonRpcServerTooFewParamsNamedTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);
		
		// Invalid parameters
		assertEquals(-32602, json.get("error").get("code").intValue());
	}
	
	@Test
	public void callMethodExactNumberOfParametersNamedAllowOn() throws Exception {
		jsonRpcServerAnnotatedParam.setAllowExtraParams(true);
		jsonRpcServerAnnotatedParam.handle(new ClassPathResource("jsonRpcServerExactParamsNamedTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);
		
		assertEquals("success", json.get("result").textValue());
	}
	
	@Test
	public void callMethodWithExtraParameterNamedAllowOn() throws Exception {
		jsonRpcServerAnnotatedParam.setAllowExtraParams(true);
		jsonRpcServerAnnotatedParam.handle(new ClassPathResource("jsonRpcServerExtraParamsNamedTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);
		
		assertEquals("success", json.get("result").textValue());
	}
	
	@Test
	public void callOverloadedMethodNoNamedParams() throws Exception {
		jsonRpcServerAnnotatedParam.handle(new ClassPathResource("jsonRpcServerOverLoadedMethodNoParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		assertEquals("noParam", json.get("result").textValue());
	}
	
	@Test
	public void callOverloadedMethodOneNamedStringParam() throws Exception {
		jsonRpcServerAnnotatedParam.handle(new ClassPathResource("jsonRpcServerOverLoadedMethodOneStringParamTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		assertEquals("stringParam1", json.get("result").textValue());
	}
	
	@Test
	public void callOverloadedMethodOneNamedIntParam() throws Exception {
		jsonRpcServerAnnotatedParam.handle(new ClassPathResource("jsonRpcServerOverLoadedMethodOneIntParamTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		assertEquals("intParam1", json.get("result").textValue());
	}
	
	@Test
	public void callOverloadedMethodTwoNamedStringParams() throws Exception {
		jsonRpcServerAnnotatedParam.handle(new ClassPathResource("jsonRpcServerOverLoadedMethodTwoStringParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		assertEquals("stringParam1, stringParam2", json.get("result").textValue());
	}
	
	@Test
	public void callOverloadedMethodTwoNamedIntParams() throws Exception {
		jsonRpcServerAnnotatedParam.handle(new ClassPathResource("jsonRpcServerOverLoadedMethodTwoIntParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		assertEquals("intParam1, intParam2", json.get("result").textValue());
	}
	
	@Test
	public void callOverloadedMethodNamedExtraParams() throws Exception {
		jsonRpcServerAnnotatedParam.handle(new ClassPathResource("jsonRpcServerOverLoadedMethodNamedExtraParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);
		
		// Invalid parameters
		assertEquals(-32602, json.get("error").get("code").intValue());
	}
	
	@Test
	public void callOverloadedMethodNamedExtraParamsAllowOn() throws Exception {
		jsonRpcServerAnnotatedParam.setAllowExtraParams(true);
		jsonRpcServerAnnotatedParam.handle(new ClassPathResource("jsonRpcServerOverLoadedMethodNamedExtraParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		assertEquals("stringParam1, stringParam2", json.get("result").textValue());
	}
	
	@Test
	public void callMethodWithoutRequiredParam() throws Exception {
		jsonRpcServerAnnotatedParam.handle(new ClassPathResource("jsonRpcServerWithoutRequiredNamedParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);
		
		// Invalid parameters
		assertEquals(-32602, json.get("error").get("code").intValue());
	}
	
	@Test
	public void callMethodWithoutRequiredParamAllowOn() throws Exception {
		jsonRpcServerAnnotatedParam.setAllowLessParams(true);
		jsonRpcServerAnnotatedParam.handle(new ClassPathResource("jsonRpcServerWithoutRequiredNamedParamsTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		assertEquals("stringParam1, null", json.get("result").textValue());
	}
	
	@Test
	public void idIntegerType() throws Exception {
		jsonRpcServer.handle(new ClassPathResource("jsonRpcServerIntegerIdTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		assertTrue(json.get("id").isIntegralNumber());
	}

	@Test
	public void idStringType() throws Exception {
		jsonRpcServer.handle(new ClassPathResource("jsonRpcServerStringIdTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		assertTrue(json.get("id").isTextual());
	}

	@Test
	public void noId() throws Exception {
		jsonRpcServer.handle(new ClassPathResource("jsonRpcServerNoIdTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		assertTrue(json.get("id").isNull());
	}

	@Test
	public void callParseErrorJson() throws Exception {
		jsonRpcServerAnnotatedParam.handle(new ClassPathResource(
				"jsonRpcParseErrorTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = mapper.readTree(response);

		// Invalid parameters
		assertEquals(-32700, json.get("error").get("code").asInt());
	}

	// Service and service interfaces used in test
	
	private interface ServiceInterface {        
		public String testMethod(String param1);
		public String overloadedMethod();
		public String overloadedMethod(String stringParam1);
		public String overloadedMethod(String stringParam1, String stringParam2);
		public String overloadedMethod(int intParam1);
		public String overloadedMethod(int intParam1, int intParam2);
	}
	
	private interface ServiceInterfaceWithParamNameAnnotaion {        
		public String testMethod(@JsonRpcParam("param1") String param1);    
		public String overloadedMethod();
		public String overloadedMethod(@JsonRpcParamName("param1") String stringParam1);
		public String overloadedMethod(@JsonRpcParamName("param1") String stringParam1, @JsonRpcParamName("param2") String stringParam2);
		public String overloadedMethod(@JsonRpcParamName("param1") int intParam1);
		public String overloadedMethod(@JsonRpcParamName("param1") int intParam1, @JsonRpcParamName("param2") int intParam2);
		
		public String methodWithoutRequiredParam(@JsonRpcParamName("param1") String stringParam1, @JsonRpcParamName(value="param2") String stringParam2);
	}

	private class Service implements ServiceInterface, ServiceInterfaceWithParamNameAnnotaion {
		public String testMethod(String param1) {
			return "success";
		}
		public String overloadedMethod() {
			return "noParam";
		}
		public String overloadedMethod(String stringParam1) {
			return stringParam1;
		}
		public String overloadedMethod(String stringParam1, String stringParam2) {
			return stringParam1+", "+stringParam2;
		}
		public String overloadedMethod(int intParam1) {
			return "intParam"+intParam1;
		}
		public String overloadedMethod(int intParam1, int intParam2) {
			return "intParam"+intParam1+", intParam"+intParam2;
		}

		public String methodWithoutRequiredParam(String stringParam1, String stringParam2) {
			return stringParam1+", "+stringParam2;
		}
		
	}
	
}
