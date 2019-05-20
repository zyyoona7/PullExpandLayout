package com.zyyoona7.pullexpand.transformer;

import android.support.annotation.NonNull;
import android.view.View;

import com.zyyoona7.pullexpand.PullExpandLayout;

/**
 * 变换器，用来处理不同的 dragType 下 header、footer、content 的偏移
 */
public interface PullExpandTransformer {

    /**
     * 变换 header view
     *
     * @param layout        PullExpandLayout
     * @param orientation   布局方向 VERTICAL or HORIZONTAL
     * @param dragType      拖拽类型 DRAG_TYPE_FIXED_BEHIND or DRAG_TYPE_FIXED_FOREGROUND
     * @param headerView    header view
     * @param heightOrWidth VERTICAL:header height ，HORIZONTAL:header width
     * @param scrollYOrX    VERTICAL:scrollY ，HORIZONTAL:scrollX
     */
    void transformHeader(PullExpandLayout layout, int orientation, int dragType, @NonNull View headerView,
                         int heightOrWidth, int scrollYOrX);

    /**
     * 变换 footer view
     *
     * @param layout        PullExpandLayout
     * @param orientation   布局方向 VERTICAL or HORIZONTAL
     * @param dragType      拖拽类型 DRAG_TYPE_FIXED_BEHIND or DRAG_TYPE_FIXED_FOREGROUND
     * @param footerView    footer view
     * @param heightOrWidth VERTICAL:header height ，HORIZONTAL:header width
     * @param scrollYOrX    VERTICAL:scrollY ，HORIZONTAL:scrollX
     */
    void transformFooter(PullExpandLayout layout, int orientation, int dragType, @NonNull View footerView,
                         int heightOrWidth, int scrollYOrX);

    /**
     * 变换 content view
     *
     * @param layout        PullExpandLayout
     * @param orientation   布局方向 VERTICAL or HORIZONTAL
     * @param dragType      拖拽类型 DRAG_TYPE_FIXED_BEHIND or DRAG_TYPE_FIXED_FOREGROUND
     * @param contentView   content view
     * @param heightOrWidth VERTICAL:header height ，HORIZONTAL:header width
     * @param scrollYOrX    VERTICAL:scrollY ，HORIZONTAL:scrollX
     */
    void transformContent(PullExpandLayout layout, int orientation, int dragType, @NonNull View contentView,
                          int heightOrWidth, int scrollYOrX);
}
