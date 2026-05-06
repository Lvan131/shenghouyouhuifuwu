package com.youhuifuwu.common.util;

import com.youhuifuwu.common.exception.BusinessException;

public final class AssertUtils {

    private AssertUtils() {
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(400, message);
        }
    }

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new BusinessException(400, message);
        }
    }
}

