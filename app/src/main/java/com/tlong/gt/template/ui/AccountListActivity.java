package com.tlong.gt.template.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tlong.gt.template.R;
import com.tlong.gt.template.bean.Account;
import com.tlong.gt.template.widget.DividerGridItemDecoration;

import java.util.ArrayList;
import java.util.List;

public class AccountListActivity extends BaseActivity {

    private RecyclerView mAccountListView;
    private AccountListAdapter mAdapter;
    private List<Account> mAccountList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_list);
        mAccountListView = (RecyclerView) findViewById(R.id.account_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mActivity);
        mAccountListView.setLayoutManager(layoutManager);
        mAdapter = new AccountListAdapter();
        mAccountListView.setAdapter(mAdapter);
        mAccountListView.addItemDecoration(new DividerGridItemDecoration(Color.BLUE, 10, 10));
        mAccountListView.setItemAnimator(new DefaultItemAnimator());
        mAccountList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Account account = new Account();
            account.setLabel("QQ" + i);
            account.setName("qaz" + i);
            account.setPassword("123qwe" + i);
            mAccountList.add(account);
        }
    }

    private class AccountListAdapter extends RecyclerView.Adapter<VH> {

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(mActivity).inflate(R.layout.item_account_list, parent, false));
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            Account account = mAccountList.get(position);
            holder.label.setText(account.getLabel());
            holder.name.setText(account.getName());
            holder.password.setText(account.getPassword());
        }

        @Override
        public int getItemCount() {
            return mAccountList == null ? 0 : mAccountList.size();
        }
    }
    class VH extends RecyclerView.ViewHolder {

        private TextView label;
        private TextView name;
        private TextView password;

        VH(View itemView) {
            super(itemView);
            label = (TextView) itemView.findViewById(R.id.label);
            name = (TextView) itemView.findViewById(R.id.name);
            password = (TextView) itemView.findViewById(R.id.password);
        }
    }
}
