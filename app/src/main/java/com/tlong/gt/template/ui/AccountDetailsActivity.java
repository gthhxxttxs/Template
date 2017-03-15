package com.tlong.gt.template.ui;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.tlong.gt.template.R;
import com.tlong.gt.template.bean.Account;
import com.tlong.gt.template.databinding.ActivityAccountDetailsBinding;

public class AccountDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAccountDetailsBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_account_details);
        Account account = new Account();
        account.setLabel("QQ");
        account.setName("asdf");
        account.setPassword("123");
        binding.setAccount(account);
//        setContentView(R.layout.activity_account_details);
    }
}
