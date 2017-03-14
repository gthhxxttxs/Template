package com.tlong.gt.template.widget;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.SparseArray;
import android.view.View;

/**
 * 网格布局分割线
 * 只有内部纵横线，最外部没有
 * Created by 高腾 on 2017/3/13.
 */

public class DividerGridItemDecoration extends RecyclerView.ItemDecoration {

    private static final String TAG = DividerGridItemDecoration.class.getSimpleName();

    private Drawable mDivider;
    private int mDividerWidth;
    private int mDividerHeight;
    private Rect mTempRect = new Rect();
    private Rect mBounds = new Rect();
    private SparseArray<boolean[]> mItemDraw = new SparseArray<>();

    public DividerGridItemDecoration(int color, int dividerWidth, int dividerHeight) {
        mDivider = new ColorDrawable(color);
        mDividerWidth = dividerWidth;
        mDividerHeight = dividerHeight;
    }

    public void setDrawable(@NonNull Drawable drawable) {
        mDivider = drawable;
        mDividerWidth = drawable.getIntrinsicWidth();
        mDividerHeight = drawable.getIntrinsicHeight();
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        if (layoutManager == null) {
            throw new NullPointerException("layoutManager is null");
        }
        mTempRect.setEmpty();
        // item位置
        int itemPosition = ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewLayoutPosition();
        // item总数
        int itemCount = parent.getAdapter().getItemCount();
        if (layoutManager instanceof StaggeredGridLayoutManager) {
            staggeredGridOffsets((StaggeredGridLayoutManager) layoutManager, itemPosition, itemCount);
        } else if (layoutManager instanceof GridLayoutManager) {
            gridOffsets((GridLayoutManager) layoutManager, itemPosition, itemCount);
        } else if (layoutManager instanceof LinearLayoutManager) {
            linearOffsets((LinearLayoutManager) layoutManager, itemPosition);
        } else {
            mTempRect.setEmpty();
        }
        // 设置当前Item的偏移量,用于绘制Decorator
        outRect.set(mTempRect);
    }

    private void linearOffsets(LinearLayoutManager layoutManager, int itemPosition) {
        switch (layoutManager.getOrientation()) {
            case OrientationHelper.HORIZONTAL:
                show(itemPosition, false, false, true, false);
                break;
            case OrientationHelper.VERTICAL:
                show(itemPosition, false, false, false, true);
                break;
            default:
                show(itemPosition, false, false, false, false);
        }
    }

    private void gridOffsets(GridLayoutManager layoutManager, int itemPosition, int itemCount) {
        gridOutRect(layoutManager.getOrientation(), layoutManager.getSpanCount(), itemPosition, itemCount);
    }

    private void staggeredGridOffsets(StaggeredGridLayoutManager layoutManager, int itemPosition, int itemCount) {
        gridOutRect(layoutManager.getOrientation(), layoutManager.getSpanCount(), itemPosition, itemCount);
    }

    /** 网格布局显示哪边的线，默认四边的线都显示. */
    private void gridOutRect(int orientation, int spanCount, int itemPosition, int itemCount) {
        switch (orientation) {
            case OrientationHelper.HORIZONTAL:
                show(itemPosition, isStartSpanPositions(itemPosition, spanCount),
                        isSpanFirst(itemPosition, spanCount),
                        true,
                        true);
                break;
            case OrientationHelper.VERTICAL:
                show(itemPosition, isSpanFirst(itemPosition, spanCount),
                        isStartSpanPositions(itemPosition, spanCount),
                        true,
                        true);
                break;
            default:
                show(itemPosition, false, false, false, false);
        }
    }

