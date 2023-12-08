package com.googlecode.jsonrpc4j.spring;

import com.googlecode.jsonrpc4j.spring.serviceb.NoopTemperatureImpl;
import com.googlecode.jsonrpc4j.spring.serviceb.Temperature;
import com.googlecode.jsonrpc4j.spring.serviceb.TemperatureImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>This test uses the new class
 * {@link AutoJsonRpcServiceImplExporter} which is designed to vend specific annotated
 * implementations.</p>
 */

@Disabled
@SpringBootTest
@ContextConfiguration("classpath:serverApplicationContextB.xml")
public class JsonRpcPathServerIntegrationTestB {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void testExportedService() {
		assertNotNull(applicationContext);

		// check that the service was vended on both paths that were configured.

		{
			Object bean = applicationContext.getBean("/api/temperature");
			assertSame(JsonServiceExporter.class, bean.getClass());
		}

		{
			Object bean = applicationContext.getBean("/api-web/temperature");
			assertSame(JsonServiceExporter.class, bean.getClass());
		}

		// check that the bean was only exported on the two paths provided.

		{
			String[] names = applicationContext.getBeanNamesForType(JsonServiceExporter.class);
			Arrays.sort(names);
			assertSame(2, names.length);
			assertEquals("/api-web/temperature", names[0]);
			assertEquals("/api/temperature", names[1]);
		}

		// check that the no-op was also successfully configured in the context.

		{
			Map<String, ? extends Temperature> beans = applicationContext.getBeansOfType(Temperature.class);
			assertSame(2, beans.size());
			Set<Class<? extends Temperature>> beanClasses = new HashSet<>();

			for (Temperature temperature : beans.values()) {
				beanClasses.add(temperature.getClass());
			}

			assertTrue(beanClasses.contains(NoopTemperatureImpl.class));
			assertTrue(beanClasses.contains(TemperatureImpl.class));
		}
	}

}
