package com.zyyoona7.pullexpandx;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 负责测量和布局
 *
 * @author zyyoona7
 * @since 2019/5/15
 */
public abstract class HeaderFooterLayout extends ViewGroup {

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    //拖拽方向
    protected int mOrientation;

    protected View mHeaderView;
    //获取Header的方式，
    // 设置布局id、设置header在Layout中的id、设置headerView为指定的id任选其一
    //Header的布局id
    @LayoutRes
    protected int mHeaderLayoutId;
    //Header的id
    @IdRes
    protected int mHeaderId = View.NO_ID;
    protected View mFooterView;
    //获取Footer的方式，
    // 设置布局id、设置Footer在Layout中的id、设置FooterView为指定的id任选其一
    @LayoutRes
    protected int mFooterLayoutId;
    @IdRes
    protected int mFooterId = View.NO_ID;
    protected View mContentView;
    //获取ContentView方式
    //设置ContentView在当前Layout中的id、设置ContentView为指定的id任选其一
    @IdRes
    protected int mContentId = View.NO_ID;

    private LayoutInflater mLayoutInflater;

    public HeaderFooterLayout(Context context) {
        this(context, null);
    }

    public HeaderFooterLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeaderFooterLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.HeaderFooterLayout);
        mOrientation = typedArray.getInt(R.styleable.HeaderFooterLayout_android_orientation, VERTICAL);
        if (mOrientation != VERTICAL && mOrientation != HORIZONTAL) {
            mOrientation = VERTICAL;
        }
        typedArray.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int childCount = getChildCount();
        if (childCount > 3) {
            throw new RuntimeException("HeaderFooterLayout child count must be <= 3");
        }

        mContentId = mContentId == View.NO_ID ? R.id.pull_expand_layout_content : mContentId;
        mContentView = getViewById(mContentView, mContentId);
        mHeaderId = mHeaderId == View.NO_ID ? R.id.pull_expand_layout_footer : mHeaderId;
        mHeaderView = getViewById(mHeaderView, mHeaderId, mHeaderLayoutId);
        mFooterId = mFooterId == View.NO_ID ? R.id.pull_expand_layout_footer : mFooterId;
        mFooterView = getViewById(mFooterView, mFooterId, mFooterLayoutId);

        if (mContentView == null) {
            if (childCount == 3) {
                mHeaderView = getChildAt(0);
                mContentView = getChildAt(1);
                mFooterView = getChildAt(2);
            } else if (childCount == 2) {
                mHeaderView = getChildAt(0);
                mContentView = getChildAt(1);
            } else if (childCount == 1) {
                mContentView = getChildAt(0);
            }
        }
