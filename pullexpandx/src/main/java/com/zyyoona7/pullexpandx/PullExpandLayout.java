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
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;

import com.zyyoona7.pullexpandx.listener.OnPullExpandChangedListener;
import com.zyyoona7.pullexpandx.listener.OnPullExpandStateListener;
import com.zyyoona7.pullexpandx.transformer.DefaultPullExpandTransformer;
import com.zyyoona7.pullexpandx.transformer.PullExpandTransformer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * 拖拽展开布局
 * 手势操作部分代码来自 https://github.com/liaoinstan/SpringView
 * 根据需求做了许多修改，并且修复了 Header Footer 同时存在的情况下，飞速上滑或者下滑会拉出 Footer 或者 Header 的问题
 * <p>
 *
 * @author zyyoona7
 * @since 2019/5/15
 */
public class PullExpandLayout extends HeaderFooterLayout {

    private static final String TAG = "PullExpandLayout";

    //阻尼效果比率 0-1之间，越大越顺畅
    private static final float DEFAULT_DRAG_RATE = 0.4f;
    //拖拽类型
    //Header Footer 跟随ContentView平移
    public static final int DRAG_TYPE_TRANSLATE = 0;
    //Header Footer 固定在ContentView之后
    public static final int DRAG_TYPE_FIXED_BEHIND = 1;
    //Header Footer 固定在ContentView之前
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

    private boolean mIsDebug = false;

    //头部高度
    private int mHeaderHeight;
    //头部宽度
    private int mHeaderWidth;
    //头部显示的阈值，显示或者隐藏高度超过此数值即打开或者关闭
    private int mHeaderDragThreshold;
    //头部显示的阈值比率，mHeaderHeight(或者Width)*mHeaderDragThresholdRage=mHeaderDragThreshold
    @FloatRange(from = 0f, to = 1f)
    private float mHeaderDragThresholdRate;
    //头部最大拖动距离
    private int mHeaderMaxDragDistance;
    //头部最大拖动距离的比率 mHeaderHeight(或者Width)*mHeaderMaxDragDistanceRate=mHeaderMaxDragDistance;
    @FloatRange(from = 1f)
    private float mHeaderMaxDragDistanceRate;
    //尾部高度
    private int mFooterHeight;
    //尾部宽度
    private int mFooterWidth;
    //尾部显示或隐藏的阈值，显示或者隐藏高度超过此数值即打开或者关闭
    private int mFooterDragThreshold;
    //尾部显示的阈值比率，mFooterHeight(或者Width)*mFooterDragThresholdRate=mFooterDragThreshold
    @FloatRange(from = 1f)
    private float mFooterDragThresholdRate;
    //尾部最大拖动距离
    private int mFooterMaxDragDistance;
    //尾部最大拖动距离的比率 mFooterHeight(或者Width)*mHeaderMaxDragDistanceRate=mHeaderMaxDragDistance;
    private float mFooterMaxDragDistanceRate;
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

    private List<OnPullExpandChangedListener> mOnPullExpandChangedListeners;
    private List<OnPullExpandStateListener> mOnPullExpandStateListeners;
    //当前Header的状态
    private int mCurrentHeaderState = STATE_COLLAPSED;
    //当前Footer的状态
    private int mCurrentFooterState = STATE_COLLAPSED;

