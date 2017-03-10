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
    public static void e(String tag, String format, Object... args) {
        e(tag, Util.formatString(format, args));
    }
}
