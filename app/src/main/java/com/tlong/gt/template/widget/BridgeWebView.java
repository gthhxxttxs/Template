package com.tlong.gt.template.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.tlong.gt.template.util.LogUtil;
import com.tlong.gt.template.util.ToastUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * Created by v_gaoteng on 2017/3/29.
 */

public class BridgeWebView extends WebView {

    private static final String TAG = BridgeWebView.class.getSimpleName();

    private static final String JS_BRIDGE_NAME = "bridge";

    public BridgeWebView(Context context) {
        this(context, null);
    }

    public BridgeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void init() {
        getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(new JsBridge(), JS_BRIDGE_NAME);
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
//        }
    }

    public void loadJs(String jsMethod) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            loadUrl("javascript:" + jsMethod + ";void(0);");
        } else {
            evaluateJavascript(jsMethod, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    ToastUtil.toast("onReceiveValue:" + value);
                }
            });
        }

    }

    private void dealJsBridgeParams(JSONObject json) {
        LogUtil.e(TAG, json.toString());
        ToastUtil.toast(json.toString());
    }

    private void dealJsBridgeParams(JSONArray json) {
        LogUtil.e(TAG, json.toString());
        ToastUtil.toast(json.toString());
    }


    private class JsBridge {
        @JavascriptInterface
        public void call(String data) {
            String trimData = data.trim();
            try {
                if (trimData.startsWith("{")) {
                    dealJsBridgeParams(new JSONObject(trimData));
                } else if (trimData.startsWith("[")) {
                    dealJsBridgeParams(new JSONArray(trimData));
                } else {
                    LogUtil.e(TAG, "data{%s} is not json", data);
                }
            } catch (JSONException e) {
                LogUtil.e(TAG, "data{%s} is not json", data);
                e.printStackTrace();
            }
        }
    }
}
