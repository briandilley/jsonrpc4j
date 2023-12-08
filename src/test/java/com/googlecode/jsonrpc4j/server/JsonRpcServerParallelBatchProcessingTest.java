package com.googlecode.jsonrpc4j.server;

import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.util.Util;
import org.easymock.EasyMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@ExtendWith(EasyMockExtension.class)
public class JsonRpcServerParallelBatchProcessingTest extends JsonRpcServerBatchTest {

    @BeforeEach
    public void setup() {
        jsonRpcServer = new JsonRpcServer(Util.mapper, mockService, JsonRpcServerTest.ServiceInterface.class);
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5,
                50,
                1000,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(50));
        jsonRpcServer.setBatchExecutorService(threadPoolExecutor);
    }
}
