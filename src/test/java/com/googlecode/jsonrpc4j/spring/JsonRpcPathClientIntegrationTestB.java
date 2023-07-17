package com.googlecode.jsonrpc4j.spring;

import com.googlecode.jsonrpc4j.spring.serviceb.Temperature;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:clientApplicationContextB.xml")
public class JsonRpcPathClientIntegrationTestB {

    @JsonRpcReference(address = "http://localhost:8080")
    private Temperature temperature;

    public Integer demo() {
        return temperature.currentTemperature();
    }


    @Test
    public void shouldCreateServiceExporter() {
        assertNotNull(temperature);
        assertTrue(AopUtils.isAopProxy(temperature));
    }

    @Test
    public void callToObjectMethodsShouldBeHandledLocally() {
        if (temperature != null) {
            assertNotNull(temperature.toString());
            // noinspection ResultOfMethodCallIgnored
            temperature.hashCode();
            // noinspection EqualsWithItself
            assertTrue(temperature.equals(temperature));
        }
    }
}
