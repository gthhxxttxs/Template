package com.tlong.gt.template;

import android.app.Activity;
import android.app.Application;
import android.app.ListActivity;
import android.content.Context;
import android.os.Process;

import com.tlong.gt.template.common.CrashHandler;
import com.tlong.gt.template.util.LogUtil;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

/**
 * app init
 * Created by GT on 2017/3/10.
 */

public class App extends Application {

    private static final String TAG = App.class.getSimpleName();

    private static WeakReference<Context> sContext;

    private static List<Activity> mActivityList;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = new WeakReference<>(getApplicationContext());
        mActivityList = new LinkedList<>();
        CrashHandler.start();
    }

    /**
     * @return ApplicationContext
     */
    public static Context getContext() {
        return sContext.get();
    }

    public static void addActivity(Activity activity) {
        mActivityList.add(activity);
    }

    public static void removeActivity(Activity activity) {
        mActivityList.remove(activity);
    }

    /**
     * 正常退出应用
     */
    public static void exit() {
        LogUtil.e(TAG, "app exit");
        finishAllActivity();
        System.exit(0);
    }

    /**
     * 杀死当前应用
     */
    public static void kill() {
        finishAllActivity();
        Process.killProcess(Process.myPid());
        System.exit(1);
    }

    private static void finishAllActivity() {
        for (Activity activity : mActivityList) {
            activity.finish();
        }
    }
}
