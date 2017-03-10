package com.tlong.gt.template;

import android.app.Application;

import com.tlong.gt.template.util.LogUtil;

/**
 * app init
 * Created by GT on 2017/3/10.
 */

public class App extends Application {

    private static final String TAG = App.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (t == null || e == null) {
                    LogUtil.e(TAG, "thread=" + t + ", throwable=" + e);
                } else {
                    LogUtil.e(TAG, "threadName=%s, errorMsg=%s", t.getName(), e.getMessage());
                }
//                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
    }
}
