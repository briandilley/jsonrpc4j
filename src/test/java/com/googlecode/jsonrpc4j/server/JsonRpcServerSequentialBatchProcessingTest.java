package com.googlecode.jsonrpc4j.server;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.util.Util;
import org.easymock.EasyMockRunner;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class JsonRpcServerSequentialBatchProcessingTest extends JsonRpcServerBatchTest {

    @Before
    public void setup() {
        jsonRpcServer = new JsonRpcServer(Util.mapper, mockService, JsonRpcServerTest.ServiceInterface.class);
    }
}
