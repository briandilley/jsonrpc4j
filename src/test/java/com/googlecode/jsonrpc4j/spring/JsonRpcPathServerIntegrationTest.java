package com.googlecode.jsonrpc4j.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * @deprecated this test should be removed (replaced by {@link JsonRpcPathServerIntegrationTestB})
 * once the {@link AutoJsonRpcServiceExporter} is dropped.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:serverApplicationContext.xml")
@Deprecated
public class JsonRpcPathServerIntegrationTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void shouldCreateServiceExporter() {
		assertNotNull(applicationContext);

		{
			Object bean = applicationContext.getBean("/TestService");
			assertSame(JsonServiceExporter.class, bean.getClass());
		}

		{
			Object bean = applicationContext.getBean("/ServiceSansInterface");
			assertSame(JsonServiceExporter.class, bean.getClass());
		}

	}
}
