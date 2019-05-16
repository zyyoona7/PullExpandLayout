package com.zyyoona7.pullexpandx.listener;

import com.zyyoona7.pullexpandx.PullExpandLayout;

public interface OnPullExpandChangedListener {

    /**
     * Header 移动的时候回调
     *
     * @param orientation     布局方向 垂直或者水平
     * @param percent         关闭到展开的百分比
     * @param offset          拖动距离
     * @param heightOrWidth   垂直方向为 Header 的高度，水平方向为 Header 的宽度
     * @param maxDragDistance 最大拖拽距离
     */
    void onHeaderMoving(int orientation, float percent,
                        int offset, int heightOrWidth, int maxDragDistance);

    /**
     * Footer 移动的时候回调
     *
     * @param orientation     布局方向 垂直或者水平
     * @param percent         关闭到展开的百分比
     * @param offset          拖动距离
     * @param heightOrWidth   垂直方向为 Footer 的高度，水平方向为 Footer 的宽度
     * @param maxDragDistance 最大拖拽距离
     */
    void onFooterMoving(int orientation, float percent,
                        int offset, int heightOrWidth, int maxDragDistance);

    /**
     * Header 状态变化
     *
     * @param layout PullExpandLayout
     * @param state  state
     */
    void onHeaderStateChanged(PullExpandLayout layout, int state);

    /**
     * Footer 状态变化
     *
     * @param layout PullExpandLayout
     * @param state  state
     */
    void onFooterStateChanged(PullExpandLayout layout, int state);
}
