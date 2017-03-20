package com.tlong.gt.template.module.lock;

import android.os.Bundle;

import com.tlong.gt.template.R;
import com.tlong.gt.template.ui.BaseActivity;
import com.tlong.gt.template.util.LogUtil;

import java.util.List;

public class LockPatternActivity extends BaseActivity implements LockPatternView.OnPatternListener {

    private LockPatternView mLockPatternView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_pattern);
        mLockPatternView = (LockPatternView) findViewById(R.id.lock_pattern_view);
        mLockPatternView.setOnPatternListener(this);
    }

    @Override
    public void onPatternStart() {
        LogUtil.e(tag, "onPatternStart");
    }

    @Override
    public void onPatternCleared() {
        LogUtil.e(tag, "onPatternCleared");
    }

    @Override
    public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {
        LogUtil.e(tag, "onPatternCellAdded:" + pattern.size());
    }

    @Override
    public void onPatternDetected(List<LockPatternView.Cell> pattern) {
        LogUtil.e(tag, "onPatternDetected:" + pattern.size());
    }
}
