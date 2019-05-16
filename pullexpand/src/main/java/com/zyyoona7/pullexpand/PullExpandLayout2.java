package com.zyyoona7.pullexpand;

import android.content.Context;
import android.support.v4.widget.ListViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ListView;
import android.widget.Scroller;

public class PullExpandLayout2 extends HeaderFooterLayout {

    private int mTouchSlop;
    private int mHeaderHeight;
    private int mHeaderShownThreshold;
    private int mFooterShownThreshold;
    private int mFooterHeight;
    private Scroller mScroller;
    private boolean mIsHeaderExpanded = false;
    private boolean mIsFooterExpanded = false;
    //手指触摸/正在滑动
    private boolean mIsFingerTouched = false;

    private static final float DRAG_RATE = 0.4f;

    public PullExpandLayout2(Context context) {
        this(context, null);
    }

    public PullExpandLayout2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullExpandLayout2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mScroller = new Scroller(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mHeaderView != null) {
            mHeaderHeight = mHeaderView.getMeasuredHeight();
            mHeaderShownThreshold = mHeaderShownThreshold == 0 ? mHeaderHeight / 3 : mHeaderShownThreshold;
        }

        if (mFooterView != null) {
            mFooterHeight = mFooterView.getMeasuredHeight();
            mFooterShownThreshold = mFooterShownThreshold == 0 ? mFooterHeight / 3 : mFooterShownThreshold;
        }
    }

    float downX = 0;
    float downY = 0;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isEnabled()) {
            return false;
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float x = ev.getX();
                float y = ev.getY();
                float deltaY = y - downY;
                if (deltaY > 0 && (!canChildScrollDown() || mIsFooterExpanded)) {
                    // 下滑操作
                    return true;

                } else if (deltaY < 0 && (!canChildScrollUp() || mIsHeaderExpanded)) {
                    //上滑
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!isEnabled()) {
            return false;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                mIsFingerTouched = true;
                break;
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();
                float deltaY = y - downY;
                if (deltaY < 0) {
                    //上滑
                } else if (deltaY > 0) {
                    //下滑
                }
                if (Math.abs(deltaY) > 0) {
                    doScrollY(-deltaY);
                }
                mIsFingerTouched = true;
                downY = y;
                downX = x;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                downX = 0;
                downY = 0;
                mIsFingerTouched = false;
                //校准状态，展开还是关闭
                calculateState();
                performClick();
                break;
        }

        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void doScrollY(float dy) {
        scrollBy(0, (int) (dy * DRAG_RATE));
    }

    private void calculateState() {
        if (getScrollY() < 0) {
            //Header漏出来了
            int absScrollY = Math.abs(getScrollY());
            if (!mIsHeaderExpanded && absScrollY > mHeaderShownThreshold) {
                //Header展开
                openHeader();
                mIsHeaderExpanded = true;
            } else {
                //Header关闭
                closeHeaderOrFooter();
                mIsHeaderExpanded = false;
            }
        } else if (getScrollY() > 0) {
            //Footer漏出来了
            int scrollY = getScrollY();
            if (!mIsFooterExpanded && scrollY > mFooterShownThreshold) {
                //Footer展开
                openFooter();
                mIsFooterExpanded = true;
            } else {
                //Footer关闭
                closeHeaderOrFooter();
                mIsFooterExpanded = false;
            }
        }
        postInvalidateOnAnimation();
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            scrollTo(0, mScroller.getCurrY());
            postInvalidateOnAnimation();
        }

        if (!mIsFingerTouched && mScroller.isFinished()) {
            //停止滚动确定一下状态
            if (getScrollY() > 0) {
                mIsFooterExpanded = true;
                mIsHeaderExpanded = false;
            } else if (getScrollY() < 0) {
                mIsHeaderExpanded = true;
                mIsFooterExpanded = false;
            } else {
                mIsHeaderExpanded = false;
                mIsFooterExpanded = false;
            }
        }
    }

    private void openHeader() {
        startScrollY(getScrollY(), -(getScrollY() + mHeaderHeight));
    }

    private void openFooter() {
        startScrollY(getScrollY(), mFooterHeight - getScrollY());
    }

    private void closeHeaderOrFooter() {
        startScrollY(getScrollY(), -getScrollY());
    }

    private void startScrollY(int startY, int dy) {
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
        mScroller.startScroll(0, startY, 0, dy);
    }

    /**
     * 目标是否可以向下滚动
     *
     * @return 是否可以向下滚动
     */
    private boolean canChildScrollDown() {
        if (mContentView instanceof ListView) {
            return ListViewCompat.canScrollList((ListView) mContentView, -1);
        }
        return mContentView.canScrollVertically(-1);
    }

    /**
     * 目标是否可以向上滚动
     *
     * @return 是否可以向上滚动
     */
    private boolean canChildScrollUp() {
        if (mContentView instanceof ListView) {
            return ListViewCompat.canScrollList((ListView) mContentView, 1);
        }
        return mContentView.canScrollVertically(1);
    }
}