//        if (mContentView == null) {
//            throw new RuntimeException("HeaderFooterLayout contentView must be not null,please check content id is right.");
//        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        //如果当前布局为 wrap_content 则宽高以contentView的宽高为主
        int contentHeight = 0;
        int contentWidth = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View childView = getChildAt(i);
            measureChildWithMargins(childView, widthMeasureSpec,
                    0, heightMeasureSpec, 0);
            MarginLayoutParams layoutParams = (MarginLayoutParams) childView.getLayoutParams();
            if (childView == mContentView) {
                contentHeight = childView.getMeasuredHeight() + layoutParams.topMargin + layoutParams.bottomMargin;
                contentWidth = childView.getMeasuredWidth() + layoutParams.leftMargin + layoutParams.rightMargin;
            }
        }
        contentHeight += getPaddingTop() + getPaddingBottom();
        contentWidth += getPaddingLeft() + getPaddingRight();

        setMeasuredDimension(resolveSize(contentWidth, widthMeasureSpec),
                resolveSize(contentHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mHeaderView != null) {
            MarginLayoutParams headerLp = (MarginLayoutParams) mHeaderView.getLayoutParams();
            if (mOrientation == VERTICAL) {
                //超出屏幕的上方 相对0，0点的移动因为超出了屏幕所以top要-bottomMargin
                mHeaderView.layout(getPaddingLeft() + headerLp.leftMargin,
                        getPaddingTop() - mHeaderView.getMeasuredHeight() - headerLp.bottomMargin,
                        getMeasuredWidth() - getPaddingRight() - headerLp.rightMargin,
                        getPaddingTop() - headerLp.bottomMargin);

            } else if (mOrientation == HORIZONTAL) {
                //超出屏幕的左边 相对0，0点的移动因为超出了屏幕所以left要-rightMargin
                mHeaderView.layout(getPaddingLeft() - mHeaderView.getMeasuredWidth() - headerLp.rightMargin,
                        getPaddingTop() + headerLp.topMargin, getPaddingLeft() - headerLp.rightMargin,
                        getMeasuredHeight() - getPaddingBottom() - headerLp.bottomMargin);
            }
        }

        if (mContentView != null) {
            MarginLayoutParams contentLp = (MarginLayoutParams) mContentView.getLayoutParams();
            mContentView.layout(getPaddingLeft() + contentLp.leftMargin,
                    contentLp.topMargin + getPaddingTop(),
                    getMeasuredWidth() - contentLp.rightMargin - getPaddingRight(),
                    getMeasuredHeight() - contentLp.bottomMargin - getPaddingBottom());
        }

        if (mFooterView != null) {
            MarginLayoutParams footerLp = (MarginLayoutParams) mFooterView.getLayoutParams();
            if (mOrientation == VERTICAL) {
                mFooterView.layout(getPaddingLeft() + footerLp.leftMargin,
                        getMeasuredHeight() + footerLp.topMargin - getPaddingBottom(),
                        getMeasuredWidth() - getPaddingRight() - footerLp.rightMargin,
                        getMeasuredHeight() + footerLp.topMargin
                                + mFooterView.getMeasuredHeight() - getPaddingBottom());
            } else if (mOrientation == HORIZONTAL) {
                mFooterView.layout(getMeasuredWidth() + footerLp.leftMargin - getPaddingRight(),
                        getPaddingTop() + footerLp.topMargin,
                        getMeasuredWidth() + footerLp.leftMargin
                                + mFooterView.getMeasuredWidth() - getPaddingRight(),
                        getMeasuredHeight() - getPaddingBottom() - footerLp.bottomMargin);
            }
        }
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        final View thisView = this;
        return new MarginLayoutParams(thisView.getContext(), attrs);
    }

    protected LayoutInflater getLayoutInflater() {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(getContext());
        }
        return mLayoutInflater;
    }

    @Nullable
    protected View getViewById(@Nullable View view, @IdRes int idRes) {
        removeFromParent(view);
        if (idRes != View.NO_ID) {
            return findViewById(idRes);
        }
        return null;
    }

    @Nullable
    protected View getViewById(@Nullable View view, @IdRes int idRes, @LayoutRes int layoutId) {
        removeFromParent(view);
        if (idRes != View.NO_ID) {
            return findViewById(idRes);
        } else if (mHeaderLayoutId != 0) {
            return getLayoutInflater().inflate(layoutId, this);
        }
        return null;
    }

    /**
     * 从父控件中移除
     *
     * @param view view
     */
    protected void removeFromParent(View view) {
        if (view != null && view.getParent() != null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }

    /**
     * 设置布局方向
     *
     * @param orientation 布局方向
     */
    public void setOrientation(@Orientation int orientation) {
        if (orientation != mOrientation) {
            mOrientation = orientation;
            requestLayout();
        }
    }

    /**
     * 设置头部View
     *
     * @param headerView 头部View
     */
    public void setHeaderView(View headerView) {
        if (headerView != null) {
            removeFromParent(headerView);
            removeFromParent(mHeaderView);
            mHeaderView = headerView;
            addView(mHeaderView);
        }
    }

    /**
     * 设置尾部View
     *
     * @param footerView 尾部View
     */
    public void setFooterView(View footerView) {
        if (footerView != null) {
            removeFromParent(footerView);
            removeFromParent(mFooterView);
            mFooterView = footerView;
            addView(mFooterView);
        }
    }

    @IntDef({VERTICAL, HORIZONTAL})
    @Retention(RetentionPolicy.SOURCE)
    @interface Orientation {
    }
}
