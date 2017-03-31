package com.tlong.gt.template.ui;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.ValueCallback;

import com.tlong.gt.template.R;
import com.tlong.gt.template.util.ToastUtil;
import com.tlong.gt.template.widget.BridgeWebView;

public class WebActivity extends AppCompatActivity {

    private BridgeWebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        mWebView = (BridgeWebView) findViewById(R.id.web_view);
        mWebView.loadUrl("file:///android_asset/jsBridgeTest.html");
        mWebView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    mWebView.loadJs("toast('bbb')");
                } else {
                    mWebView.evaluateJavascript("toast('bbb')", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            ToastUtil.toast("onReceiveValue:" + value);
                        }
                    });
                }
                return true;
            }
        });


    }

    @Override
    protected void onDestroy() {
        if (mWebView != null) {
            ViewParent parent = mWebView.getParent();
            if(parent != null && parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(mWebView);
            }
            mWebView.removeAllViews();
            mWebView.destroy();
            mWebView =null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.canGoBack()) {
            mWebView.goBack();
            return;
        }
        super.onBackPressed();
    }
}
