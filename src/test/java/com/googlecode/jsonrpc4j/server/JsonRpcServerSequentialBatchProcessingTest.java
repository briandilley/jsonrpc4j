package com.googlecode.jsonrpc4j.server;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.util.Util;
import org.easymock.EasyMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EasyMockExtension.class)
public class JsonRpcServerSequentialBatchProcessingTest extends JsonRpcServerBatchTest {

    @BeforeEach
    public void setup() {
        jsonRpcServer = new JsonRpcServer(Util.mapper, mockService, JsonRpcServerTest.ServiceInterface.class);
    }
}