    //PullExpandLayout 转换器
    private PullExpandTransformer mPullExpandTransformer;

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
        mHeaderDragThresholdRate = typedArray.getFloat(R.styleable.PullExpandLayout_pel_headerDragThresholdRate, 0f);
        mFooterDragThreshold = typedArray.getDimensionPixelOffset(R.styleable.PullExpandLayout_pel_footerDragThreshold, 0);
        mFooterDragThresholdRate = typedArray.getFloat(R.styleable.PullExpandLayout_pel_footerDragThresholdRate, 0f);
        mHeaderDragThresholdRate = getDragThresholdRateInRange(mHeaderDragThresholdRate);
        mFooterDragThresholdRate = getDragThresholdRateInRange(mFooterDragThresholdRate);
        mHeaderMaxDragDistance = typedArray.getDimensionPixelOffset(R.styleable.PullExpandLayout_pel_headerMaxDragDistance, 0);
        mHeaderMaxDragDistanceRate = typedArray.getFloat(R.styleable.PullExpandLayout_pel_headerMaxDragDistanceRate, 0f);
        mHeaderMaxDragDistanceRate = getMaxDragDisRateInRange(mHeaderMaxDragDistanceRate);
        mFooterMaxDragDistance = typedArray.getDimensionPixelOffset(R.styleable.PullExpandLayout_pel_footerMaxDragDistance, 0);
        mFooterMaxDragDistanceRate = typedArray.getFloat(R.styleable.PullExpandLayout_pel_footerMaxDragDistanceRate, 0f);
        mFooterMaxDragDistanceRate = getMaxDragDisRateInRange(mFooterMaxDragDistanceRate);
        mIsHeaderEnabled = typedArray.getBoolean(R.styleable.PullExpandLayout_pel_headerEnabled, true);
        mIsFooterEnabled = typedArray.getBoolean(R.styleable.PullExpandLayout_pel_footerEnabled, true);
        typedArray.recycle();
    }

    /**
     * 控制 drag threshold 不能超出范围
     *
     * @param dragRate drag threshold rate
     * @return drag threshold rate in range
     */
    private float getDragThresholdRateInRange(float dragRate) {
        if (dragRate < 0f) {
            dragRate = 0f;
        } else if (dragRate > 1f) {
            dragRate = 1f;
        }
        return dragRate;
    }

    /**
     * 控制 最大拖拽距离比率不能超出范围
     *
     * @param maxDragDisRate max drag distance rate
     * @return max drag distance rate in range
     */
    private float getMaxDragDisRateInRange(float maxDragDisRate) {
        if (maxDragDisRate != 0 && maxDragDisRate < 1f) {
            maxDragDisRate = 1f;
        }
        return maxDragDisRate;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mHeaderView != null) {
            mHeaderHeight = mHeaderView.getMeasuredHeight();
            mHeaderWidth = mHeaderView.getMeasuredWidth();
            mHeaderDragThreshold = getDragThreshold(mHeaderHeight, mHeaderWidth,
                    mHeaderDragThreshold, mHeaderDragThresholdRate);
            mHeaderMaxDragDistance = getMaxDragDistance(mHeaderHeight, mHeaderWidth,
                    mHeaderMaxDragDistance, mHeaderMaxDragDistanceRate);
        }

        if (mFooterView != null) {
            mFooterHeight = mFooterView.getMeasuredHeight();
            mFooterWidth = mFooterView.getMeasuredWidth();
            mFooterDragThreshold = getDragThreshold(mFooterHeight, mFooterWidth,
                    mFooterDragThreshold, mFooterDragThresholdRate);
            mFooterMaxDragDistance = getMaxDragDistance(mFooterHeight, mFooterWidth,
                    mFooterMaxDragDistance, mFooterMaxDragDistanceRate);
        }

        if (mIsDebug) {
            Log.d(TAG, "onMeasure: header height:" + mHeaderHeight + ",header width:" + mHeaderWidth
                    + ",header max drag height" + mHeaderMaxDragDistance);

            Log.d(TAG, "onMeasure: footer height:" + mFooterHeight + ",footer width:" + mFooterWidth
                    + ",footer max drag height" + mFooterMaxDragDistance);
        }
    }

    /**
     * 获取 Header 或者 Footer 的拖拽临界值
     *
     * @param height        Header 或 Footer 高度
     * @param width         Header 或 Footer 宽度
     * @param dragThreshold 属性获取的 dragThreshold
     * @param rate          属性获取的 drag rate
     * @return Header 或者 Footer 的拖拽临界值
     */
    private int getDragThreshold(int height, int width, int dragThreshold, float rate) {
        int defaultThreshold = isVertical() ? height / 3 : width / 3;
        if (dragThreshold == 0 && rate == 0f) {
            return defaultThreshold;
        }
        if (rate > 0f) {
            return (int) (isVertical() ? height * rate : width * rate);
        }
        if (dragThreshold > 0) {
            return dragThreshold;
        }
        return defaultThreshold;
    }

    /**
     * 获取 Header 或者 Footer 的最大拖拽距离
     *
     * @param height          Header 或 Footer 高度
     * @param width           Header 或 Footer 宽度
     * @param maxDragDistance 属性获取的最大拖拽距离
     * @param rate            属性获取的最大拖拽距离比率
     * @return
     */
    private int getMaxDragDistance(int height, int width, int maxDragDistance, float rate) {
        int defaultMaxDragDistance = isVertical() ? height : width;
        if (maxDragDistance == 0 && rate == 0) {
            return defaultMaxDragDistance;
        }

        if (rate > 0f) {
            return (int) (isVertical() ? height * rate : width * rate);
        }
        if (maxDragDistance > 0
                && (isVertical() ? maxDragDistance > height : maxDragDistance > width)) {
            return maxDragDistance;
        }

        return defaultMaxDragDistance;
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

        //初始化结束后执行一次状态变化
        onMovingAndStateCallback(false);

        if (mIsDebug) {
            Log.d(TAG, "onLayout: execute...");
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mIsDebug) {
            Log.d(TAG, "onSizeChanged: execute...");
        }
        //当前View的尺寸发生改变时同步一下状态
        if ((oldw != 0 && w != oldw) || (oldh != 0 && h != oldh)) {
            computeScrollToState(true);
        }
    }

    /**
     * 当 Header 的尺寸发生变化时调用
     *
     * @param headerView headerView
     * @param left       current left
     * @param top        current top
     * @param right      current right
     * @param bottom     current bottom
     * @param oldLeft    old left
     * @param oldTop     old top
     * @param oldRight   old right
     * @param oldBottom  old bottom
     */
    @Override
    protected void onHeaderLayoutChanged(@NonNull View headerView, int left, int top, int right,
                                         int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (mCurrentHeaderState == STATE_EXPANDED) {
            //当Header展开时并且Header尺寸发生变化，需要同步一下状态，否则会卡住
            if (isVertical()) {
                openHeader(true);
            }
        }
        if (mIsDebug) {
            Log.d(TAG, "onHeaderLayoutChanged: execute...");
        }
    }

    /**
     * 当 Header 的尺寸发生变化时调用
     *
     * @param footerView footerView
     * @param left       current left
     * @param top        current top
     * @param right      current right
     * @param bottom     current bottom
     * @param oldLeft    old left
     * @param oldTop     old top
     * @param oldRight   old right
     * @param oldBottom  old bottom
     */
    @Override
    protected void onFooterLayoutChanged(@NonNull View footerView, int left, int top, int right,
                                         int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (mCurrentFooterState == STATE_EXPANDED) {
            //当Footer展开时并且Footer尺寸发生变化，需要同步一下状态，否则会卡住
            openFooter(true);
        }
        if (mIsDebug) {
            Log.d(TAG, "onHeaderLayoutChanged: execute...");
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
                if (mIsDebug) {
                    Log.d(TAG, "dispatchTouchEvent ACTION_DOWN...");
                }
                break;
            case MotionEvent.ACTION_MOVE:
                mIsFingerTouched = true;
                mIsComputeScrollCanCheckState = true;
                mIsNeedSelfMove = isNeedSelfMoveVertical() || isNeedSelfMoveHorizontal();
                //需要自身滚动，并且自身并没有在控制滚动
                if (mIsNeedSelfMove && !mIsInSelfControl) {
                    if (mIsDebug) {
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
                onActionMove(event);
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
                if (mIsDebug) {
                    Log.d(TAG, "onTouchEvent ACTION_UP.." + getScrollY());
                }
                computeScrollToState(true);
                callReleaseChangedListeners();
                mVelocityTracker.clear();//清空速度追踪器
                break;
            case MotionEvent.ACTION_CANCEL:
                mVelocityTracker.clear();//清空速度追踪器
                break;
        }
        return true;
    }

    /**
     * 分发 ACTION_DOWN 事件
     *
     * @param event
     */
    private void dispatchSuperActionDown(MotionEvent event) {
        MotionEvent ev2 = MotionEvent.obtain(event);
        ev2.setAction(MotionEvent.ACTION_DOWN);
        super.dispatchTouchEvent(ev2);
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
        if (!mIsHeaderEnabled && isTop && moveDown) {
            return false;
        }
        //用户禁止了上拉操作，则不控制
        if (!mIsFooterEnabled && isBottom && moveUp) {
            return false;
        }
        if (mHeaderView != null) {
            //其中的20是一个防止触摸误差的偏移量
            //getScrollY() < -20 有可能在手动开启的 Header，contentView并没有到达顶部 上滑后再下滑
            //如果 Header 已经拉到头了则不拦截
            if (moveDown && (isTop || (getScrollY() < -20 && Math.abs(getScrollY()) < mHeaderHeight))) {
                if (mIsDebug) {
                    Log.d(TAG, "isNeedSelfMove moveDown Header");
                }
                return true;
            }
            if (moveUp && getScrollY() < -20) {
                //上滑的时候 Header 有可能展开了也有可能没展开
                //有可能通过方法展开后，contentView可能不在顶部
                if (mIsDebug) {
                    Log.d(TAG, "isNeedSelfMove moveUp Header");
                }
                return true;
            }
        }
        if (mFooterView != null) {
            //getScrollY() > 20 有可能在手动开启的 Footer，contentView并没有到达底部 下滑后再上滑
            //如果 Footer 已经拉到头了，则不拦截
            if (moveUp && (isBottom || (getScrollY() > 20 && getScrollY() < mFooterHeight))) {
                if (mIsDebug) {
                    Log.d(TAG, "isNeedSelfMove moveUp Footer");
                }
                return true;
            }
            if (moveDown && getScrollY() > 20) {
                //下滑的时候 Footer 有可能展开了也有可能没展开
                //有可能通过方法展开后，contentView可能不在底部
                if (mIsDebug) {
                    Log.d(TAG, "isNeedSelfMove moveDown Footer");
                }
                return true;
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
        //水平布局 竖直拖拽距离大于水平距离则不拦截
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
        if (!mIsHeaderEnabled && isLeft && moveRight) {
            return false;
        }
        //用户禁止了上拉操作，则不控制
        if (!mIsFooterEnabled && isRight && moveLeft) {
            return false;
        }
        if (mHeaderView != null) {
            //其中的20是一个防止触摸误差的偏移量
            //getScrollX() < -20 有可能在手动开启的 Header，contentView并没有到达最左侧 左滑后再右滑
            //如果 Header 已经拉到头了则不拦截
            if (moveRight && (isLeft || (getScrollX() < -20 && Math.abs(getScrollX()) < mHeaderWidth))) {
                if (mIsDebug) {
                    Log.d(TAG, "isNeedSelfMove moveRight Header");
                }
                return true;
            }
            if (moveLeft && getScrollX() < -20) {
                //上滑的时候 Header 有可能展开了也有可能没展开
                //有可能通过方法展开后，contentView可能不在最左侧
                if (mIsDebug) {
                    Log.d(TAG, "isNeedSelfMove moveLeft Header");
                }
                return true;
            }
        }
        if (mFooterView != null) {
            //getScrollX() > 20 有可能在手动开启的 Footer，contentView并没有到达最右侧 右滑后再左滑
            //如果 Footer 已经拉到头了则不拦截
            if (moveLeft && (isRight || (getScrollX() > 20 && getScrollX() < mFooterWidth))) {
                if (mIsDebug) {
                    Log.d(TAG, "isNeedSelfMove moveLeft Footer");
                }
                return true;
            }
            if (moveRight && getScrollX() > 20) {
                //右滑的时候 Footer 有可能展开了也有可能没展开
                //有可能通过方法展开后，contentView可能不在最右侧
                if (mIsDebug) {
                    Log.d(TAG, "isNeedSelfMove moveRight Footer");
                }
                return true;
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
     * 判断当前滚动位置是否已经进入可折叠范围了
     *
     * @return
     */
    private boolean isFlowX() {
        return getScrollX() > -30 && getScrollX() < 30;
    }

    /**
     * 手指滑动的时候执行
     *
     * @param event MotionEvent
     */
    private void onActionMove(MotionEvent event) {
        if (mIsNeedSelfMove) {
            doScrollOrFastScroll(event);
            mLastScrollY = getScrollY();
            mLastScrollX = getScrollX();
        } else {
            boolean isResetState = isVertical() ? mDeltaY != 0 && isFlowY()
                    : mDeltaX != 0 && isFlowX();
            //手指在产生移动的时候（dy!=0 || dx!=0）才重置位置
            if (isResetState) {
                computeScrollToState(false);
                mIsInSelfControl = false;
                //把滚动事件交给内部控件处理
                dispatchSuperActionDown(event);
            }
        }
    }

    /**
     * 执行滚动或者快速滚动
     *
     * @param event MotionEvent
     */
    private void doScrollOrFastScroll(MotionEvent event) {
        //如果这一次的滚动相较与上一次的滚动做乘法正负符号发生变化则表示滚动太快 视作快速滚动
        boolean isNotFastScroll = isVertical() ? mLastScrollY * (mLastScrollY - mDeltaY) >= 0
                : mLastScrollX * (mLastScrollX - mDeltaX) >= 0;
        if (isNotFastScroll) {
            doScroll();
            if (mIsDebug) {
                Log.d(TAG, "doScrollOrFastScroll dy or dx.." + (isVertical() ? mDeltaY : mDeltaX)
                        + ",scrollYOrX.." + (isVertical() ? getScrollY() : getScrollX()));
            }
        } else {
            doFastScroll();
            dispatchSuperActionDown(event);
            mIsInSelfControl = false;
            if (mIsDebug) {
                Log.d(TAG, "doScrollOrFastScroll release scroll.." + (isVertical() ? mDeltaY : mDeltaX));
            }
        }
    }

    /**
     * 执行滚动
     */
    private void doScroll() {
        if (!mScroller.isFinished()) {
            mScroller.forceFinished(true);
        }
        //根据下拉高度计算位移距离，（越拉越慢）
        float moveDyOrDx;
        float deltaYOrX = isVertical() ? mDeltaY : mDeltaX;
        int scrollYOrX = isVertical() ? getScrollY() : getScrollX();
        if (deltaYOrX > 0) {
            //下滑操作
            if (scrollYOrX <= 0) {
                //当前需要显示Header
                moveDyOrDx = (mHeaderMaxDragDistance + scrollYOrX) * 1.0f
                        / (mHeaderMaxDragDistance == 0 ? 1f : mHeaderMaxDragDistance) * deltaYOrX * mDragRate;
                if (mIsDebug) {
                    Log.d(TAG, "doScroll: 下/右滑操作，需要显示Header");
                }
            } else {
                //当前需要隐藏Footer
                moveDyOrDx = (mFooterMaxDragDistance + scrollYOrX) * 1.0f
                        / (mFooterMaxDragDistance == 0 ? 1f : mFooterMaxDragDistance) * deltaYOrX * mDragRate;
                if (mIsDebug) {
                    Log.d(TAG, "doScroll: 下/右滑操作，需要隐藏Footer");
                }
            }
        } else {
            //上滑操作
            if (scrollYOrX >= 0) {
                //当前需要显示Footer
                moveDyOrDx = (mFooterMaxDragDistance - scrollYOrX) * 1.0f
                        / (mFooterMaxDragDistance == 0 ? 1f : mFooterMaxDragDistance) * deltaYOrX * mDragRate;
                if (mIsDebug) {
                    Log.d(TAG, "doScroll: 上/左滑操作，需要显示Footer");
                }
            } else {
                //当前需要隐藏Header
                moveDyOrDx = (mHeaderMaxDragDistance - scrollYOrX) * 1.0f
                        / (mHeaderMaxDragDistance == 0 ? 1f : mHeaderMaxDragDistance) * deltaYOrX * mDragRate;
                if (mIsDebug) {
                    Log.d(TAG, "doScroll: 上/左滑操作，需要隐藏Header");
                }
            }
        }
        if (mIsDebug) {
            Log.d(TAG, "doScroll -moveDyOrDx:" + (-moveDyOrDx));
        }
        //负数向上取整,Math.floor(-1.1)=-2, Math.floor(0.1)=0
        //正数向上取整,Math.ceil(0.1)=1, Math.ceil(-1.1)=-1
        int dyOrDx = 0;
        if (moveDyOrDx < 0) {
            dyOrDx = (int) Math.floor(moveDyOrDx);
        } else if (moveDyOrDx > 0) {
            dyOrDx = (int) Math.ceil(moveDyOrDx);
        }
        if (isVertical()) {
            scrollBy(0, -dyOrDx);
        } else {
            scrollBy(-dyOrDx, 0);
        }
        //偏移回调
        onMovingAndStateCallback();
        doOnScrollAndDrag();
    }

    /**
     * 执行快速滚动
     */
    private void doFastScroll() {
        boolean isHeaderShow = isVertical() ? mLastScrollY < 0 : mLastScrollX < 0;
        boolean isFooterShow = isVertical() ? mLastScrollY > 0 : mLastScrollX > 0;

        if (isHeaderShow
                && (isVertical() ? mLastScrollY - mDeltaY > 0 : mLastScrollX - mDeltaX > 0)) {
            //正在快速上/右滑，需要关闭header
            //在scrollTo 0 之前执行，为了防止快速滑动有可能会闪动一下，尤其是有背景的时候
            doOnScrollAndDrag(0);
            scrollTo(0, 0);
            return;
        }
        if (isFooterShow
                && (isVertical() ? mLastScrollY - mDeltaY < 0 : mLastScrollX - mDeltaX < 0)) {
            //正在快速上/左滑，需要关闭footer
            //在scrollTo 0 之前执行，为了防止快速滑动有可能会闪动一下，尤其是有背景的时候
            doOnScrollAndDrag(0);
            scrollTo(0, 0);
        }
        //偏移回调
        onMovingAndStateCallback();
    }

    /**
     * 根据拖拽类型子滚动和拖拽的时候更新 Header Footer 状态
     */
    private void doOnScrollAndDrag() {
        doOnScrollAndDrag(isVertical() ? getScrollY() : getScrollX());
    }

    /**
     * 根据拖拽类型子滚动和拖拽的时候更新 Header Footer 状态
     *
     * @param scrollYOrX scrollY or scrollX
     */
    private void doOnScrollAndDrag(int scrollYOrX) {
        if (mPullExpandTransformer == null) {
            mPullExpandTransformer = new DefaultPullExpandTransformer();
        }
        if (mDragType == DRAG_TYPE_FIXED_BEHIND) {
            transformForFixedBehind(mOrientation, scrollYOrX);
        } else if (mDragType == DRAG_TYPE_FIXED_FOREGROUND) {
            transformNormal(mOrientation, DRAG_TYPE_FIXED_FOREGROUND, scrollYOrX);
        } else {
            transformNormal(mOrientation, DRAG_TYPE_TRANSLATE, scrollYOrX);
        }
    }

    /**
     * dragType==DRAG_TYPE_FIXED_BEHIND 变换回调
     *
     * @param orientation 布局方向
     * @param scrollYOrX  scrollY or scrollX
     */
    private void transformForFixedBehind(int orientation, int scrollYOrX) {
        boolean isVertical = isVertical();
        boolean isCallTransformHeader = isVertical ? getScrollY() <= 0 : getScrollX() <= 0;
        boolean isCallTransformFooter = isVertical ? getScrollY() >= 0 : getScrollX() >= 0;
        if (mHeaderView != null && isCallTransformHeader) {
            mPullExpandTransformer.transformHeader(this, orientation, DRAG_TYPE_FIXED_BEHIND, mHeaderView,
                    isVertical ? mHeaderHeight : mHeaderWidth, scrollYOrX);
        }
        if (mFooterView != null && isCallTransformFooter) {
            mPullExpandTransformer.transformFooter(this, orientation, DRAG_TYPE_FIXED_BEHIND, mFooterView,
                    isVertical ? mFooterHeight : mFooterWidth, scrollYOrX);
        }
        if (mContentView != null) {
            mPullExpandTransformer.transformContent(this, orientation, DRAG_TYPE_FIXED_BEHIND, mContentView,
                    isVertical ? getHeight() : getWidth(), scrollYOrX);
        }
    }

    /**
     * 普通的变换回调
     *
     * @param orientation 布局方向
     * @param dragType    拖拽类型
     * @param scrollYOrX  scrollY or scrollX
     */
    private void transformNormal(int orientation, int dragType, int scrollYOrX) {
        boolean isVertical = isVertical();
        if (mHeaderView != null) {
            mPullExpandTransformer.transformHeader(this, orientation, dragType, mHeaderView,
                    isVertical ? mHeaderHeight : mHeaderWidth, scrollYOrX);
        }
        if (mFooterView != null) {
            mPullExpandTransformer.transformFooter(this, orientation, dragType, mFooterView,
                    isVertical ? mFooterHeight : mFooterWidth, scrollYOrX);
        }
        if (mContentView != null) {
            mPullExpandTransformer.transformContent(this, orientation, dragType, mContentView,
                    isVertical ? getHeight() : getWidth(), scrollYOrX);
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            mIsComputeScrollCanCheckState = true;
            if (isVertical()) {
                scrollTo(0, mScroller.getCurrY());
            } else {
                scrollTo(mScroller.getCurrX(), 0);
            }
            onMovingAndStateCallback();
            doOnScrollAndDrag();
            mLastScrollY = getScrollY();
            mLastScrollX = getScrollX();
            ViewCompat.postInvalidateOnAnimation(this);
        }

        if (mIsComputeScrollCanCheckState && !mIsFingerTouched
                && mScroller.isFinished()) {
            checkFinalHeaderFooterState();
            if (mIsDebug) {
                Log.d(TAG, "computeScroll check state finish.header:"
                        + mIsHeaderExpanded + ",footer:" + mIsFooterExpanded);
            }
            mIsComputeScrollCanCheckState = false;
        }
    }

    /**
     * 通过计算 scrollY or scrollX 来判断滚动到最终态
     *
     * @param isAnimateScroll 是否动画滚动
     */
    private void computeScrollToState(boolean isAnimateScroll) {
        mIsInSelfControl = false;
        boolean isVertical = isVertical();
        int scrollYOrX = isVertical ? getScrollY() : getScrollX();
        int headerHeightOrWidth = isVertical ? mHeaderHeight : mHeaderWidth;
        int footerHeightOrWidth = isVertical ? mFooterHeight : mFooterWidth;
        //header完全展开了，并且向上推的高度>mHeaderShownThreshold 表示关闭
        if (mIsHeaderExpanded && scrollYOrX < 0
                && headerHeightOrWidth + scrollYOrX > mHeaderDragThreshold
                && Math.abs(scrollYOrX) < headerHeightOrWidth
                && mCurrentHeaderState == STATE_COLLAPSING) {
            closeHeaderOrFooter(isAnimateScroll);
            return;
        }
        //footer完全展开了，并且向下推的高度>mFooterShownThreshold 表示关闭
        if (mIsFooterExpanded && scrollYOrX > 0
                && footerHeightOrWidth - scrollYOrX > mFooterDragThreshold
                && scrollYOrX < footerHeightOrWidth
                && mCurrentFooterState == STATE_COLLAPSING) {
            closeHeaderOrFooter(isAnimateScroll);
            return;
        }

        if (-scrollYOrX > mHeaderDragThreshold) {
            openHeader(isAnimateScroll);
        } else if (scrollYOrX > mFooterDragThreshold) {
            openFooter(isAnimateScroll);
        } else {
            closeHeaderOrFooter(isAnimateScroll);
        }
    }

    /**
     * 检查当前Header Footer的状态
     */
    private void checkFinalHeaderFooterState() {
        //停止滚动确定一下状态
        int lastHeaderState = mCurrentHeaderState;
        int lastFooterState = mCurrentFooterState;
        int scrollYOrX = isVertical() ? getScrollY() : getScrollX();
        if (scrollYOrX > 0) {
            mIsHeaderExpanded = false;
            mIsFooterExpanded = true;
            mCurrentHeaderState = STATE_COLLAPSED;
            mCurrentFooterState = STATE_EXPANDED;
        } else if (scrollYOrX < 0) {
            mIsHeaderExpanded = true;
            mIsFooterExpanded = false;
            mCurrentHeaderState = STATE_EXPANDED;
            mCurrentFooterState = STATE_COLLAPSED;
        } else {
            mIsHeaderExpanded = false;
            mIsFooterExpanded = false;
            mCurrentHeaderState = STATE_COLLAPSED;
            mCurrentFooterState = STATE_COLLAPSED;
        }

        callHeaderAndFooterChangedListeners(false, -1);

        callHeaderAndFooterStateListeners(lastHeaderState, lastFooterState);
    }

    /**
     * 打开Header
     *
     * @param isAnimateScroll 是否动画滚动
     */
    private void openHeader(boolean isAnimateScroll) {
        if (isVertical()) {
            startScrollY(getScrollY(), -(getScrollY() + mHeaderHeight), isAnimateScroll);
        } else {
            startScrollX(getScrollX(), -(getScrollX() + mHeaderWidth), isAnimateScroll);
        }
    }

    /**
     * 关闭 Header
     *
     * @param isAnimateScroll 是否动画滚动
     */
    private void closeHeader(boolean isAnimateScroll) {
        closeHeaderOrFooter(isAnimateScroll);
    }

    /**
     * 打开 Footer
     *
     * @param isAnimateScroll 是否动画滚动
     */
    private void openFooter(boolean isAnimateScroll) {
        if (isVertical()) {
            startScrollY(getScrollY(), mFooterHeight - getScrollY(), isAnimateScroll);
        } else {
            startScrollX(getScrollX(), mFooterWidth - getScrollX(), isAnimateScroll);
        }
    }

    /**
     * 关闭Footer
     *
     * @param isAnimateScroll 是否动画滚动
     */
    private void closeFooter(boolean isAnimateScroll) {
        closeHeaderOrFooter(isAnimateScroll);
    }

    /**
     * 关闭 Header 或者 Footer
     *
     * @param isAnimateScroll 是否动画滚动
     */
    private void closeHeaderOrFooter(boolean isAnimateScroll) {
        if (isVertical()) {
            startScrollY(getScrollY(), -getScrollY(), isAnimateScroll);
        } else {
            startScrollX(getScrollX(), -getScrollX(), isAnimateScroll);
        }
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
        ViewCompat.postInvalidateOnAnimation(this);
    }

    /**
     * 通过Scroller滚动x方向
     *
     * @param startX          startX
     * @param dx              delta x
     * @param isAnimateScroll 是否执行动画滚动
     */
    private void startScrollX(int startX, int dx, boolean isAnimateScroll) {
        if (!mScroller.isFinished()) {
            mScroller.forceFinished(true);
        }
        mScroller.startScroll(startX, 0, dx, 0, isAnimateScroll ? 300 : 16);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    /**
     * 滑动时回调
     */
    private void onMovingAndStateCallback() {
        onMovingAndStateCallback(true);
    }

    /**
     * 滑动时回调
     *
     * @param isCallMoving 是否执行 xxMoving 回调
     */
    private void onMovingAndStateCallback(boolean isCallMoving) {
        int scrollYOrX = isVertical() ? getScrollY() : getScrollX();
        int absScrollY = Math.abs(scrollYOrX);
        boolean isExpanding = absScrollY > Math.abs(isVertical() ? mLastScrollY : mLastScrollX);
        boolean isCollapsing = absScrollY < Math.abs(isVertical() ? mLastScrollY : mLastScrollX);

        if (scrollYOrX < 0) {
            //操作Header
            int lastHeaderState = mCurrentHeaderState;
            if (isExpanding) {
                mCurrentHeaderState = STATE_EXPANDING;
            } else if (isCollapsing) {
                mCurrentHeaderState = STATE_COLLAPSING;
            }
            callHeaderChangedListeners(isCallMoving, absScrollY);
            callHeaderStateListeners(lastHeaderState);
        } else if (scrollYOrX > 0) {
            //操作Footer
            int lastFooterState = mCurrentFooterState;
            if (isExpanding) {
                mCurrentFooterState = STATE_EXPANDING;
            } else if (isCollapsing) {
                mCurrentFooterState = STATE_COLLAPSING;
            }
            callFooterChangedListeners(isCallMoving, absScrollY);
            callFooterStateListeners(lastFooterState);
        } else {
            //还原 Header Footer 都收起了
            int lastHeaderState = mCurrentHeaderState;
            int lastFooterState = mCurrentFooterState;
            mCurrentHeaderState = STATE_COLLAPSED;
            mCurrentFooterState = STATE_COLLAPSED;
            callHeaderAndFooterChangedListeners(isCallMoving, scrollYOrX);
            callHeaderAndFooterStateListeners(lastHeaderState, lastFooterState);
        }
    }

    /**
     * 执行 Header 状态变化回调
     *
     * @param isCallMoving  是否回调 onHeaderMoving 方法
     * @param absScrollYOrX scrollY 的绝对值
     */
    private void callHeaderChangedListeners(boolean isCallMoving, int absScrollYOrX) {
        callHeaderAndFooterChangedListeners(isCallMoving, absScrollYOrX, true,
                false);
    }

    /**
     * 执行 Footer 状态变化回调
     *
     * @param isCallMoving  是否回调 onFooterMoving 方法
     * @param absScrollYOrX scroll 绝对值
     */
    private void callFooterChangedListeners(boolean isCallMoving, int absScrollYOrX) {
        callHeaderAndFooterChangedListeners(isCallMoving, absScrollYOrX, false,
                true);
    }

    /**
     * 执行 Header Footer 状态变化回调
     *
     * @param isCallMoving 是否回调 onXxMoving 方法
     * @param scrollYOrX   scrollY or scrollX
     */
    private void callHeaderAndFooterChangedListeners(boolean isCallMoving, int scrollYOrX) {
        callHeaderAndFooterChangedListeners(isCallMoving, scrollYOrX, true,
                true);
    }

    /**
     * 执行 Header Footer 状态变化回调
     *
     * @param isCallMoving 是否回调 onXxMoving 方法
     * @param scrollYOrX   scrollY or scrollX
     * @param isCallHeader 是否回调 Header 方法
     * @param isCallFooter 是否回调 Footer 方法
     */
    private void callHeaderAndFooterChangedListeners(boolean isCallMoving, int scrollYOrX,
                                                     boolean isCallHeader, boolean isCallFooter) {
        int headerHeightOrWidth = isVertical() ? mHeaderHeight : mHeaderWidth;
        int footerHeightOrWidth = isVertical() ? mFooterHeight : mFooterWidth;
        if (mOnPullExpandChangedListeners != null) {
            for (OnPullExpandChangedListener onPullExpandChangedListener : mOnPullExpandChangedListeners) {
                if (isCallHeader && mHeaderView != null) {
                    if (isCallMoving) {
                        onPullExpandChangedListener.onHeaderMoving(mOrientation, Math.abs(scrollYOrX) * 1.0f / headerHeightOrWidth,
                                Math.abs(scrollYOrX), headerHeightOrWidth, mHeaderMaxDragDistance);
                    }
                    onPullExpandChangedListener.onHeaderStateChanged(this, mCurrentHeaderState);
                }
                if (isCallFooter && mFooterView != null) {
                    if (isCallMoving) {
                        onPullExpandChangedListener.onFooterMoving(mOrientation, Math.abs(scrollYOrX) * 1.0f / footerHeightOrWidth,
                                Math.abs(scrollYOrX), footerHeightOrWidth, mFooterMaxDragDistance);
                    }
                    onPullExpandChangedListener.onFooterStateChanged(this, mCurrentFooterState);
                }
            }
        }
    }

    /**
     * 执行 onRelease 回调
     */
    private void callReleaseChangedListeners() {
        if (mOnPullExpandChangedListeners != null) {
            for (OnPullExpandChangedListener onPullExpandChangedListener : mOnPullExpandChangedListeners) {
                onPullExpandChangedListener.onReleased(this,
                        isVertical() ? getScrollY() : getScrollX());
            }
        }
    }

    /**
     * 执行 Header 状态回调
     *
     * @param lastHeaderState 前一个 Header 的状态
     */
    private void callHeaderStateListeners(int lastHeaderState) {
        callHeaderAndFooterStateListeners(true, lastHeaderState, false, -1);
    }

    /**
     * 执行 Footer 状态回调
     *
     * @param lastFooterState 前一个 Footer 的状态
     */
    private void callFooterStateListeners(int lastFooterState) {
        callHeaderAndFooterStateListeners(false, lastFooterState, true, lastFooterState);
    }

    /**
     * 执行 Header Footer 状态回调
     *
     * @param lastHeaderState 前一个 Header 的状态
     * @param lastFooterState 前一个 Footer 的状态
     */
    private void callHeaderAndFooterStateListeners(int lastHeaderState, int lastFooterState) {
        callHeaderAndFooterStateListeners(true, lastHeaderState,
                true, lastFooterState);
    }

    /**
     * 执行 Header Footer 状态回调
     *
     * @param isCallHeader    是否回调 Header 状态方法
     * @param lastHeaderState 前一个 Header 的状态
     * @param isCallFooter    是否回调 Footer 状态方法
     * @param lastFooterState 前一个 Footer 的状态
     */
    private void callHeaderAndFooterStateListeners(boolean isCallHeader, int lastHeaderState,
                                                   boolean isCallFooter, int lastFooterState) {
        //只有不相等的时候才调用
        if (mOnPullExpandStateListeners != null) {
            for (OnPullExpandStateListener onPullExpandStateListener : mOnPullExpandStateListeners) {
                if (isCallHeader && mHeaderView != null && lastHeaderState != mCurrentHeaderState) {
                    onPullExpandStateListener.onHeaderStateChanged(this, mCurrentHeaderState);
                }
                if (isCallFooter && mFooterView != null && lastFooterState != mCurrentFooterState) {
                    onPullExpandStateListener.onFooterStateChanged(this, mCurrentFooterState);
                }
            }
        }
    }

    private boolean isVertical() {
        return mOrientation == VERTICAL;
    }

    /**
     * 获取 Header 高度
     *
     * @return Header 高度
     */
    public int getHeaderHeight() {
        return mHeaderHeight;
    }

    /**
     * 获取 Header 宽度
     *
     * @return Header 宽度
     */
    public int getHeaderWidth() {
        return mHeaderWidth;
    }

    /**
     * 获取 Footer 高度
     *
     * @return Footer 高度
     */
    public int getFooterHeight() {
        return mFooterHeight;
    }

    /**
     * 获取 Footer 宽度
     *
     * @return Footer 宽度
     */
    public int getFooterWidth() {
        return mFooterWidth;
    }

    /**
     * Header 是否展开
     *
     * @return Header 是否展开
     */
    public boolean isHeaderExpanded() {
        return mIsHeaderExpanded;
    }

    /**
     * Footer 是否展开
     *
     * @return Footer 是否展开
     */
    public boolean isFooterExpanded() {
        return mIsFooterExpanded;
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
     * 设置 Header 的 drag threshold rate
     *
     * @param headerDragThresholdRate Header 拖拽比率
     */
    public void setHeaderDragThresholdRate(@FloatRange(from = 0f, to = 1f) float headerDragThresholdRate) {
        mHeaderDragThresholdRate = getDragThresholdRateInRange(headerDragThresholdRate);
        if (mHeaderDragThresholdRate != 0) {
            mHeaderDragThreshold = (int) (isVertical() ? mHeaderHeight * mHeaderDragThresholdRate
                    : mHeaderWidth * mHeaderDragThresholdRate);
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
     * 设置 Footer 的 drag threshold rate
     *
     * @param footerDragThresholdRate footer 拖拽比率
     */
    public void setFooterDragThresholdRate(@FloatRange(from = 0f, to = 1f) float footerDragThresholdRate) {
        mFooterDragThresholdRate = getDragThresholdRateInRange(footerDragThresholdRate);
        if (mFooterDragThresholdRate != 0) {
            mFooterDragThreshold = (int) (isVertical() ? mFooterHeight * mFooterDragThresholdRate
                    : mFooterWidth * mFooterDragThresholdRate);
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
     * 获取 Header 最大拖动距离
     *
     * @return 最大拖动距离
     */
    public int getHeaderMaxDragDistance() {
        return mHeaderMaxDragDistance;
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
     * 获取 Footer 最大拖动距离
     *
     * @return 最大拖动距离
     */
    public int getFooterMaxDragDistance() {
        return mFooterMaxDragDistance;
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
     * 设置 PullExpandLayout 滑动时的转换器
     *
     * @param pullExpandTransformer transformer
     */
    public void setPullExpandTransformer(PullExpandTransformer pullExpandTransformer) {
        mPullExpandTransformer = pullExpandTransformer;
    }

    /**
     * 添加状态变化监听器
     *
     * @param listener OnPullExpandChangedListener
     */
    public void addOnPullExpandChangedListener(OnPullExpandChangedListener listener) {
        if (mOnPullExpandChangedListeners == null) {
            mOnPullExpandChangedListeners = new ArrayList<>();
        }
        if (listener != null && !mOnPullExpandChangedListeners.contains(listener)) {
            mOnPullExpandChangedListeners.add(listener);
        }
    }

    /**
     * 移除状态变化监听器
     *
     * @param listener OnPullExpandChangedListener
     */
    public void removePullExpandChangedListener(OnPullExpandChangedListener listener) {
        if (mOnPullExpandChangedListeners != null && listener != null) {
            mOnPullExpandChangedListeners.remove(listener);
        }
    }

    /**
     * 移除所有状态变化监听器
     */
    public void removeAllPullExpandChangedListeners() {
        if (mOnPullExpandChangedListeners != null) {
            mOnPullExpandChangedListeners.clear();
        }
    }

    /**
     * 添加状态监听器
     *
     * @param listener OnPullExpandStateListener
     */
    public void addOnPullExpandStateListener(OnPullExpandStateListener listener) {
        if (mOnPullExpandStateListeners == null) {
            mOnPullExpandStateListeners = new ArrayList<>();
        }
        if (listener != null && !mOnPullExpandStateListeners.contains(listener)) {
            mOnPullExpandStateListeners.add(listener);
        }
    }

    /**
     * 移除状态监听器
     *
     * @param listener OnPullExpandStateListener
     */
    public void removePullExpandStateListener(OnPullExpandStateListener listener) {
        if (mOnPullExpandStateListeners != null && listener != null) {
            mOnPullExpandStateListeners.remove(listener);
        }
    }

    /**
     * 移除所有状态监听器
     */
    public void removeAllPullExpandStateListeners() {
        if (mOnPullExpandStateListeners != null) {
            mOnPullExpandStateListeners.clear();
        }
    }

    /**
     * 获取当前 Header 状态
     *
     * @return 当前状态
     */
    public int getCurrentHeaderState() {
        return mCurrentHeaderState;
    }

    /**
     * 获取当前 Footer 的状态
     *
     * @return 当前状态
     */
    public int getCurrentFooterState() {
        return mCurrentFooterState;
    }

    /**
     * 设置debug模式
     *
     * @param debug 是否开启debug日志打印
     */
    public void setDebug(boolean debug) {
        mIsDebug = debug;
    }

    /**
     * 设置 Header 打开/关闭
     *
     * @param isExpand 是否展开
     */
    public void setHeaderExpanded(boolean isExpand) {
        setHeaderExpanded(isExpand, true);
    }

    /**
     * 设置 Header 打开/关闭
     *
     * @param isExpand 是否展开
     * @param isAnim   是否伴随动画
     */
    public void setHeaderExpanded(boolean isExpand, boolean isAnim) {
        if (isExpand) {
            openHeader(isAnim);
        } else {
            closeHeader(isAnim);
        }
    }

    /**
     * 设置 Footer 打开/关闭
     *
     * @param isExpand 是否展开
     */
    public void setFooterExpanded(boolean isExpand) {
        setFooterExpanded(isExpand, true);
    }

    /**
     * 设置 Footer 打开/关闭
     *
     * @param isExpand 是否展开
     * @param isAnim   是否伴随动画
     */
    public void setFooterExpanded(boolean isExpand, boolean isAnim) {
        if (isExpand) {
            openFooter(isAnim);
        } else {
            closeFooter(isAnim);
        }
    }

    @IntDef({DRAG_TYPE_TRANSLATE, DRAG_TYPE_FIXED_BEHIND, DRAG_TYPE_FIXED_FOREGROUND})
    @Retention(RetentionPolicy.SOURCE)
    private @interface DragType {
    }
}
