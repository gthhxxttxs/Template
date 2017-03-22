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

    /**
     * 数字过滤
     * @param num 源数字
     * @param min 最小值
     * @param max 最大值
     * @return 介于最小值和最大值之间的数字
     */
    public static int clamp(int num, int min, int max) {
        if (num > max) {
            return max;
        }
        if (num < min) {
            return min;
        }
        return num;
    }
}
