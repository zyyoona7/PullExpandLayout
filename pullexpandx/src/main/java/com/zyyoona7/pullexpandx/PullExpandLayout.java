package com.zyyoona7.pullexpandx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;

import com.zyyoona7.pullexpandx.listener.OnPullExpandChangedListener;
import com.zyyoona7.pullexpandx.listener.OnPullExpandStateListener;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 拉拽展开布局
 * 手势部分代码来自 https://github.com/liaoinstan/SpringView
 * 并且修复了 Header Footer 同时存在的情况下，飞速上滑或者下滑会拉出 Footer 或者 Header 的问题
 * <p>
 *
 * @author zyyoona7
 * @since 2019/5/15
 */
public class PullExpandLayout extends HeaderFooterLayout {

    private static final String TAG = "PullExpandLayout";

    // TODO: 2019-05-17 仿微信游戏视觉差效果，横向拖拽

    //阻尼效果比率 0-1之间，越大越顺畅
    private static final float DEFAULT_DRAG_RATE = 0.4f;
    //拖拽类型
    //跟随ContentView平移
    public static final int DRAG_TYPE_TRANSLATE = 0;
    //滚动在ContentView之后
    public static final int DRAG_TYPE_FIXED_BEHIND = 1;
    //固定在ContentView之前
    public static final int DRAG_TYPE_FIXED_FOREGROUND = 2;

    //Header 或者 Footer 状态
    //已经展开
    public static final int STATE_EXPANDED = 0;
    //正在展开
    public static final int STATE_EXPANDING = 1;
    //正在收缩
    public static final int STATE_COLLAPSING = 2;
    //已经收缩
    public static final int STATE_COLLAPSED = 3;

    private boolean DEBUG = false;

    //头部高度
    private int mHeaderHeight;
    //头部宽度
    private int mHeaderWidth;
    //头部显示的阈值，显示或者隐藏高度超过此数值即打开或者关闭
    private int mHeaderDragThreshold;
    //头部最大拖动高度
    private int mHeaderMaxDragDistance;
    //尾部高度
    private int mFooterHeight;
    //尾部宽度
    private int mFooterWidth;
    //尾部显示或隐藏的阈值，显示或者隐藏高度超过此数值即打开或者关闭
    private int mFooterDragThreshold;
    //尾部最大拖动高度
    private int mFooterMaxDragDistance;
    //Scroller
    private Scroller mScroller;
    //手指触摸/正在滑动
    private boolean mIsFingerTouched = false;
    //是否有操作，比如按下滑动之类的，
    // 控制 computeScroll 没有人为操作的情况下不一直执行
    private boolean mIsComputeScrollCanCheckState = false;
    //头部是否展开
    private boolean mIsHeaderExpanded = false;
    //尾部是否展开
    private boolean mIsFooterExpanded = false;

    //头部开关
    private boolean mIsHeaderEnabled;
    //尾部开关
    private boolean mIsFooterEnabled;

    //阻尼效果比率 0-1之间，越大越顺畅
    @FloatRange(from = 0f, to = 1f)
    private float mDragRate;
    //拖动类型
    @DragType
    private int mDragType;

    //储存手指拉动上次的Y坐标
    private float mLastY;
    private float mLastX;
    //记录单次滚动x,y轴偏移量
    private float mDeltaY;
    private float mDeltaX;
    //滑动事件目前是否在本控件的控制中
    // （用于过渡滑动事件：比如正在滚动recyclerView到顶部后自动切换到Layout处理后续事件进行下拉）
    private boolean mIsInSelfControl = false;
    //是否需要自身拦截和处理滑动事件
    private boolean mIsNeedSelfMove = false;

    //最后移动操作时的 scrollY ，结合 mDeltaY 用于判断快速上下滑
    private float mLastScrollY = 0;
    //最后移动操作时的 scrollX ，结合 mDeltaX 用于判断快速上下滑
    private float mLastScrollX = 0;

    //手势检测 用于检测当前手势和滑动方向
    private VelocityTracker mVelocityTracker;
    private int mMaximumVelocity;
//    private float mCurrentVelocityY;
//    private float mCurrentVelocityX;

    private OnPullExpandChangedListener mOnPullExpandChangedListener;
    private OnPullExpandStateListener mOnPullExpandStateListener;
    //当前Header的状态
    private int mCurrentHeaderState = STATE_COLLAPSED;
    //当前Footer的状态
    private int mCurrentFooterState = STATE_COLLAPSED;

