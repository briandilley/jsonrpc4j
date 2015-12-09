package com.googlecode.jsonrpc4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Map;

import org.junit.Test;

public class ReflectionUtilTest {

	public static interface JsonRpcTestService {

		void noParams();

		void noNamedParams(String one, int two);

		void someNamedParams(@JsonRpcParam("one") String one, int two);

		void allNamedParams(@JsonRpcParam("one") String one, @JsonRpcParam("two") int two);
	}

	@Test
	public void noParams() throws Exception {

		assertEquals(0, ((Object[]) ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("noParams"), null)).length);

		Object[] arguments = new Object[0];
		assertSame(arguments, ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("noParams"), arguments));
	}

	@Test
	public void noNamedParams() throws Exception {

		Object[] arguments = { "1", 2 };
		assertSame(arguments, ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("noNamedParams", String.class, int.class), arguments));
	}

	@Test(expected=RuntimeException.class)
	public void someNamedParams() throws Exception {

		ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("someNamedParams", String.class, int.class), null);
	}

	@Test
	public void allNamedParams() throws Exception {

		Object[] arguments = { "1", 2 };
		Map<String, Object> namedParams = (Map<String, Object>) ReflectionUtil.parseArguments(JsonRpcTestService.class.getMethod("allNamedParams", String.class, int.class), arguments);

		assertEquals(2, namedParams.size());
		assertEquals("1", namedParams.get("one"));
		assertEquals(2, namedParams.get("two"));
	}
}
