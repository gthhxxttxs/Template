package com.tlong.gt.template.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tlong.gt.template.R;
import com.tlong.gt.template.module.camera.CameraPreviewActivity;
import com.tlong.gt.template.module.lock.LockPatternActivity;
import com.tlong.gt.template.module.lock.LockPatternSetupActivity;
import com.tlong.gt.template.util.LogUtil;
import com.tlong.gt.template.widget.DividerGridItemDecoration;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private RecyclerView mRecyclerView;
    private RecyclerAdapter mAdapter;

    private List<String> mDataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
//        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
//        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
//        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new RecyclerAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new DividerGridItemDecoration(Color.RED, 10, 10));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        initData();
    }

    private void initData() {
        for (int i = 0; i < 50; i++) {
            mDataList.add("hhad" + i);
        }
    }

    @Override
    public void onClick(View view) {
        int position = ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewLayoutPosition();
        LogUtil.e(tag, "position=" + position);
        switch (position) {
            case 0:
                startActivity(new Intent(mActivity, AccountListActivityBase.class));
                break;
            case 1:
                startActivity(new Intent(mActivity, LockPatternActivity.class));
                break;
            case 2:
                startActivity(new Intent(mActivity, LockPatternSetupActivity.class));
                break;
            case 3:
                startActivity(new Intent(mActivity, CameraPreviewActivity.class));
                break;
            case 4:
                startActivity(new Intent(mActivity, WebActivity.class));
                break;
            default:
        }
    }

    private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {

        @Override
        public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mActivity).inflate(R.layout.item_main, parent, false);
            return new RecyclerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerViewHolder holder, int position) {
            holder.itemView.setTag(position);
            holder.icon.setImageResource(R.mipmap.ic_launcher);
            holder.name.setText(mDataList.get(position));
        }

        @Override
        public int getItemCount() {
            return mDataList == null ? 0 : mDataList.size();
        }
    }

    class RecyclerViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView name;

        RecyclerViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            name = (TextView) itemView.findViewById(R.id.name);
            itemView.setOnClickListener(MainActivity.this);
        }
    }
}
