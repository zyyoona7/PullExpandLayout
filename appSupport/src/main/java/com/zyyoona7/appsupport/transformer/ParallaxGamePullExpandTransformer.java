package com.zyyoona7.appsupport.transformer;

import android.support.annotation.NonNull;
import android.view.View;

import com.zyyoona7.pullexpand.PullExpandLayout;
import com.zyyoona7.pullexpand.transformer.PullExpandTransformer;

public class ParallaxGamePullExpandTransformer implements PullExpandTransformer {

    private static final String TAG = "ParallaxGameTransformer";

    //默认的视差距离
    private static final float DEFAULT_PARALLAX_DISTANCE = 100f;
    //拖拽到此距离时 视差移动满
    private static final float DEFAULT_PULL_PARALLAX_DISTANCE = 100f;

    //视差距离
    private float mParallaxDistance = 0f;
    //拖拽到此距离时 视差移动满
    private float mPullParallaxDistance = 0f;

    public ParallaxGamePullExpandTransformer(float parallaxDistance, float pullParallaxDistance) {
        mParallaxDistance = parallaxDistance;
        mPullParallaxDistance = pullParallaxDistance;
    }

    @Override
    public void transformHeader(PullExpandLayout layout, int orientation, int dragType, @NonNull View headerView,
                                int heightOrWidth, int scrollYOrX) {
        if (orientation == PullExpandLayout.VERTICAL) {
            if (dragType == PullExpandLayout.DRAG_TYPE_TRANSLATE) {

            }
        }
    }

    @Override
    public void transformFooter(PullExpandLayout layout, int orientation, int dragType, @NonNull View footerView,
                                int heightOrWidth, int scrollYOrX) {
        if (orientation == PullExpandLayout.VERTICAL) {
            if (dragType == PullExpandLayout.DRAG_TYPE_TRANSLATE) {

            }
        }
    }

    @Override
    public void transformContent(PullExpandLayout layout, int orientation, int dragType, @NonNull View contentView,
                                 int heightOrWidth, int scrollYOrX) {
        if (dragType == PullExpandLayout.DRAG_TYPE_FIXED_FOREGROUND) {
            if (mParallaxDistance == 0) {
                mParallaxDistance = DEFAULT_PARALLAX_DISTANCE;
            }
            if (mPullParallaxDistance == 0) {
                //拖拽到此距离后 视差距离滑到 mParallaxDistance
                mPullParallaxDistance = DEFAULT_PULL_PARALLAX_DISTANCE;
            }
            int absScrollYOrX = Math.abs(scrollYOrX);
            boolean isVertical = orientation == PullExpandLayout.VERTICAL;
            float percent = absScrollYOrX > mPullParallaxDistance ? 1f : absScrollYOrX / mPullParallaxDistance;
            if (scrollYOrX < 0) {
                //拖拽HEADER
                if (isVertical) {
                    contentView.setTranslationY(-mParallaxDistance * percent);
                } else {
                    contentView.setTranslationX(-mParallaxDistance * percent);
                }
            } else if (scrollYOrX > 0) {
                //拖拽FOOTER
                if (isVertical) {
                    contentView.setTranslationY(mParallaxDistance * percent);
                } else {
                    contentView.setTranslationX(mParallaxDistance * percent);
                }
            } else {
                //还原
                contentView.setTranslationY(0f);
                contentView.setTranslationX(0f);
            }
        }
    }
}
