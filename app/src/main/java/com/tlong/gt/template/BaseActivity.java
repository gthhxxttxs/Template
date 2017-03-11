package com.tlong.gt.template;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Activity基类
 * Created by GT on 2017/3/11.
 */

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.addActivity(this);
    }

    @Override
    protected void onDestroy() {
        App.removeActivity(this);
        super.onDestroy();
    }
}
