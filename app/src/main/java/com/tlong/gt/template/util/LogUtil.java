package com.tlong.gt.template.util;

import android.util.Log;

/**
 * 打印Log工具
 * Created by GT on 2017/3/10.
 */

public class LogUtil {

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    /**
     * %s 字符串
     * $d 整数(十进制)
     * $f 浮点数
     * $b boolean
     * %t日期时间(%tF 2017-2-2, %tT 14:28:56)
     */
    public static void e(String tag, String format, Object... args) {
        e(tag, Util.formatString(format, args));
    }
}
