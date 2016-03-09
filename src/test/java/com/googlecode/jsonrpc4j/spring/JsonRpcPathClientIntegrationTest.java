package com.googlecode.jsonrpc4j.spring;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import com.googlecode.jsonrpc4j.spring.service.Service;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:clientApplicationContext.xml")
public class JsonRpcPathClientIntegrationTest {

	@Autowired
	private Service service;

	@Test
	public void shouldCreateServiceExporter() {
		assertNotNull(service);
		assertTrue(AopUtils.isAopProxy(service));
	}

	@Test
	public void callToObjectMethodsShouldBeHandledLocally() {
		if (service != null) {
			assertNotNull(service.toString());
			// noinspection ResultOfMethodCallIgnored
			service.hashCode();
			// noinspection EqualsWithItself
			assertTrue(service.equals(service));
		}
	}
}
