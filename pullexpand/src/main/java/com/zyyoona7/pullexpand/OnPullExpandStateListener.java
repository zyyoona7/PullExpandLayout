package com.zyyoona7.pullexpand;

/**
 * 状态改变
 * 每种状态只在改变时回调一次不会重复调用
 */
public interface OnPullExpandStateListener {

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