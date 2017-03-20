package com.tlong.gt.template.ui;

import android.view.Menu;
import android.view.MenuItem;

import java.util.HashSet;
import java.util.Set;

/**
 * 使用Menu的Activity
 * Created by v_gaoteng on 2017/3/16.
 */

public abstract class BaseMenuActivity extends BaseActivity {

    private Set<MenuItem> mCanCollapseActionViewMenuItems;

    @Override
    protected void onDestroy() {
        if (mCanCollapseActionViewMenuItems != null) {
            mCanCollapseActionViewMenuItems.clear();
            mCanCollapseActionViewMenuItems = null;
        }
        super.onDestroy();
    }

    /** 注册能够展开的MenuItem. */
    protected void registerCanExpandActionViewMenuItem(MenuItem item) {
        if (mCanCollapseActionViewMenuItems == null) {
            mCanCollapseActionViewMenuItems = new HashSet<>();
        }
        mCanCollapseActionViewMenuItems.add(item);
    }

    /** 折叠所有注册的MenuItem. */
    protected void collapseAllActionView() {
        if (mCanCollapseActionViewMenuItems == null || mCanCollapseActionViewMenuItems.isEmpty()) {
            return;
        }
        for (MenuItem item : mCanCollapseActionViewMenuItems) {
            if (item.isActionViewExpanded()) {
                item.collapseActionView();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(getMenuResource(), menu);
        initMenu(menu);
        return true;
    }

    protected abstract int getMenuResource();

    /** 配置Menu及MenuItem. */
    protected void initMenu(Menu menu) {
        // 子类实现
    }

}
