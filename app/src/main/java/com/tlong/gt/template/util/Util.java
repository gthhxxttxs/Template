package com.tlong.gt.template.util;

import java.util.Locale;

/**
 * 工具类
 * Created by GT on 2017/3/10.
 */

public class Util {

    /**
     * %s 字符串
     * %c 字符
     * %b boolean
     * %d 整数(十进制)
     * %x 整数(十六进制)
     * %o 整数(八进制)
     * %f 浮点数
     * %h 散列码 'A' -> 41
     * %t 时间 %tF -> 年-月-日; %tT -> 时:分:秒(24时制); %tr -> HH:MM:SS PM(12时制)
     */
    public static String formatString(String format, Object... args) {
        return String.format(Locale.CHINA, format, args);
    }
}