    /** 显示哪个item哪边的线. */
    private void show(int position, boolean left, boolean top, boolean right, boolean bottom) {
        boolean[] shouldDraw = mItemDraw.get(position);
        if (shouldDraw == null) {
            shouldDraw = new boolean[4];
            mItemDraw.put(position, shouldDraw);
        }
        shouldDraw[0] = left;
        shouldDraw[1] = top;
        shouldDraw[2] = right;
        shouldDraw[3] = bottom;

        if (left) {
            mTempRect.left = mDividerWidth;
        } else {
            mTempRect.left = 0;
        }

        if (top) {
            mTempRect.top = mDividerHeight;
        } else {
            mTempRect.top = 0;
        }

        if (right) {
            mTempRect.right = mDividerWidth;
        } else {
            mTempRect.right = 0;
        }

        if (bottom) {
            mTempRect.bottom = mDividerHeight;
        } else {
            mTempRect.bottom = 0;
        }
    }

    // 暂时只支持Item宽高都相同的情况
    /** 以竖直方向为例，是否是最左侧. */
    private boolean isSpanFirst(int position, int spanCount) {
        return position % spanCount == 0;
    }
    /** 以竖直方向为例，是否是最右侧. */
    private boolean isSpanLast(int position, int spanCount) {
        return (position + 1) % spanCount == 0;
    }
    /** 以竖直方向为例，是否是最顶端. */
    private boolean isStartSpanPositions(int position, int spanCount) {
        return position < spanCount;
    }
    /** 以竖直方向为例，是否是最底端. */
    private boolean isEndSpanPositions(int position, int spanCount, int count) {
        return position >= count - (count % spanCount);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        // 绘制Decorator,超出偏移量的部分会被ItemView覆盖
        drawDivider(c, parent);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        // 绘制Decorator,超出偏移量的部分会覆盖ItemView
    }

    /** 画线. */
    private void drawDivider(Canvas c, RecyclerView parent) {
        c.save();
        clipParentPadding(c, parent);
        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            boolean[] shouldDraw = mItemDraw.get(i);
            if (shouldDraw == null) {
                continue;
            }
            final View child = parent.getChildAt(i);
            parent.getDecoratedBoundsWithMargins(child, mBounds);
            final int x = Math.round(ViewCompat.getTranslationX(child));
            final int y = Math.round(ViewCompat.getTranslationY(child));
            if (shouldDraw[0]) {
                drawLeft(c, x, y);
            }
            if (shouldDraw[1]) {
                drawTop(c, x, y);
            }
            if (shouldDraw[2]) {
                drawRight(c, x, y);
            }
            if (shouldDraw[3]) {
                drawBottom(c, x, y);
            }
        }
        c.restore();
    }

    /** 画板缩放到parent的padding值内. */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void clipParentPadding(Canvas c, RecyclerView parent) {
        if (parent.getClipToPadding()) {
            c.clipRect(parent.getPaddingLeft(),
                    parent.getPaddingTop(),
                    parent.getWidth() - parent.getPaddingRight(),
                    parent.getHeight() - parent.getPaddingBottom());
        }
    }

    /** 绘制左侧垂直线. */
    private void drawLeft(Canvas c, int x, int y) {
        final int left = mBounds.left + x;
        final int right = left + mDividerWidth;
        mDivider.setBounds(left, mBounds.top + y, right, mBounds.bottom + y);
        mDivider.draw(c);
    }

    /** 绘制顶部水平线. */
    private void drawTop(Canvas c, int x, int y) {
        final int top = mBounds.top + y;
        final int bottom = top + mDividerHeight;
        mDivider.setBounds(mBounds.left + x, top, mBounds.right + x, bottom);
        mDivider.draw(c);
    }

    /** 绘制右侧垂直线. */
    private void drawRight(Canvas c, int x, int y) {
        final int right = mBounds.right + x;
        final int left = right - mDividerWidth;
        mDivider.setBounds(left, mBounds.top + y, right, mBounds.bottom + y);
        mDivider.draw(c);
    }

    /** 绘制底部水平线. */
    private void drawBottom(Canvas c, int x, int y) {
        final int bottom = mBounds.bottom + y;
        final int top = bottom - mDividerHeight;
        mDivider.setBounds(mBounds.left + x, top, mBounds.right + x, bottom);
        mDivider.draw(c);
    }
}
