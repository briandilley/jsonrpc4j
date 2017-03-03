package com.googlecode.jsonrpc4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 20.04.2016
 * KostaPC
*/

public class VarArgsUtil {

    public static Map<String,Object> convertArgs(Object[] params) {

        final Map<String,Object> unsafeMap = new HashMap<>();
        for (int i = 0; i < params.length; i += 2) {
            if (params[i] instanceof String && params[i] != null && !params[i].toString().isEmpty()) {
                unsafeMap.put(params[i].toString(), params[i + 1]);
            }
        }
        return unsafeMap;

    }
}
