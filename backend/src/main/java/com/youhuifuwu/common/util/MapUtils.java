package com.youhuifuwu.common.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MapUtils {

    private MapUtils() {
    }

    public static Map<String, Object> of(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }
}
