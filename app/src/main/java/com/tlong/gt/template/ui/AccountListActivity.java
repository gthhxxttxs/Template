package com.tlong.gt.template.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.tlong.gt.template.BR;
import com.tlong.gt.template.R;
import com.tlong.gt.template.adapter.DataBingingAdapter;
import com.tlong.gt.template.bean.Account;
import com.tlong.gt.template.util.LogUtil;
import com.tlong.gt.template.widget.DividerGridItemDecoration;

import java.util.ArrayList;
import java.util.List;

public class AccountListActivity extends BaseActivity implements DataBingingAdapter.OnItemClickListener {

    private RecyclerView mAccountListView;
    private DataBingingAdapter mAdapter;
    private List<Account> mAccountList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_list);
        mAccountListView = (RecyclerView) findViewById(R.id.account_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mActivity);
        mAccountListView.setLayoutManager(layoutManager);
        mAdapter = new DataBingingAdapter(mActivity, R.layout.item_account_list, BR.account);
        mAccountListView.setAdapter(mAdapter);
        mAccountListView.addItemDecoration(new DividerGridItemDecoration(Color.BLUE, 10, 10));
        mAccountListView.setItemAnimator(new DefaultItemAnimator());

        mAdapter.setOnItemClickListener(this);

        initData();
    }

    private void initData() {
        mAccountList = new ArrayList<>();
        mAdapter.setDataList(mAccountList);
        for (int i = 0; i < 20; i++) {
            Account account = new Account();
            account.setLabel("QQ" + i);
            account.setName("qaz" + i);
            account.setPassword("123qwe" + i);
            mAccountList.add(account);
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        LogUtil.e(tag, "position=" + position);
        startActivity(new Intent(mActivity, AccountDetailsActivity.class));
    }
}
