package com.tlong.gt.template.util;

import java.util.Locale;

/**
 * 工具类
 * Created by GT on 2017/3/10.
 */

public class Util {

    public static String formatString(String format, Object... args) {
        return String.format(Locale.CHINA, format, args);
    }
}
