package com.tlong.gt.template.util;

import android.widget.Toast;

import com.tlong.gt.template.App;

/**
 * Toast工具类
 * Created by v_gaoteng on 2017/3/16.
 */

public class ToastUtil {

    private static Toast sToast;

    public static void toast(String text) {
        if (sToast == null) {
            sToast = Toast.makeText(App.getContext(), text, Toast.LENGTH_SHORT);
        } else {
            sToast.setText(text);
        }
        sToast.show();
    }
}
