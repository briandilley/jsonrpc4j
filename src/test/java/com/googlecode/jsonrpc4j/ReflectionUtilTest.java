package com.googlecode.jsonrpc4j;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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

	@Test
	public void someNamedParams() throws Exception {

		assertThrows(RuntimeException.class, () -> ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("someNamedParams", String.class, int.class), null));
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

	@Test
	public void someNamedParamsPassParamsAuto() throws Exception {

		assertThrows(RuntimeException.class, () -> ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("someNamedParamsPassParamsAuto", String.class, int.class), null));
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

	@Test
	public void someNamedParamsPassParamsArray() throws Exception {

		assertThrows(RuntimeException.class, () -> ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("someNamedParamsPassParamsArray", String.class, int.class), null));
	}
	
	@Test
	public void allNamedParamsPassParamsArray() throws Exception {
		
		Object[] arguments = {"1", 2};
		Object[] params = (Object[]) ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("allNamedParamsPassParamsArray", String.class, int.class), arguments);
		
		assertEquals(2, params.length);
		assertEquals("1", params[0]);
		assertEquals(2, params[1]);
	}

	@Test
	public void noNamedParamsPassParamsObject() throws Exception {
		
		Object[] arguments = {"1", 2};
		assertThrows(RuntimeException.class, () -> ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("noNamedParamsPassParamsObject", String.class, int.class), arguments));
	}

	@Test
	public void someNamedParamsPassParamsObject() throws Exception {

		assertThrows(RuntimeException.class, () -> ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("someNamedParamsPassParamsObject", String.class, int.class), null));
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

		@JsonRpcMethod(value = "allNamedParamsPassParamsAuto", paramsPassMode = JsonRpcParamsPassMode.AUTO)
		void allNamedParamsPassParamsAuto(@JsonRpcParam("one") String one, @JsonRpcParam("two") int two);

		@JsonRpcMethod(value = "noNamedParamsPassParamsArray", paramsPassMode = JsonRpcParamsPassMode.ARRAY)
		void noNamedParamsPassParamsArray(String one, int two);

		@JsonRpcMethod(value = "someNamedParamsPassParamsArray", paramsPassMode = JsonRpcParamsPassMode.ARRAY)
		void someNamedParamsPassParamsArray(@JsonRpcParam("one") String one, int two);

		@JsonRpcMethod(value = "allNamedParamsPassParamsArray", paramsPassMode = JsonRpcParamsPassMode.ARRAY)
		void allNamedParamsPassParamsArray(@JsonRpcParam("one") String one, @JsonRpcParam("two") int two);

		@JsonRpcMethod(value = "noNamedParamsPassParamsObject", paramsPassMode = JsonRpcParamsPassMode.OBJECT)
		void noNamedParamsPassParamsObject(String one, int two);

		@JsonRpcMethod(value = "someNamedParamsPassParamsObject", paramsPassMode = JsonRpcParamsPassMode.OBJECT)
		void someNamedParamsPassParamsObject(@JsonRpcParam("one") String one, int two);

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
