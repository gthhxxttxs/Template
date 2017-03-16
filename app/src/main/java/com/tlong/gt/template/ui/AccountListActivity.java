package com.tlong.gt.template.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.litesuits.orm.db.assit.QueryBuilder;
import com.tlong.gt.template.App;
import com.tlong.gt.template.BR;
import com.tlong.gt.template.R;
import com.tlong.gt.template.adapter.DataBingingAdapter;
import com.tlong.gt.template.bean.Account;
import com.tlong.gt.template.util.LogUtil;
import com.tlong.gt.template.util.ToastUtil;
import com.tlong.gt.template.widget.DividerGridItemDecoration;

import java.util.ArrayList;
import java.util.List;

public class AccountListActivity extends MenuActivity implements DataBingingAdapter.OnItemClickListener {

    private RecyclerView mAccountListView;
    private DataBingingAdapter mAdapter;
    private List<Account> mShowDataList;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initToolbar(toolbar);

        mAccountListView = (RecyclerView) findViewById(R.id.account_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mActivity);
        mAccountListView.setLayoutManager(layoutManager);
        mAdapter = new DataBingingAdapter(mActivity, R.layout.item_account_list, BR.account);
        mShowDataList = new ArrayList<>();
        mAdapter.setDataList(mShowDataList);
        mAccountListView.setAdapter(mAdapter);
        mAccountListView.addItemDecoration(new DividerGridItemDecoration(Color.BLUE, 10, 10));
        mAccountListView.setItemAnimator(new DefaultItemAnimator());

        mAdapter.setOnItemClickListener(this);

        initData();
    }

    private void initToolbar(Toolbar toolbar) {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToastUtil.toast("back");
                onBackPressed();
            }
        });
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String msg;
                switch (item.getItemId()) {
                    case R.id.action_add:
                        msg = "add";
                        break;
                    case R.id.action_search:
                        msg = "search";
                        break;
                    case R.id.action_settings:
                        msg = "settings";
                        break;
                    default:
                        msg = "未知item";
                }
                ToastUtil.toast(msg);
                return true;
            }
        });
    }

    private void initData() {
        for (int i = 0; i < 30; i++) {
            App.getDao().insert(new Account("QQ" + i, "qwe" + (i * 2), "123" + (i * 3)));
        }
        updateAllData(App.getDao().query(Account.class));
    }

    @Override
    protected int getMenuResource() {
        return R.menu.menu_account_list;
    }

    @Override
    protected void initMenu(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.action_search);
        // 注册搜索框，方便之后折叠
        registerCanExpandActionViewMenuItem(searchItem);

        initSearchView((SearchView) searchItem.getActionView());
    }

    private void initSearchView(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // return true 不再发action，启动处理的Activity
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                LogUtil.e(tag, "searchText:" + newText);
                ArrayList<Account> list = App.getDao().query(new QueryBuilder<>(Account.class)
                        .where("label LIKE ?", "%" + newText + "%")
                        .whereAppendOr()
                        .whereAppend("name LIKE ?", "%" + newText + "%")
                        .whereAppendOr()
                        .whereAppend("password LIKE ?", "%" + newText + "%"));
                updateAllData(list);
                return true;
            }
        });
    }

    private void updateAllData(List<Account> dataList) {
        mShowDataList.clear();
        mShowDataList.addAll(dataList);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(View view, int position) {
        LogUtil.e(tag, "position=" + position);
        collapseAllActionView();
        startActivity(new Intent(mActivity, AccountDetailsActivity.class));
    }
}
