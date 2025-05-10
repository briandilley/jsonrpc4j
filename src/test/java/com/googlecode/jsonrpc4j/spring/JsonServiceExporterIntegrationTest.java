package com.googlecode.jsonrpc4j.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.googlecode.jsonrpc4j.spring.service.Service;
import com.googlecode.jsonrpc4j.spring.service.ServiceImpl;

import static org.junit.Assert.*;

/**
 * This test ensures that {@link com.googlecode.jsonrpc4j.spring.JsonServiceExporter} bean is
 * constructed according to Spring Framework configuration.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:serverApplicationContextC.xml")
public class JsonServiceExporterIntegrationTest {

	private static final String BEAN_NAME_AND_URL_PATH = "/UserService.json";

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void testExportedService() {
		assertNotNull(applicationContext);

		// check that the bean was only exported on the configured path.
		{
			Object bean = applicationContext.getBean(BEAN_NAME_AND_URL_PATH);
			assertEquals(JsonServiceExporter.class, bean.getClass());

			String[] names = applicationContext.getBeanNamesForType(JsonServiceExporter.class);
			assertNotNull(names);
			assertEquals(1, names.length);
			assertEquals(BEAN_NAME_AND_URL_PATH, names[0]);
		}

		// check that service classes were also successfully configured in the context.

		{
			Service service = applicationContext.getBean(Service.class);
			assertTrue(service instanceof ServiceImpl);

			ServiceImpl serviceImpl = applicationContext.getBean(ServiceImpl.class);
			assertNotNull(serviceImpl);
		}
	}
}
