package com.tlong.gt.template.adapter;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tlong.gt.template.BR;
import com.tlong.gt.template.util.Util;

import java.util.List;

/**
 * 使用DataBinging框架的通用RecyclerView.Adapter
 * Created by v_gaoteng on 2017/3/15.
 */

public class DataBingingAdapter extends RecyclerView.Adapter<DataBingingAdapter.DataBingingViewHolder>
        implements View.OnClickListener, View.OnLongClickListener {

    private static final int DEFAULT_VIEW_TYPE = 0;
    private static final int DEFAULT_INT = -1;

    private Context mContext;
    private SparseIntArray mTypeLayoutArray;
    private SparseIntArray mTypeVariableIdArray;

    private List mDataList;

    private OnItemClickListener mItemClickListener;
    private OnItemLongClickListener mItemLongClickListener;

    public DataBingingAdapter(Context context) {
        mContext = context;
        mTypeLayoutArray = new SparseIntArray();
        mTypeVariableIdArray = new SparseIntArray();
    }

    public DataBingingAdapter(Context context, int layoutRes, int variableId) {
        this(context);
        addType(DEFAULT_VIEW_TYPE, layoutRes, variableId);
    }

    public void setDataList(List dataList) {
        mDataList = dataList;
    }

    /**
     * 覆写{@link #getItemViewType(int)} 设置position对应的viewType
     * 添加每种type对应的layout资源和layout中data的id
     * @param viewType 布局类型
     * @param layoutRes 布局资源
     * @param variableId 布局变量id, 调用{@link BR}获取
     */
    public void addType(int viewType, int layoutRes, int variableId) {
        mTypeLayoutArray.append(viewType, layoutRes);
        mTypeVariableIdArray.append(viewType, variableId);
    }

    private int getLayoutRes(int viewType) {
        int layoutRes = mTypeLayoutArray.get(viewType, DEFAULT_INT);
        checkInt(layoutRes, "viewType{%d}对应的layoutResource不存在", viewType);
        return layoutRes;
    }

    private int getVariableId(int viewType) {
        int variableId = mTypeVariableIdArray.get(viewType, DEFAULT_INT);
        checkInt(variableId, "viewType{%d}对应的variableId不存在", viewType);
        return variableId;
    }

    @Override
    public int getItemViewType(int position) {
        return DEFAULT_VIEW_TYPE;
    }

    private int checkInt(int i, String s, Object... args) {
        if (i == DEFAULT_INT) {
            throw new IllegalStateException(Util.formatString(s, args));
        }
        return i;
    }

    @Override
    public DataBingingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DataBingingViewHolder(createView(parent, getLayoutRes(viewType)), viewType);
    }

    private View createView(ViewGroup parent, int layoutResource) {
        return LayoutInflater.from(mContext).inflate(layoutResource, parent, false);
    }

    @Override
    public void onBindViewHolder(DataBingingViewHolder holder, int position) {
        holder.binding.setVariable(holder.variableId, mDataList.get(position));
        holder.binding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return mDataList == null ? 0 : mDataList.size();
    }

    class DataBingingViewHolder extends RecyclerView.ViewHolder {

        ViewDataBinding binding;
        int variableId;
        DataBingingViewHolder(View itemView, int viewType) {
            super(itemView);
            this.binding = DataBindingUtil.bind(itemView);
            this.variableId = getVariableId(viewType);
            itemView.setOnClickListener(DataBingingAdapter.this);
            itemView.setOnLongClickListener(DataBingingAdapter.this);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(View view, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        mItemLongClickListener = listener;
    }

    @Override
    public void onClick(View v) {
        if (mItemClickListener != null) {
            mItemClickListener.onItemClick(v,
                    ((RecyclerView.LayoutParams) v.getLayoutParams()).getViewLayoutPosition());
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return mItemLongClickListener != null
                && mItemLongClickListener.onItemLongClick(v,
                ((RecyclerView.LayoutParams) v.getLayoutParams()).getViewLayoutPosition());
    }
}
