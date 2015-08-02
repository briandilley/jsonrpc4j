package com.googlecode.jsonrpc4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.Assert.*;

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

	private JsonRpcBasicServer jsonRpcServer;

	private JsonRpcBasicServer jsonRpcServerAnnotatedParam;

    private JsonRpcBasicServer jsonRpcServerAnnotatedMethod;

	@Before
	public void setup() {
		mapper = new ObjectMapper();
		baos = new ByteArrayOutputStream();
		jsonRpcServer = new JsonRpcBasicServer(mapper, new Service(), ServiceInterface.class);
		jsonRpcServerAnnotatedParam = new JsonRpcBasicServer(mapper, new Service(), ServiceInterfaceWithParamNameAnnotation.class);
        jsonRpcServerAnnotatedMethod = new JsonRpcBasicServer(mapper, new Service(), ServiceInterfaceWithCustomMethodNameAnnotation.class);
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

    @Test
    public void callMethodWithCustomMethodNameTest() throws Exception {
        jsonRpcServerAnnotatedMethod.handle(new ClassPathResource("jsonRpcServerCustomMethodNameTest.json").getInputStream(), baos);

        String response = baos.toString(JSON_ENCODING);
        JsonNode json = mapper.readTree(response);

        assertEquals("custom", json.get("result").textValue());
    }

    @Test
    public void callMethodWithoutCustomMethodNameTest() throws Exception {
        jsonRpcServerAnnotatedMethod.handle(new ClassPathResource("jsonRpcServerNotCustomMethodNameTest.json").getInputStream(), baos);

        String response = baos.toString(JSON_ENCODING);
        JsonNode json = mapper.readTree(response);

        assertEquals("custom", json.get("result").textValue());
    }


    @Test
    public void callMethodWithCustomMethodNameAndParamTest() throws Exception {
        jsonRpcServerAnnotatedMethod.handle(new ClassPathResource("jsonRpcServerCustomMethodNameAndParamTest.json").getInputStream(), baos);

        String response = baos.toString(JSON_ENCODING);
        JsonNode json = mapper.readTree(response);

        assertEquals("custom2", json.get("result").textValue());
    }

    @Test
    public void callMethodWithoutCustomMethodNameAndParamTest() throws Exception {
        jsonRpcServerAnnotatedMethod.handle(new ClassPathResource("jsonRpcServerNotCustomMethodNameAndParamTest.json").getInputStream(), baos);

        String response = baos.toString(JSON_ENCODING);
        JsonNode json = mapper.readTree(response);

        assertEquals("custom2", json.get("result").textValue());
    }

    /**
     * The {@link com.googlecode.jsonrpc4j.JsonRpcBasicServer} is able to have an instance of
     * {@link com.googlecode.jsonrpc4j.InvocationListener} configured for it.  Prior to a
     * method being invoked, the lister is notified and after the method is invoked, the
     * listener is notified.  This test checks that these two events are hit correctly in
     * the case that an exception is raised when the method is invoked.
     */

    @Test
    public void callMethodThrowingWithInvocationListener() throws Exception {

        Mockery mockCtx = new Mockery();
        final Sequence sequence = mockCtx.sequence("listener");
        final InvocationListener invocationListener = mockCtx.mock(InvocationListener.class);

        mockCtx.checking(new Expectations() {{

           oneOf(invocationListener).willInvoke(
                   with(equal(ServiceInterface.class.getMethod("throwsMethod", String.class))),
                   with(any(List.class))
           ); inSequence(sequence);

            oneOf(invocationListener).didInvoke(
                    with(equal(ServiceInterface.class.getMethod("throwsMethod", String.class))),
                    with(any(List.class)),
                    with(aNull(Object.class)),
                    with(aNonNull(Throwable.class)),
                    with(Matchers.greaterThanOrEqualTo(0l))
            ); inSequence(sequence);

        }});

        jsonRpcServer = new JsonRpcBasicServer(mapper, new Service(), ServiceInterface.class);
        jsonRpcServer.setInvocationListener(invocationListener);
        jsonRpcServer.handle(new ClassPathResource("jsonRpcServerMethodThrowingWithInvocationListener.json").getInputStream(), baos);

        String response = baos.toString(JSON_ENCODING);

        JsonNode json = mapper.readTree(response);

        assertNull(json.get("result"));
        assertNotNull(json.get("error"));

        //mockCtx.assertIsSatisfied();

    }

    /**
     * The {@link com.googlecode.jsonrpc4j.JsonRpcBasicServer} is able to have an instance of
     * {@link com.googlecode.jsonrpc4j.InvocationListener} configured for it.  Prior to a
     * method being invoked, the lister is notified and after the method is invoked, the
     * listener is notified.  This test checks that these two events are hit correctly
     * when a method is invoked.
     */

    @Test
    public void callMethodWithInvocationListener() throws Exception {

        Mockery mockCtx = new Mockery();
        final Sequence sequence = mockCtx.sequence("listener");
        final InvocationListener invocationListener = mockCtx.mock(InvocationListener.class);

        mockCtx.checking(new Expectations() {{

            oneOf(invocationListener).willInvoke(
                    with(equal(ServiceInterface.class.getMethod("testMethod", String.class))),
                    with(any(List.class))
            ); inSequence(sequence);

            oneOf(invocationListener).didInvoke(
                    with(equal(ServiceInterface.class.getMethod("testMethod", String.class))),
                    with(any(List.class)),
                    with(equal(new TextNode("success"))),
                    with(aNull(Throwable.class)),
                    with(Matchers.greaterThanOrEqualTo(0l))
            ); inSequence(sequence);

        }});

        jsonRpcServer = new JsonRpcBasicServer(mapper, new Service(), ServiceInterface.class);
        jsonRpcServer.setInvocationListener(invocationListener);
        jsonRpcServer.handle(new ClassPathResource("jsonRpcServerMethodWithInvocationListener.json").getInputStream(), baos);

        String response = baos.toString(JSON_ENCODING);

        JsonNode json = mapper.readTree(response);

        assertNotNull(json.get("result"));
        assertNull(json.get("error"));

        mockCtx.assertIsSatisfied();

    }

	// Service and service interfaces used in test
	
	private interface ServiceInterface {        
		public String testMethod(String param1);
		public String overloadedMethod();
		public String overloadedMethod(String stringParam1);
		public String overloadedMethod(String stringParam1, String stringParam2);
		public String overloadedMethod(int intParam1);
		public String overloadedMethod(int intParam1, int intParam2);
        public String throwsMethod(String param1) throws TestException;
	}
	
	private interface ServiceInterfaceWithParamNameAnnotation {
		public String testMethod(@JsonRpcParam("param1") String param1);    
		public String overloadedMethod();
		public String overloadedMethod(@JsonRpcParam("param1") String stringParam1);
		public String overloadedMethod(@JsonRpcParam("param1") String stringParam1, @JsonRpcParam("param2") String stringParam2);
		public String overloadedMethod(@JsonRpcParam("param1") int intParam1);
		public String overloadedMethod(@JsonRpcParam("param1") int intParam1, @JsonRpcParam("param2") int intParam2);
		
		public String methodWithoutRequiredParam(@JsonRpcParam("param1") String stringParam1, @JsonRpcParam(value="param2") String stringParam2);
	}

    private interface ServiceInterfaceWithCustomMethodNameAnnotation {
        @JsonRpcMethod("Test.custom")
        public String customMethod();

        @JsonRpcMethod("Test.custom2")
        public String customMethod2(String stringParam1);
    }

	private class Service implements ServiceInterface, ServiceInterfaceWithParamNameAnnotation,
            ServiceInterfaceWithCustomMethodNameAnnotation {
		public String testMethod(String param1) {
			return "success";
		}
        public String customMethod() {
            return "custom";
        }

        public String customMethod2(String stringParam1) {
            return "custom2";
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

        public String throwsMethod(String param1) throws TestException {
            throw new TestException("throwsMethod");
        }

    }

}
