package com.googlecode.jsonrpc4j;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MultiServiceTest {

	private static final String JSON_ENCODING = "UTF-8";

	private JsonRpcMultiServer multiServer;
	private ByteArrayOutputStream baos;

	@Before
	public void setup() {
		multiServer = new JsonRpcMultiServer();
		multiServer.addService("Test", new Service(), ServiceInterfaceWithParamNameAnnotaion.class);
		baos = new ByteArrayOutputStream();
	}

	@Test
	public void callMethodExactNumberOfParametersNamed() throws Exception {		
		multiServer.handle(new ClassPathResource("jsonRpcMultiServerExactParamsNamedTest.json").getInputStream(), baos);

		String response = baos.toString(JSON_ENCODING);
		JsonNode json = new ObjectMapper().readTree(response);
		
		assertEquals("success", json.get("result").textValue());
	}

	private interface ServiceInterfaceWithParamNameAnnotaion {        
		public String testMethod(@JsonRpcParam("param1") String param1);    
		public String overloadedMethod();
		public String overloadedMethod(@JsonRpcParam("param1") String stringParam1);
		public String overloadedMethod(@JsonRpcParam("param1") String stringParam1, @JsonRpcParam("param2") String stringParam2);
		public String overloadedMethod(@JsonRpcParam("param1") int intParam1);
		public String overloadedMethod(@JsonRpcParam("param1") int intParam1, @JsonRpcParam("param2") int intParam2);
		
		public String methodWithoutRequiredParam(@JsonRpcParam("param1") String stringParam1, @JsonRpcParam(value="param2") String stringParam2);
	}

	private class Service implements ServiceInterfaceWithParamNameAnnotaion {
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