    public PullExpandLayout(Context context) {
        this(context, null);
    }

    public PullExpandLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullExpandLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mScroller = new Scroller(context);
    }

    /**
     * 初始化自定义属性
     *
     * @param context context
     * @param attrs   attrs
     */
    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PullExpandLayout);
        mHeaderLayoutId = typedArray.getResourceId(R.styleable.PullExpandLayout_pel_headerLayoutId, 0);
        mHeaderId = typedArray.getResourceId(R.styleable.PullExpandLayout_pel_headerId, View.NO_ID);
        mContentId = typedArray.getResourceId(R.styleable.PullExpandLayout_pel_contentId, View.NO_ID);
        mFooterLayoutId = typedArray.getResourceId(R.styleable.PullExpandLayout_pel_footerLayoutId, 0);
        mFooterId = typedArray.getResourceId(R.styleable.PullExpandLayout_pel_footerId, View.NO_ID);
        mDragRate = typedArray.getFloat(R.styleable.PullExpandLayout_pel_dragRate, DEFAULT_DRAG_RATE);
        if (mDragRate > 1 || mDragRate < 0) {
            mDragRate = DEFAULT_DRAG_RATE;
        }
        mDragType = typedArray.getInt(R.styleable.PullExpandLayout_pel_dragType, DRAG_TYPE_TRANSLATE);
        mHeaderDragThreshold = typedArray.getDimensionPixelOffset(R.styleable.PullExpandLayout_pel_headerDragThreshold, 0);
        mFooterDragThreshold = typedArray.getDimensionPixelOffset(R.styleable.PullExpandLayout_pel_footerDragThreshold, 0);
        mHeaderMaxDragDistance = typedArray.getDimensionPixelOffset(R.styleable.PullExpandLayout_pel_headerMaxDragDistance, 0);
        mFooterMaxDragDistance = typedArray.getDimensionPixelOffset(R.styleable.PullExpandLayout_pel_footerMaxDragDistance, 0);
        mIsHeaderEnabled = typedArray.getBoolean(R.styleable.PullExpandLayout_pel_headerEnabled, true);
        mIsFooterEnabled = typedArray.getBoolean(R.styleable.PullExpandLayout_pel_footerEnabled, true);
        typedArray.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mHeaderView != null) {
            mHeaderHeight = mHeaderView.getMeasuredHeight();
            mHeaderWidth = mHeaderView.getMeasuredWidth();
            mHeaderDragThreshold = mHeaderDragThreshold == 0
                    ? (mOrientation == VERTICAL ? mHeaderHeight / 3 : mHeaderWidth / 3)
                    : mHeaderDragThreshold;
            mHeaderMaxDragDistance = mHeaderMaxDragDistance == 0
                    ? (mOrientation == VERTICAL ? mHeaderHeight : mHeaderWidth)
                    : mHeaderMaxDragDistance;
        }

        if (mFooterView != null) {
            mFooterHeight = mFooterView.getMeasuredHeight();
            mFooterWidth = mFooterView.getMeasuredWidth();
            mFooterDragThreshold = mFooterDragThreshold == 0
                    ? (mOrientation == VERTICAL ? mFooterHeight / 3 : mFooterWidth / 3)
                    : mFooterDragThreshold;
            mFooterMaxDragDistance = mFooterMaxDragDistance == 0
                    ? (mOrientation == VERTICAL ? mFooterHeight : mFooterWidth)
                    : mFooterMaxDragDistance;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (mDragType == DRAG_TYPE_FIXED_BEHIND) {
            if (mContentView != null) {
                mContentView.bringToFront();
            }
        } else if (mDragType == DRAG_TYPE_FIXED_FOREGROUND) {
            if (mHeaderView != null) {
                mHeaderView.bringToFront();
            }

            if (mFooterView != null) {
                mFooterView.bringToFront();
            }
        }
    }

    /**
     * 为什么重写次方法，因为需要平滑滚动，而重写 onInterceptTouchEvent 等只是消耗事件，并不能随时释放事件
     * 详情 https://juejin.im/entry/585cc680128fe1006de3adaf
     *
     * @param ev MotionEvent
     * @return true consume
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!isEnabled()) {
            return super.dispatchTouchEvent(ev);
        }

        dealMulTouchEvent(ev);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mIsNeedSelfMove = false;
                mIsFingerTouched = true;
                mIsComputeScrollCanCheckState = true;
                mLastScrollY = getScrollY();
                mLastScrollX = getScrollX();
                if (DEBUG) {
                    Log.d(TAG, "dispatchTouchEvent ACTION_DOWN...");
                }
                break;
            case MotionEvent.ACTION_MOVE:
                mIsFingerTouched = true;
                mIsComputeScrollCanCheckState = true;
                mIsNeedSelfMove = isNeedSelfMoveVertical() || isNeedSelfMoveHorizontal();
                //需要自身滚动，并且自身并没有在控制滚动
                if (mIsNeedSelfMove && !mIsInSelfControl) {
                    if (DEBUG) {
                        Log.d(TAG, "dispatchTouchEvent in self Control.");
                    }
                    //把内部控件的事件转发给本控件处理
                    mIsInSelfControl = true;
                    ev.setAction(MotionEvent.ACTION_CANCEL);
                    MotionEvent ev2 = MotionEvent.obtain(ev);
                    super.dispatchTouchEvent(ev);
                    ev2.setAction(MotionEvent.ACTION_DOWN);
                    return super.dispatchTouchEvent(ev2);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsFingerTouched = false;
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 处理多点触控的情况，准确地计算Y坐标和移动距离dy
     * 同时兼容单点触控的情况
     */
    private int mActivePointerId = MotionEvent.INVALID_POINTER_ID;

    /**
     * 多点触控
     *
     * @param ev MotionEvent
     */
    public void dealMulTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final int pointerIndex = ev.getActionIndex();
                final float x = ev.getX(pointerIndex);
                final float y = ev.getY(pointerIndex);
                mLastX = x;
                mLastY = y;
                mActivePointerId = ev.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                final float x = ev.getX(pointerIndex);
                final float y = ev.getY(pointerIndex);
                mDeltaX = x - mLastX;
                mDeltaY = y - mLastY;
                mLastY = y;
                mLastX = x;
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = MotionEvent.INVALID_POINTER_ID;
                break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                final int pointerIndex = ev.getActionIndex();
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId != mActivePointerId) {
                    mLastX = ev.getX(pointerIndex);
                    mLastY = ev.getY(pointerIndex);
                    mActivePointerId = ev.getPointerId(pointerIndex);
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = ev.getActionIndex();
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastX = ev.getX(newPointerIndex);
                    mLastY = ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mIsNeedSelfMove;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mLastScrollY = getScrollY();
                mLastScrollX = getScrollX();
                mVelocityTracker.addMovement(event);
                break;
            case MotionEvent.ACTION_MOVE:
                mVelocityTracker.addMovement(event);
                if (mIsNeedSelfMove) {
                    if (mLastScrollY * (mLastScrollY - mDeltaY) >= 0) {
                        doScrollY();
                        if (DEBUG) {
                            Log.d(TAG, "onTouchEvent doScrollY dy.." + mDeltaY + ",scrollY.." + getScrollY());
                        }
                    } else {
                        doFastScrollY();
                        MotionEvent ev2 = MotionEvent.obtain(event);
                        ev2.setAction(MotionEvent.ACTION_DOWN);
                        super.dispatchTouchEvent(ev2);
                        mIsInSelfControl = false;
                        if (DEBUG) {
                            Log.d(TAG, "onTouchEvent release scroll.." + mDeltaY);
                        }
                    }
                    mLastScrollY = getScrollY();
                    mLastScrollX = getScrollX();
                } else {
                    //手指在产生移动的时候（dy!=0）才重置位置
                    if (mDeltaY != 0 && isFlowY()) {
                        computeScrollYToState(false);
                        mIsInSelfControl = false;
                        //把滚动事件交给内部控件处理
                        event.setAction(MotionEvent.ACTION_DOWN);
                        super.dispatchTouchEvent(event);
                        if (DEBUG) {
                            Log.d(TAG, "onTouchEvent else ..");
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
//                mCurrentVelocityY = mVelocityTracker.getYVelocity();
//                mCurrentVelocityX = mVelocityTracker.getXVelocity();
                mDeltaY = 0;
                mDeltaX = 0;
                mLastX = 0;
                mLastY = 0;
                if (DEBUG) {
                    Log.d(TAG, "onTouchEvent ACTION_UP.." + getScrollY());
                }
                computeScrollYToState(true);
                mVelocityTracker.clear();//清空速度追踪器
                break;
            case MotionEvent.ACTION_CANCEL:
                mVelocityTracker.clear();//清空速度追踪器
                break;
        }
        return true;
    }

    /**
     * 是否需要自身处理垂直滑动事件
     *
     * @return 是否自身处理滑动事件
     */
    private boolean isNeedSelfMoveVertical() {
        //布局方向，垂直方向才执行判断
        if (mOrientation != VERTICAL) {
            return false;
        }
        //垂直布局 横向拖拽距离大于竖直距离则不拦截
        if (Math.abs(mDeltaY) <= Math.abs(mDeltaX)) {
            return false;
        }
        boolean isTop = isChildScrollToTop();
        boolean isBottom = isChildScrollToBottom();
        //下滑
        boolean moveDown = mDeltaY > 0;
        //上滑
        boolean moveUp = mDeltaY < 0;
        //用户禁止了下拉操作，则不控制
        if (!mIsHeaderEnabled && isTop && mDeltaY > 0) {
            return false;
        }
        //用户禁止了上拉操作，则不控制
        if (!mIsFooterEnabled && isBottom && mDeltaY < 0) {
            return false;
        }
        if (mHeaderView != null) {
            //其中的20是一个防止触摸误差的偏移量
            if (moveDown) {
                if (isTop && mDeltaY > 0 || getScrollY() < -20) {
                    if (DEBUG) {
                        Log.d(TAG, "isNeedSelfMove moveDown Header");
                    }
                    return true;
                }
            }
            if (moveUp) {
                //上滑的时候 Header 有可能展开了也有可能没展开
                if (getScrollY() < -20 && isTop) {
                    if (DEBUG) {
                        Log.d(TAG, "isNeedSelfMove moveUp Header");
                    }
                    return true;
                }
            }
        }
        if (mFooterView != null) {
            if (moveUp) {
                if (isBottom && mDeltaY < 0 || getScrollY() > 20) {
                    if (DEBUG) {
                        Log.d(TAG, "isNeedSelfMove moveUp Footer");
                    }
                    return true;
                }
            }
            if (moveDown) {
                //下滑的时候 Footer 有可能展开了也有可能没展开
                if (getScrollY() > 20 && isBottom) {
                    if (DEBUG) {
                        Log.d(TAG, "isNeedSelfMove moveDown Footer");
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 是否需要自身处理水平滑动事件
     *
     * @return 是否需要自身处理
     */
    private boolean isNeedSelfMoveHorizontal() {
        //布局方向，垂直方向才执行以下判断
        if (mOrientation != HORIZONTAL) {
            return false;
        }
        //水平布局 竖直拖拽距离大于大于距离则不拦截
        if (Math.abs(mDeltaX) <= Math.abs(mDeltaY)) {
            return false;
        }
        boolean isLeft = isChildScrollToLeft();
        boolean isRight = isChildScrollToRight();
        //右滑
        boolean moveRight = mDeltaX > 0;
        //左滑
        boolean moveLeft = mDeltaX < 0;
        //用户禁止了下拉操作，则不控制
        if (!mIsHeaderEnabled && isLeft && mDeltaX > 0) {
            return false;
        }
        //用户禁止了上拉操作，则不控制
        if (!mIsFooterEnabled && isRight && mDeltaX < 0) {
            return false;
        }
        if (mHeaderView != null) {
            //其中的20是一个防止触摸误差的偏移量
            if (moveRight) {
                if (isLeft && mDeltaX > 0 || getScrollX() < -20) {
                    if (DEBUG) {
                        Log.d(TAG, "isNeedSelfMove moveDown Header");
                    }
                    return true;
                }
            }
            if (moveLeft) {
                //上滑的时候 Header 有可能展开了也有可能没展开
                if (getScrollX() < -20 && isLeft) {
                    if (DEBUG) {
                        Log.d(TAG, "isNeedSelfMove moveLeft Header");
                    }
                    return true;
                }
            }
        }
        if (mFooterView != null) {
            if (moveLeft) {
                if (isRight && mDeltaX < 0 || getScaleX() > 20) {
                    if (DEBUG) {
                        Log.d(TAG, "isNeedSelfMove moveLeft Footer");
                    }
                    return true;
                }
            }
            if (moveRight) {
                //右滑的时候 Footer 有可能展开了也有可能没展开
                if (getScaleX() > 20 && isRight) {
                    if (DEBUG) {
                        Log.d(TAG, "isNeedSelfMove moveRight Footer");
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断目标View是否滑动到顶部 还能否继续滑动
     *
     * @return
     */
    private boolean isChildScrollToTop() {
        return mContentView != null && !mContentView.canScrollVertically(-1);
    }

    /**
     * 是否滑动到底部
     *
     * @return
     */
    private boolean isChildScrollToBottom() {
        return mContentView != null && !mContentView.canScrollVertically(1);
    }

    /**
     * 是否滑动到最左侧
     *
     * @return
     */
    private boolean isChildScrollToLeft() {
        return mContentView != null && !mContentView.canScrollHorizontally(-1);
    }

    /**
     * 是否滑动到最右侧
     *
     * @return
     */
    private boolean isChildScrollToRight() {
        return mContentView != null && !mContentView.canScrollHorizontally(1);
    }

    /**
     * 判断当前滚动位置是否已经进入可折叠范围了
     *
     * @return
     */
    private boolean isFlowY() {
        return getScrollY() > -30 && getScrollY() < 30;
    }

    /**
     * 执行垂直滚动
     */
    private void doScrollY() {
        if (!mScroller.isFinished()) {
            mScroller.forceFinished(true);
        }
        //根据下拉高度计算位移距离，（越拉越慢）
        int moveDy;
        if (mDeltaY > 0) {
            moveDy = (int) (((mHeaderMaxDragDistance + getScrollY()) / (float) mHeaderMaxDragDistance) * mDeltaY * mDragRate);
        } else {
            moveDy = (int) (((mFooterMaxDragDistance - getScrollY()) / (float) mFooterMaxDragDistance) * mDeltaY * mDragRate);
        }
        scrollBy(0, -moveDy);
        //偏移回调
        onMovingYCallback();
        doOnScrollAndDragVertical();
    }

    /**
     * 执行快速垂直滚动
     */
    private void doFastScrollY() {
        boolean isHeaderShow = mLastScrollY < 0;
        boolean isFooterShow = mLastScrollY > 0;

        if (isHeaderShow && mLastScrollY - mDeltaY > 0) {
            //正在快速上滑，需要关闭header
            //在scrollTo 0 之前执行，为了防止快速滑动有可能会闪动一下，尤其是有背景的时候
            doOnScrollAndDragVertical(0);
            scrollTo(0, 0);
            return;
        }
        if (isFooterShow && mLastScrollY - mDeltaY < 0) {
            //正在快速上滑，需要关闭footer
            //在scrollTo 0 之前执行，为了防止快速滑动有可能会闪动一下，尤其是有背景的时候
            doOnScrollAndDragVertical(0);
            scrollTo(0, 0);
        }
        //偏移回调
        onMovingYCallback();
    }

    /**
     * 根据拖拽类型子滚动和拖拽的时候更新 Header Footer 状态
     */
    private void doOnScrollAndDragVertical() {
        doOnScrollAndDragVertical(getScrollY());
    }

    /**
     * 根据拖拽类型子滚动和拖拽的时候更新 Header Footer 状态
     *
     * @param scrollY scrollY
     */
    private void doOnScrollAndDragVertical(int scrollY) {
        if (mDragType == DRAG_TYPE_FIXED_BEHIND) {
            if (mHeaderView != null && getScrollY() <= 0) {
                mHeaderView.setTranslationY(mHeaderHeight + scrollY);
            }
            if (mFooterView != null && getScrollY() >= 0) {
                mFooterView.setTranslationY(-mFooterHeight + scrollY);
            }
        } else if (mDragType == DRAG_TYPE_FIXED_FOREGROUND) {
            if (mContentView != null) {
                mContentView.setTranslationY(scrollY);
            }
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            mIsComputeScrollCanCheckState = true;
            scrollTo(0, mScroller.getCurrY());
            onMovingYCallback();
            doOnScrollAndDragVertical();
            mLastScrollY = getScrollY();
            mLastScrollX = getScrollX();
            postInvalidateOnAnimation();
        }

        if (mIsComputeScrollCanCheckState && !mIsFingerTouched
                && mScroller.isFinished()) {
            checkVerticalHeaderFooterState();
            if (DEBUG) {
                Log.d(TAG, "computeScroll check state finish.header:"
                        + mIsHeaderExpanded + ",footer:" + mIsFooterExpanded);
            }
            mIsComputeScrollCanCheckState = false;
        }
    }

    /**
     * 通过计算scrollY来判断滚动到最终态
     *
     * @param isAnimateScroll 是否动画滚动
     */
    private void computeScrollYToState(boolean isAnimateScroll) {
        mIsInSelfControl = false;
        //header完全展开了，并且向上推的高度>mHeaderShownThreshold 表示关闭
        if (mIsHeaderExpanded && getScrollY() < 0
                && getScrollY() < -mHeaderDragThreshold
                && getScrollY() > -mHeaderHeight && mCurrentHeaderState == STATE_COLLAPSING) {
            closeHeaderOrFooterVertical(isAnimateScroll);
            return;
        }
        //footer完全展开了，并且向下推的高度>mFooterShownThreshold 表示关闭
        if (mIsFooterExpanded && getScrollY() > 0
                && mFooterHeight - getScrollY() > mFooterDragThreshold
                && mCurrentFooterState == STATE_COLLAPSING) {
            closeHeaderOrFooterVertical(isAnimateScroll);
            return;
        }

        if (-getScrollY() > mHeaderDragThreshold) {
            openHeaderVertical(isAnimateScroll);
        } else if (getScrollY() > mFooterDragThreshold) {
            openFooterVertical(isAnimateScroll);
        } else {
            closeHeaderOrFooterVertical(isAnimateScroll);
        }
    }

    /**
     * 检查垂直方向当前Header Footer的状态
     */
    private void checkVerticalHeaderFooterState() {
        //停止滚动确定一下状态
        int lastHeaderState = mCurrentHeaderState;
        int lastFooterState = mCurrentFooterState;
        if (getScrollY() > 0) {
            mIsFooterExpanded = true;
            mIsHeaderExpanded = false;
            mCurrentHeaderState = STATE_EXPANDED;
            mCurrentFooterState = STATE_COLLAPSED;
        } else if (getScrollY() < 0) {
            mIsHeaderExpanded = true;
            mIsFooterExpanded = false;
            mCurrentHeaderState = STATE_COLLAPSED;
            mCurrentFooterState = STATE_EXPANDED;
        } else {
            mIsHeaderExpanded = false;
            mIsFooterExpanded = false;
            mCurrentHeaderState = STATE_COLLAPSED;
            mCurrentFooterState = STATE_COLLAPSED;
        }

        if (mOnPullExpandChangedListener != null) {
            if (mHeaderView != null) {
                mOnPullExpandChangedListener.onHeaderStateChanged(this, mCurrentHeaderState);
            }

            if (mFooterView != null) {
                mOnPullExpandChangedListener.onFooterStateChanged(this, mCurrentFooterState);
            }
        }

        if (mOnPullExpandStateListener != null) {
            if (mHeaderView != null && lastHeaderState != mCurrentHeaderState) {
                mOnPullExpandStateListener.onHeaderStateChanged(this, mCurrentHeaderState);
            }

            if (mFooterView != null && lastFooterState != mCurrentFooterState) {
                mOnPullExpandStateListener.onFooterStateChanged(this, mCurrentFooterState);
            }
        }
    }

    /**
     * 打开Header
     *
     * @param isAnimateScroll 是否动画滚动
     */
    private void openHeaderVertical(boolean isAnimateScroll) {
        startScrollY(getScrollY(), -(getScrollY() + mHeaderHeight), isAnimateScroll);
    }

    /**
     * 关闭 Header
     *
     * @param isAnimateScroll 是否动画滚动
     */
    private void closeHeaderVertical(boolean isAnimateScroll) {
        closeHeaderOrFooterVertical(isAnimateScroll);
    }

    /**
     * 打开 Footer
     *
     * @param isAnimateScroll 是否动画滚动
     */
    private void openFooterVertical(boolean isAnimateScroll) {
        startScrollY(getScrollY(), mFooterHeight - getScrollY(), isAnimateScroll);
    }

    /**
     * 关闭Footer
     *
     * @param isAnimateScroll 是否动画滚动
     */
    private void closeFooterVertical(boolean isAnimateScroll) {
        closeHeaderOrFooterVertical(isAnimateScroll);
    }

    /**
     * 关闭 Header 或者 Footer
     *
     * @param isAnimateScroll 是否动画滚动
     */
    private void closeHeaderOrFooterVertical(boolean isAnimateScroll) {
        startScrollY(getScrollY(), -getScrollY(), isAnimateScroll);
    }

    /**
     * 通过Scroller滚动Y方向
     *
     * @param startY          startY
     * @param dy              delta y
     * @param isAnimateScroll 是否执行动画滚动
     */
    private void startScrollY(int startY, int dy, boolean isAnimateScroll) {
        if (!mScroller.isFinished()) {
            mScroller.forceFinished(true);
        }
        mScroller.startScroll(0, startY, 0, dy, isAnimateScroll ? 300 : 16);
        postInvalidateOnAnimation();
    }

    /**
     * 垂直滑动时回调
     */
    private void onMovingYCallback() {
        int scrollY = getScrollY();
        int absScrollY = Math.abs(scrollY);
        boolean isExpanding = absScrollY > Math.abs(mLastScrollY);
        boolean isCollapsing = absScrollY < Math.abs(mLastScrollY);

        if (getScrollY() < 0) {
            //操作Header
            int lastHeaderState = mCurrentHeaderState;
            if (isExpanding) {
                mCurrentHeaderState = STATE_EXPANDING;
            } else if (isCollapsing) {
                mCurrentHeaderState = STATE_COLLAPSING;
            }
            if (mOnPullExpandChangedListener != null && mHeaderView != null) {
                mOnPullExpandChangedListener.onHeaderMoving(mOrientation,
                        absScrollY * 1.0f / mHeaderHeight, absScrollY,
                        mHeaderHeight, mHeaderMaxDragDistance);
                mOnPullExpandChangedListener.onHeaderStateChanged(this, mCurrentHeaderState);
            }
            //只有不相等的时候才调用
            if (mOnPullExpandStateListener != null && mHeaderView != null
                    && lastHeaderState != mCurrentHeaderState) {
                mOnPullExpandStateListener.onHeaderStateChanged(this, mCurrentHeaderState);
            }
        } else if (getScrollY() > 0) {
            //操作Footer
            int lastFooterState = mCurrentFooterState;
            if (isExpanding) {
                mCurrentFooterState = STATE_EXPANDING;
            } else if (isCollapsing) {
                mCurrentFooterState = STATE_COLLAPSING;
            }
            if (mOnPullExpandChangedListener != null && mFooterView != null) {
                mOnPullExpandChangedListener.onFooterMoving(mOrientation,
                        absScrollY * 1.0f / mFooterHeight, absScrollY,
                        mFooterHeight, mFooterMaxDragDistance);
                mOnPullExpandChangedListener.onFooterStateChanged(this, mCurrentFooterState);
            }
            //只有不相等的时候才调用
            if (mOnPullExpandStateListener != null && mFooterView != null
                    && lastFooterState != mCurrentFooterState) {
                mOnPullExpandStateListener.onFooterStateChanged(this, mCurrentFooterState);
            }
        } else {
            //还原 Header Footer 都收起了
            int lastHeaderState = mCurrentHeaderState;
            int lastFooterState = mCurrentFooterState;
            mCurrentHeaderState = STATE_COLLAPSED;
            mCurrentFooterState = STATE_COLLAPSED;
            if (mOnPullExpandChangedListener != null) {
                if (mHeaderView != null) {
                    mOnPullExpandChangedListener.onHeaderMoving(mOrientation, scrollY * 1.0f / mHeaderHeight,
                            scrollY, mHeaderHeight, mHeaderMaxDragDistance);
                    mOnPullExpandChangedListener.onHeaderStateChanged(this, mCurrentHeaderState);
                }
                if (mFooterView != null) {
                    mOnPullExpandChangedListener.onFooterMoving(mOrientation, scrollY * 1.0f / mFooterHeight,
                            scrollY, mFooterHeight, mFooterMaxDragDistance);
                    mOnPullExpandChangedListener.onFooterStateChanged(this, mCurrentFooterState);
                }
            }

            //只有不相等的时候才调用
            if (mOnPullExpandStateListener != null) {
                if (mHeaderView != null && lastHeaderState != mCurrentHeaderState) {
                    mOnPullExpandStateListener.onHeaderStateChanged(this, mCurrentHeaderState);
                }
                if (mFooterView != null && lastFooterState != mCurrentFooterState) {
                    mOnPullExpandStateListener.onFooterStateChanged(this, mCurrentFooterState);
                }
            }
        }
    }

    /**
     * 设置拖拽类型
     *
     * @param dragType 拖拽类型
     */
    public void setDragType(@DragType int dragType) {
        mDragType = dragType;
        requestLayout();
    }

    /**
     * 设置拖拽的阻尼系数 越接近0越吃力
     *
     * @param dragRate 阻尼系数
     */
    public void setDragRate(@FloatRange(from = 0f, to = 1f) float dragRate) {
        mDragRate = dragRate;
    }

    /**
     * Header 拖动时打开/关闭的临界值
     *
     * @param headerDragThreshold 临界值
     */
    public void setHeaderDragThreshold(int headerDragThreshold) {
        if (headerDragThreshold > 0) {
            mHeaderDragThreshold = headerDragThreshold;
        }
    }

    /**
     * Footer 拖动时打开/关闭的临界值
     *
     * @param footerDragThreshold 临界值
     */
    public void setFooterDragThreshold(int footerDragThreshold) {
        if (footerDragThreshold > 0) {
            mFooterDragThreshold = footerDragThreshold;
        }
    }

    /**
     * Header 最大拖动距离
     *
     * @param headerMaxDragDistance 最大拖动距离
     */
    public void setHeaderMaxDragDistance(int headerMaxDragDistance) {
        if (headerMaxDragDistance > 0) {
            mHeaderMaxDragDistance = headerMaxDragDistance;
        }
    }

    /**
     * Footer 最大拖动距离
     *
     * @param footerMaxDragDistance 最大拖动距离
     */
    public void setFooterMaxDragDistance(int footerMaxDragDistance) {
        if (footerMaxDragDistance > 0) {
            mFooterMaxDragDistance = footerMaxDragDistance;
        }
    }

    /**
     * Header 是否可用
     *
     * @param headerEnabled 是否可用
     */
    public void setHeaderEnabled(boolean headerEnabled) {
        mIsHeaderEnabled = headerEnabled;
    }

    /**
     * Footer 是否可用
     *
     * @param footerEnabled 是否可用
     */
    public void setFooterEnabled(boolean footerEnabled) {
        mIsFooterEnabled = footerEnabled;
    }

    /**
     * 设置状态改变监听器
     *
     * @param onPullExpandChangedListener listener
     */
    public void setOnPullExpandChangedListener(OnPullExpandChangedListener onPullExpandChangedListener) {
        mOnPullExpandChangedListener = onPullExpandChangedListener;
    }

    /**
     * 设置状态监听器
     * 这个回调对应的方法只有在状态改变的时候回调一次，不会重复回调
     *
     * @param onPullExpandStateListener listener
     */
    public void setOnPullExpandStateListener(OnPullExpandStateListener onPullExpandStateListener) {
        mOnPullExpandStateListener = onPullExpandStateListener;
    }

    /**
     * 设置 Header 打开/关闭
     *
     * @param isExpand 是否展开
     */
    public void setHeaderExpand(boolean isExpand) {
        setHeaderExpand(isExpand, true);
    }

    /**
     * 设置 Header 打开/关闭
     *
     * @param isExpand 是否展开
     * @param isAnim   是否伴随动画
     */
    public void setHeaderExpand(boolean isExpand, boolean isAnim) {
        if (isExpand) {
            if (mOrientation == VERTICAL) {
                openHeaderVertical(isAnim);
            }
        } else {
            if (mOrientation == VERTICAL) {
                closeFooterVertical(isAnim);
            }
        }
    }

    /**
     * 设置 Footer 打开/关闭
     *
     * @param isExpand 是否展开
     */
    public void setFooterExpand(boolean isExpand) {
        setFooterExpand(isExpand, true);
    }

    /**
     * 设置 Footer 打开/关闭
     *
     * @param isExpand 是否展开
     * @param isAnim   是否伴随动画
     */
    public void setFooterExpand(boolean isExpand, boolean isAnim) {
        if (isExpand) {
            if (mOrientation == VERTICAL) {
                openFooterVertical(isAnim);
            }
        } else {
            if (mOrientation == VERTICAL) {
                closeFooterVertical(isAnim);
            }
        }
    }

    @IntDef({DRAG_TYPE_TRANSLATE, DRAG_TYPE_FIXED_BEHIND, DRAG_TYPE_FIXED_FOREGROUND})
    @Retention(RetentionPolicy.SOURCE)
    private @interface DragType {
    }
}
