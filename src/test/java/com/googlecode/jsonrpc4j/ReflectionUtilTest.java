package com.googlecode.jsonrpc4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class ReflectionUtilTest {
	
	@Test
	public void noParams() throws Exception {
		
		assertEquals(0, ((Object[]) ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("noParams"), null)).length);
		
		Object[] arguments = new Object[0];
		assertSame(arguments, ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("noParams"), arguments));
	}
	
	@Test
	public void noNamedParams() throws Exception {
		
		Object[] arguments = {"1", 2};
		assertSame(arguments, ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("noNamedParams", String.class, int.class), arguments));
	}
	
	@Test(expected = RuntimeException.class)
	public void someNamedParams() throws Exception {
		
		ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("someNamedParams", String.class, int.class), null);
	}
	
	@Test
	public void allNamedParams() throws Exception {
		
		Object[] arguments = {"1", 2};
		@SuppressWarnings("unchecked")
		Map<String, Object> namedParams = (Map<String, Object>) ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("allNamedParams", String.class, int.class), arguments);
		
		assertEquals(2, namedParams.size());
		assertEquals("1", namedParams.get("one"));
		assertEquals(2, namedParams.get("two"));
	}
	
	@Test
	public void noNamedParamsPassParamsAuto() throws Exception {
		
		Object[] arguments = {"1", 2};
		assertSame(arguments, ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("noNamedParamsPassParamsAuto", String.class, int.class), arguments));
	}
	
	@Test(expected = RuntimeException.class)
	public void someNamedParamsPassParamsAuto() throws Exception {
		
		ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("someNamedParamsPassParamsAuto", String.class, int.class), null);
	}

	@Test
	public void fixedParamPassParamsAuto() throws Exception {
		Object[] arguments = { "1", 2 };

		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) ReflectionUtil.parseArguments(
				JsonRpcTestService.class.getMethod("fixedParamPassParamsAuto", String.class, int.class), arguments);

		assertEquals(3, params.size());
		assertEquals("1", params.get("one"));
		assertEquals(2, params.get("two"));
		assertEquals("value1", params.get("param1"));
	}

	@Test
	public void fixedParamsPassParamsAuto() throws Exception {
		Object[] arguments = { "1", 2 };

		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) ReflectionUtil.parseArguments(
				JsonRpcTestService.class.getMethod("fixedParamsPassParamsAuto", String.class, int.class), arguments);

		assertEquals(4, params.size());
		assertEquals("1", params.get("one"));
		assertEquals(2, params.get("two"));
		assertEquals("value1", params.get("param1"));
		assertEquals("value2", params.get("param2"));
	}
	
	@Test
	public void allNamedParamsPassParamsAuto() throws Exception {
		
		Object[] arguments = {"1", 2};
		@SuppressWarnings("unchecked")
		Map<String, Object> namedParams = (Map<String, Object>) ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("allNamedParamsPassParamsAuto", String.class, int.class), arguments);
		
		assertEquals(2, namedParams.size());
		assertEquals("1", namedParams.get("one"));
		assertEquals(2, namedParams.get("two"));
	}
	
	@Test
	public void noNamedParamsPassParamsArray() throws Exception {
		
		Object[] arguments = {"1", 2};
		assertSame(arguments, ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("noNamedParamsPassParamsArray", String.class, int.class), arguments));
	}
	
	@Test(expected = RuntimeException.class)
	public void someNamedParamsPassParamsArray() throws Exception {
		
		ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("someNamedParamsPassParamsArray", String.class, int.class), null);
	}

	@Test
	public void fixedParamPassParamsArray() throws Exception {
		Object[] arguments = { "1", 2 };
		
		Object[] params = (Object[]) ReflectionUtil.parseArguments(
				JsonRpcTestService.class.getMethod("fixedParamPassParamsArray", String.class, int.class), arguments);

		assertEquals(3, params.length);
		assertEquals("value1", params[0]);
		assertEquals("1", params[1]);
		assertEquals(2, params[2]);
	}

	@Test
	public void fixedParamsPassParamsArray() throws Exception {
		Object[] arguments = { "1", 2 };

		Object[] params = (Object[]) ReflectionUtil.parseArguments(
				JsonRpcTestService.class.getMethod("fixedParamsPassParamsArray", String.class, int.class), arguments);

		assertEquals(4, params.length);
		assertEquals("value1", params[0]);
		assertEquals("value2", params[1]);
		assertEquals("1", params[2]);
		assertEquals(2, params[3]);
	}
	
	@Test
	public void allNamedParamsPassParamsArray() throws Exception {
		
		Object[] arguments = {"1", 2};
		Object[] params = (Object[]) ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("allNamedParamsPassParamsArray", String.class, int.class), arguments);
		
		assertEquals(2, params.length);
		assertEquals("1", params[0]);
		assertEquals(2, params[1]);
	}
	
	@Test(expected = RuntimeException.class)
	public void noNamedParamsPassParamsObject() throws Exception {
		
		Object[] arguments = {"1", 2};
		ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("noNamedParamsPassParamsObject", String.class, int.class), arguments);
	}
	
	@Test(expected = RuntimeException.class)
	public void someNamedParamsPassParamsObject() throws Exception {
		
		ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("someNamedParamsPassParamsObject", String.class, int.class), null);
	}

	@Test
	public void fixedParamPassParamsObject() throws Exception {
		Object[] arguments = { "1", 2 };

		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) ReflectionUtil.parseArguments(
				JsonRpcTestService.class.getMethod("fixedParamPassParamsObject", String.class, int.class), arguments);

		assertEquals(3, params.size());
		assertEquals("1", params.get("one"));
		assertEquals(2, params.get("two"));
		assertEquals("value1", params.get("param1"));
	}

	@Test
	public void fixedParamsPassParamsObject() throws Exception {
		Object[] arguments = { "1", 2 };

		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) ReflectionUtil.parseArguments(
				JsonRpcTestService.class.getMethod("fixedParamsPassParamsObject", String.class, int.class), arguments);

		assertEquals(4, params.size());
		assertEquals("1", params.get("one"));
		assertEquals(2, params.get("two"));
		assertEquals("value1", params.get("param1"));
		assertEquals("value2", params.get("param2"));
	}
	
	@Test
	public void allNamedParamsPassParamsObject() throws Exception {
		
		Object[] arguments = {"1", 2};
		@SuppressWarnings("unchecked")
		Map<String, Object> namedParams = (Map<String, Object>) ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("allNamedParamsPassParamsAuto", String.class, int.class), arguments);
		
		assertEquals(2, namedParams.size());
		assertEquals("1", namedParams.get("one"));
		assertEquals(2, namedParams.get("two"));
	}
	
  @Test
  public void sameNameObjectParamJsonRpcMethod() {
    Set<Method> methods = ReflectionUtil.findCandidateMethods(new Class<?>[] { JsonRpcTestService.class }, "objectParamSameName");
    assertEquals(1, methods.size());
    methods = ReflectionUtil.findCandidateMethods(new Class<?>[] { JsonRpcTestService.class }, "diffMethodName");
    assertEquals(1, methods.size());
  }

	private interface JsonRpcTestService {
		
		void noParams();
		
		void noNamedParams(String one, int two);
		
		void someNamedParams(@JsonRpcParam("one") String one, int two);
		
		void allNamedParams(@JsonRpcParam("one") String one, @JsonRpcParam("two") int two);

		@JsonRpcMethod(value = "noNamedParamsPassParamsAuto", paramsPassMode = JsonRpcParamsPassMode.AUTO)
		void noNamedParamsPassParamsAuto(String one, int two);

		@JsonRpcMethod(value = "someNamedParamsPassParamsAuto", paramsPassMode = JsonRpcParamsPassMode.AUTO)
		void someNamedParamsPassParamsAuto(@JsonRpcParam("one") String one, int two);

		@JsonRpcMethod(value = "fixedParamPassParamsAuto", paramsPassMode = JsonRpcParamsPassMode.AUTO)
		@JsonRpcFixedParam(name = "param1", value = "value1")
		void fixedParamPassParamsAuto(@JsonRpcParam("one") String one, @JsonRpcParam("two") int two);

		@JsonRpcMethod(value = "fixedParamsPassParamsAuto", paramsPassMode = JsonRpcParamsPassMode.AUTO)
		@JsonRpcFixedParams(fixedParams = { @JsonRpcFixedParam(name = "param1", value = "value1"),
				@JsonRpcFixedParam(name = "param2", value = "value2") })
		void fixedParamsPassParamsAuto(@JsonRpcParam("one") String one, @JsonRpcParam("two") int two);

		@JsonRpcMethod(value = "allNamedParamsPassParamsAuto", paramsPassMode = JsonRpcParamsPassMode.AUTO)
		void allNamedParamsPassParamsAuto(@JsonRpcParam("one") String one, @JsonRpcParam("two") int two);

		@JsonRpcMethod(value = "noNamedParamsPassParamsArray", paramsPassMode = JsonRpcParamsPassMode.ARRAY)
		void noNamedParamsPassParamsArray(String one, int two);

		@JsonRpcMethod(value = "someNamedParamsPassParamsArray", paramsPassMode = JsonRpcParamsPassMode.ARRAY)
		void someNamedParamsPassParamsArray(@JsonRpcParam("one") String one, int two);

		@JsonRpcMethod(value = "fixedParamPassParamsArray", paramsPassMode = JsonRpcParamsPassMode.ARRAY)
		@JsonRpcFixedParam(name = "param1", value = "value1")
		void fixedParamPassParamsArray(@JsonRpcParam("one") String one, @JsonRpcParam("two") int two);

		@JsonRpcMethod(value = "fixedParamsPassParamsArray", paramsPassMode = JsonRpcParamsPassMode.ARRAY)
		@JsonRpcFixedParams(fixedParams = { @JsonRpcFixedParam(name = "param1", value = "value1"),
				@JsonRpcFixedParam(name = "param2", value = "value2") })
		void fixedParamsPassParamsArray(@JsonRpcParam("one") String one, @JsonRpcParam("two") int two);

		@JsonRpcMethod(value = "allNamedParamsPassParamsArray", paramsPassMode = JsonRpcParamsPassMode.ARRAY)
		void allNamedParamsPassParamsArray(@JsonRpcParam("one") String one, @JsonRpcParam("two") int two);

		@JsonRpcMethod(value = "noNamedParamsPassParamsObject", paramsPassMode = JsonRpcParamsPassMode.OBJECT)
		void noNamedParamsPassParamsObject(String one, int two);

		@JsonRpcMethod(value = "someNamedParamsPassParamsObject", paramsPassMode = JsonRpcParamsPassMode.OBJECT)
		void someNamedParamsPassParamsObject(@JsonRpcParam("one") String one, int two);

		@JsonRpcMethod(value = "fixedParamPassParamsObject", paramsPassMode = JsonRpcParamsPassMode.OBJECT)
		@JsonRpcFixedParam(name = "param1", value = "value1")
		void fixedParamPassParamsObject(@JsonRpcParam("one") String one, @JsonRpcParam("two") int two);

		@JsonRpcMethod(value = "fixedParamsPassParamsObject", paramsPassMode = JsonRpcParamsPassMode.OBJECT)
		@JsonRpcFixedParams(fixedParams = { @JsonRpcFixedParam(name = "param1", value = "value1"),
				@JsonRpcFixedParam(name = "param2", value = "value2") })
		void fixedParamsPassParamsObject(@JsonRpcParam("one") String one, @JsonRpcParam("two") int two);

		@JsonRpcMethod(value = "allNamedParamsPassParamsObject", paramsPassMode = JsonRpcParamsPassMode.OBJECT)
		void allNamedParamsPassParamsObject(@JsonRpcParam("one") String one, @JsonRpcParam("two") int two);

    void objectParamSameName(Object1 obj);

    @JsonRpcMethod(value="diffMethodName", required=true)
    void objectParamSameName(Object2 obj);
	}

  private static class Object1 {
    String foo;

    public String getFoo() {
      return foo;
    }

    public void setFoo(String foo) {
      this.foo = foo;
    }
  }

  private static class Object2 {
    String foo;

    public String getFoo() {
      return foo;
    }

    public void setFoo(String foo) {
      this.foo = foo;
    }

  }

}
