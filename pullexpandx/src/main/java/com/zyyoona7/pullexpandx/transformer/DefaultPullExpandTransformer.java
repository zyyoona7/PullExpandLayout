package com.zyyoona7.pullexpandx.transformer;

import android.view.View;

import androidx.annotation.NonNull;

import com.zyyoona7.pullexpandx.PullExpandLayout;

public class DefaultPullExpandTransformer implements PullExpandTransformer {

    @Override
    public void transformHeader(PullExpandLayout layout, int orientation, int dragType, @NonNull View headerView,
                                int heightOrWidth, int scrollYOrX) {
        if (dragType == PullExpandLayout.DRAG_TYPE_FIXED_BEHIND) {
            if (isVertical(orientation)) {
                headerView.setTranslationY(heightOrWidth + scrollYOrX);
            } else {
                headerView.setTranslationX(heightOrWidth + scrollYOrX);
            }
        }
    }

    @Override
    public void transformFooter(PullExpandLayout layout, int orientation, int dragType, @NonNull View footerView,
                                int heightOrWidth, int scrollYOrX) {
        if (dragType == PullExpandLayout.DRAG_TYPE_FIXED_BEHIND) {
            if (isVertical(orientation)) {
                footerView.setTranslationY(-heightOrWidth + scrollYOrX);
            } else {
                footerView.setTranslationX(-heightOrWidth + scrollYOrX);
            }
        }
    }

    @Override
    public void transformContent(PullExpandLayout layout, int orientation, int dragType, @NonNull View contentView,
                                 int heightOrWidth, int scrollYOrX) {
        if (dragType == PullExpandLayout.DRAG_TYPE_FIXED_FOREGROUND) {
            if (isVertical(orientation)) {
                contentView.setTranslationY(scrollYOrX);
            } else {
                contentView.setTranslationX(scrollYOrX);
            }
        }
    }

    private boolean isVertical(int orientation) {
        return orientation == PullExpandLayout.VERTICAL;
    }
}
