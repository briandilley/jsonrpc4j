package com.googlecode.jsonrpc4j.spring;

import static junit.framework.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:serverApplicationContext.xml")
public class JsonRpcPathServerIntegrationTest {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  public void shouldCreateServiceExporter() {
    assertNotNull(applicationContext);
    Object bean = applicationContext.getBean("/Service");
    assertSame(JsonServiceExporter.class, bean.getClass());
  }
}
