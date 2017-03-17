package com.tlong.gt.template.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.tlong.gt.template.App;
import com.tlong.gt.template.R;
import com.tlong.gt.template.bean.Account;
import com.tlong.gt.template.databinding.ActivityAccountDetailsBinding;

public class AccountDetailsActivity extends AppCompatActivity {

    public static final String ID = "id";

    public static void start(@NonNull Context context, int accountId) {
        Intent intent = new Intent(context, AccountDetailsActivity.class);
        if (!(context instanceof Activity)) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.putExtra(ID, accountId);
        context.startActivity(intent);
    }

    private Account mAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAccountDetailsBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_account_details);
        int id = getIntent().getIntExtra(ID, -1);
        mAccount = App.getDao().queryById(id, Account.class);
        if (mAccount == null) {
            mAccount = new Account();
        }
        binding.setAccount(mAccount);
    }

    public void submit(View view) {
        App.getDao().save(mAccount);
        finish();
    }
}
