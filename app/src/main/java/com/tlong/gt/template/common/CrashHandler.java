package com.tlong.gt.template.common;

import com.tlong.gt.template.App;
import com.tlong.gt.template.util.LogUtil;

/**
 * 全局异常处理器
 * Created by GT on 2017/3/11.
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = CrashHandler.class.getSimpleName();

    /** 系统默认UncaughtException处理器. */
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private CrashHandler() {}

    public static void start() {
        new CrashHandler().init();
    }

    private void init() {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
   public void uncaughtException(Thread t, Throwable e) {
        if (!handlerException(t, e)) {
            if (mDefaultHandler == null) {
                App.exit();
            } else {
                mDefaultHandler.uncaughtException(t, e);
            }
        }
    }

    private boolean handlerException(Thread t, Throwable e) {
        if (t == null || e == null) return false;
        LogUtil.e(TAG, "ThreadName = %s\nErrorMsg = $s", t.getName(), e.getMessage());
        saveCrash(t, e);
        return true;
    }

    private void saveCrash(Thread t, Throwable e) {
        e.printStackTrace();
    }
}
