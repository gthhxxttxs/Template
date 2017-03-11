package com.tlong.gt.template.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.tlong.gt.template.App;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 权限工具
 * PermissionUtil.i(activity)
 *         .permission(Manifest.permission.CAMERA)
 *         .request(new PermissionUtil.Callback<PermissionUtil.Permission>() {
 *             public void call(PermissionUtil.Permission permission) {
 *                 permission.isGranted();
 *             }
 *         });
 *
 * Created by 高腾 on 2017/2/22.
 */

public class PermissionUtil {

    private static final String TAG = PermissionUtil.class.getSimpleName();

    private static final int REQUEST_CODE = 11;

    public static class Permission {
        private String name;
        private boolean isGranted;
        private boolean isShouldRationale;

        Permission(String name, boolean isGranted, boolean isShouldRationale) {
            this.name = name;
            this.isGranted = isGranted;
            this.isShouldRationale = isShouldRationale;
        }

        public String getName() {
            return name;
        }

        public boolean isGranted() {
            return isGranted;
        }

        public boolean isShouldRationale() {
            return isShouldRationale;
        }

        @Override
        public String toString() {
            return "Permission{" +
                    "name='" + name + '\'' +
                    ", isGranted=" + isGranted +
                    ", isShouldRationale=" + isShouldRationale +
                    '}';
        }
    }

    public interface Callback<T> {
        void call(T t);
    }

    /**
     * 检测权限
     * @param context 上下文
     * @param permission 权限
     * @return 权限是否被允许
     */
    public static boolean checkPermission(@NonNull Context context, @NonNull String permission) {
        return check(ContextCompat.checkSelfPermission(context, permission));
    }

    private static boolean check(int result) {
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private static PermissionUtil i;
    /** activity的弱引用. */
    private static WeakReference<Activity> sActivity;
    /** 所有请求权限和其对应回调集合的映射集合. */
    private static Map<String, Set<Callback<Permission>>> sMap = new HashMap<>();
    /** 所有待请求的权限集合. */
    private static Set<String> allPermissions = new HashSet<>();
    /** 是否可以直接请求. */
    private static boolean isPrepare = true;
    /** 单次请求权限集合. */
    private Set<String> params;

    private PermissionUtil() {}

    public static PermissionUtil i(@NonNull Activity activity) {
        init().newRequest();
        if (sActivity != null && activity.equals(sActivity.get())) {
            return i;
        }
        sActivity = new WeakReference<>(activity);
        return i;
    }

    private static PermissionUtil init() {
        if (i == null) {
            synchronized (PermissionUtil.class) {
                if (i == null) {
                    i = new PermissionUtil();
                }
            }
        }
        return i;
    }

    private void newRequest() {
        // 每次请求用新的参数容器，方便做最后的处理
        params = new HashSet<>();
    }

    public PermissionUtil permission(@NonNull String permission) {
        params.add(permission);
        return this;
    }

    public PermissionUtil permissions(@NonNull String... permissions) {
        Collections.addAll(params, permissions);
        return this;
    }

    public PermissionUtil permissions(@NonNull Collection<String> permissions) {
        params.addAll(permissions);
        return this;
    }

    /**
     * 请求权限
     * @param callback 每个权限会回调一次
     */
    public void request(@NonNull Callback<Permission> callback) {
        for (String permission : params) {
            Set<Callback<Permission>> callbacks = sMap.get(permission);
            if (callbacks == null) {
                callbacks = new HashSet<>();
                sMap.put(permission, callbacks);
            }
            callbacks.add(callback);
        }
        allPermissions.addAll(params);
        dispatcher();
        params = null;
    }

    /**
     * 分发请求的权限
     */
    private void dispatcher() {
        Activity activity = sActivity.get();
        if (App.checkActivityDestroyed(activity)) {
            over();
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Android6.0以下直接返回check结果
            for (String permission : params) {
                boolean isGranted = checkPermission(activity, permission);
                call(permission, isGranted, !isGranted);
            }
            over();
        } else {
            List<String> list = new ArrayList<>();
            for (String permission : params) {
                if (checkPermission(activity, permission)) {
                    call(permission, true, false);
                    continue;
                }
                list.add(permission);
            }
            // 若可以则直接请求未允许的权限
            if (!list.isEmpty() && isPrepare) {
                isPrepare = false;
                activity.requestPermissions(list.toArray(new String[list.size()]), REQUEST_CODE);
            }
        }
    }

    /**
     * 请求权限是否全部允许
     * @param callback 回调
     */
    public void requestAllGranted(@NonNull final Callback<Boolean> callback) {
        final Set<String> temp = this.params;
        request(new Callback<Permission>() {
            boolean isOver = false;
            int i = 0;
            @Override
            public void call(Permission permission) {
                // 如果结束或者返回的权限不是这次请求里的不做处理
                if (isOver || !temp.contains(permission.getName())) {
                    return;
                }
                i++; // 记录处理次数
                // 如果返回的权限被拒，则直接返回被拒结果，不再继续处理
                if (!permission.isGranted) {
                    callback.call(false);
                    isOver = true;
                    return;
                }
                // 如果最后一次都没被拒，则说明用户全部允许，返回允许结果
                if (i == temp.size()) {
                    callback.call(true);
                }
            }
        });
    }

    /**
     * 请求权限是否有任何一个被允许
     * @param callback 回调
     */
    public void requestAnyoneGranted(@NonNull final Callback<Boolean> callback) {
        final Set<String> temp = this.params;
        request(new Callback<Permission>() {
            boolean isOver = false;
            int i = 0;
            @Override
            public void call(Permission permission) {
                // 如果结束或者返回的权限不是这次请求里的不做处理
                if (isOver || !temp.contains(permission.getName())) {
                    return;
                }
                i++; // 记录处理次数
                // 如果返回的权限被允许，则直接返回允许结果，不再继续处理
                if (permission.isGranted) {
                    callback.call(true);
                    isOver = true;
                    return;
                }
                // 如果最后一次都没被允许，则说明用户全部拒绝，返回拒绝结果
                if (i == temp.size()) {
                    callback.call(false);
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static void onRequestPermissionsResult(@NonNull Activity activity,
                                                  int requestCode,
                                                  @NonNull String[] permissions,
                                                  @NonNull int[] grantResults) {
        if (requestCode != REQUEST_CODE) return;
        Activity act = sActivity.get();
        if (act == null || act.isDestroyed() || !act.equals(activity)) {
            over();
            return;
        }
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            call(permission, check(grantResults[i]), activity.shouldShowRequestPermissionRationale(permission));
        }
        if (allPermissions.isEmpty()) {
            over();
            return;
        }
        // 继续请求未授权的权限
        activity.requestPermissions(allPermissions.toArray(new String[allPermissions.size()]), REQUEST_CODE);
    }

    /**
     * 返回权限请求结果
     * @param permission 权限
     * @param isGranted 是否授权
     * @param isShouldRationale 是否需要详细说明此权限的用途
     */
    private static void call(@NonNull String permission, boolean isGranted, boolean isShouldRationale) {
        call(sMap.remove(permission), new Permission(permission, isGranted, isShouldRationale));
        // 返回结果的权限在列表中删除
        allPermissions.remove(permission);
    }

    private static  <T> void call(@NonNull Set<Callback<T>> callbacks, @NonNull T t) {
        for (Callback<T> callback : callbacks) {
            callback.call(t);
        }
    }

    /**
     * 请求结束重置全局参数
     */
    private static void over() {
        sActivity = null;
        sMap.clear();
        allPermissions.clear();
        isPrepare = true;
    }
}
