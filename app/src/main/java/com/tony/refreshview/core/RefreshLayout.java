package com.tony.refreshview.core;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;
import android.widget.TextView;

import com.tony.refreshview.R;
import com.tony.refreshview.indicator.RefreshIndicator;

/**
 * 通常情况下 Header 会实现 UIRefresh 接口， Content 可以为任意的 View。
 */
public class RefreshLayout extends ViewGroup {
    //四种刷新状态
    public final static byte REFRESH_STATUS_INIT = 1;
    public final static byte REFRESH_STATUS_PREPARE = 2;
    public final static byte REFRESH_STATUS_LOADING = 3;
    public final static byte REFRESH_STATUS_COMPLETE = 4;
    private byte mStatus = REFRESH_STATUS_INIT;

    //刷新状态记录指示器
    private RefreshIndicator mRefreshIndicator;
    //两个view
    private View mHeaderView, mContentView;

    private boolean isPinContent = false;
    private boolean isEnableContinueRefresh = false;
    private boolean isAutoRefresh = false;
    private boolean isAutoRefreshButLater = false;

    //xml
    private int mHeaderId = 0;//设置头部id
    private int mContentId = 0;//设置内容id
    private int mDurationToClose = 200;//回弹延时，默认200ms，回弹到刷新高度所用时间。
    private int mDurationToCloseHeader = 500;//头部回弹时间，默认 800ms。
    private boolean mKeepHeaderWhenRefresh = true;//刷新是否保持头部，默认值 true。
    private boolean mPullToRefresh = false;//下拉刷新 / 释放刷新，默认为释放刷新。

    // working parameters
    private ScrollWrapper mScrollWrapper;
    private int mPagingTouchSlop;
    private int mHeaderHeight;

    private long mLoadingStartTime = 0;

    private UIRefreshHook mRefreshCompleteHook;
    private UIRefreshHolder mPtrUIHandlerHolder = UIRefreshHolder.create();
    private Refresh mRefresh;
    private boolean mHasSendCancelEvent = false;
    private MotionEvent mLastMoveEvent;
    // disable when detect moving horizontally
    private boolean mPreventForHorizontal = false; //防止水平滚动
    private boolean mDisableHorizontalMove = false;//禁用水平移动
    private int mLoadingMinTime = 500;
    private Runnable mPerformRefreshCompleteDelay = new Runnable() {
        @Override
        public void run() {
            performRefreshComplete();
        }
    };

    public RefreshLayout(Context context) {
        this(context, null);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        mPagingTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop() * 2;
        mRefreshIndicator = new RefreshIndicator();
        mScrollWrapper = new ScrollWrapper();
        //获取xml属性
        TypedArray typeArray = context.obtainStyledAttributes(attrs, R.styleable.RefreshLayout, 0, 0);
        if (typeArray != null) {
            mHeaderId = typeArray.getResourceId(R.styleable.RefreshLayout_header, mHeaderId);
            mContentId = typeArray.getResourceId(R.styleable.RefreshLayout_content, mContentId);
            mRefreshIndicator.setResistance(typeArray.getFloat(R.styleable.RefreshLayout_resistance, mRefreshIndicator.getResistance()));
            mDurationToClose = typeArray.getInt(R.styleable.RefreshLayout_duration_to_close, mDurationToClose);
            mDurationToCloseHeader = typeArray.getInt(R.styleable.RefreshLayout_duration_to_close_header, mDurationToCloseHeader);
            float ratio = typeArray.getFloat(R.styleable.RefreshLayout_ratio_of_header_height_to_refresh, mRefreshIndicator.getRatioOfHeaderToHeightRefresh());
            mRefreshIndicator.setRatioOfHeaderHeightToRefresh(ratio);
            mKeepHeaderWhenRefresh = typeArray.getBoolean(R.styleable.RefreshLayout_keep_header_when_refresh, mKeepHeaderWhenRefresh);
            mPullToRefresh = typeArray.getBoolean(R.styleable.RefreshLayout_pull_to_fresh, mPullToRefresh);
            typeArray.recycle();
        }
    }


    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p != null && p instanceof MarginLayoutParams;
    }

    /**
     * 重写 onFinishInflate 方法来确定 Header 和 Content
     */
    @Override
    protected void onFinishInflate() {
        final int childCount = getChildCount();
        if (childCount > 2) {
            throw new IllegalStateException("RefreshLayout can only contains two children");
        } else if (childCount == 2) {
            if (mHeaderId != 0 && mHeaderView == null) {
                mHeaderView = findViewById(mHeaderId);
            }
            if (mContentId != 0 && mContentView == null) {
                mContentView = findViewById(mContentId);
            }
            if (mHeaderView == null || mContentView == null) {
                View child1 = getChildAt(0);
                View child2 = getChildAt(1);
                //header优先
                if (child1 instanceof UIRefresh) {
                    mHeaderView = child1;
                    mContentView = child2;
                } else if (child2 instanceof UIRefresh) {
                    mHeaderView = child2;
                    mContentView = child1;
                } else {
                    // both are not specified
                    if (mHeaderView == null && mContentView == null) {
                        mHeaderView = child1;
                        mContentView = child2;
                    } else {// only one is specified
                        if (mHeaderView == null) {
                            mHeaderView = mContentView == child1 ? child2 : child1;
                        } else {
                            mContentView = mHeaderView == child1 ? child2 : child1;
                        }
                    }
                }
            }
        } else if (childCount == 1) {
            mContentView = getChildAt(0);
        } else {
            TextView errorView = new TextView(getContext());
            errorView.setClickable(true);
            errorView.setTextColor(0xffff6600);
            errorView.setGravity(Gravity.CENTER);
            errorView.setTextSize(20);
            errorView.setText("The content view in PtrFrameLayout is empty. Do you forget to specify its id in xml layout file?");
            mContentView = errorView;
            addView(mContentView);
        }
        if (mHeaderView != null) {
            //置于顶端
            mHeaderView.bringToFront();
        }
        super.onFinishInflate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mHeaderView != null) {
            //该方法是一个调用子view　onMeasure的过度层
            measureChildWithMargins(mHeaderView, widthMeasureSpec, 0, heightMeasureSpec, 0);
            MarginLayoutParams lp = (MarginLayoutParams) mHeaderView.getLayoutParams();
            mHeaderHeight = mHeaderView.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            //保存header height
            mRefreshIndicator.setHeaderHeight(mHeaderHeight);
        }

        if (mContentView != null) {
            final MarginLayoutParams lp = (MarginLayoutParams) mContentView.getLayoutParams();
            //RefreshLayout的padding.
            final int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin, lp.width);
            final int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom() + lp.topMargin, lp.height);
            mContentView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        //contentView竖直移动的距离　 这个值随着后期用户拖动contentView而一直变化.但是初始状态为0. 所以初始状态时,-mHeaderHeight会让headerView初始时向上偏移到刚好看不见.
        int offsetY = mRefreshIndicator.getCurrentPosY();
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        if (mHeaderView != null) {
            MarginLayoutParams lp = (MarginLayoutParams) mHeaderView.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            // enhance readability(header is layout above screen when first init)
            //-mHeaderHeight会让headerView初始时向上偏移到刚好看不见
            final int top = paddingTop + lp.topMargin + offsetY - mHeaderHeight;
            final int right = left + mHeaderView.getMeasuredWidth();
            final int bottom = top + mHeaderView.getMeasuredHeight();
            mHeaderView.layout(left, top, right, bottom);
        }
        //没有减去mHeaderView,所以初始状态值看得到一个contentView. 并且headerView始终都是在contentView上面的.就和竖直方向的一个线性布局似的.
        //因为控制view在viewGroup的左上右下下个坐标,headerView和contentView只在top上坐标有差异.
        if (mContentView != null) {
            if (isPinContent()) {//固定Content
                offsetY = 0;
            }
            MarginLayoutParams lp = (MarginLayoutParams) mContentView.getLayoutParams();
            final int left = paddingLeft + lp.leftMargin;
            final int top = paddingTop + lp.topMargin + offsetY;
            final int right = left + mContentView.getMeasuredWidth();
            final int bottom = top + mContentView.getMeasuredHeight();
            mContentView.layout(left, top, right, bottom);
        }
    }

    /**
     * 核心
     *
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!isEnabled() || mHeaderView == null || mContentView == null) {
            return super.dispatchTouchEvent(ev);
        }
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN://手指按下　暂定所有正在进行的动作　等待手指操作
                mRefreshIndicator.onTouchDown(ev.getX(), ev.getY());//位置指示器记录位置信息
                mScrollWrapper.abortIfWorking();//滚动器强制暂停
                mHasSendCancelEvent = false;//是否已经发送取消事件
                mPreventForHorizontal = false;//不禁止水平滚动
                super.dispatchTouchEvent(ev);
                return true;
            case MotionEvent.ACTION_MOVE:
                mRefreshIndicator.onTouchMove(ev.getX(), ev.getY());//位置指示器记录位置信息
                mLastMoveEvent = ev;//记录当前动作　防止以后获取上次的事件
                float offsetX = mRefreshIndicator.getOffsetX();//获取偏移量
                float offsetY = mRefreshIndicator.getOffsetY();

                //方向判定
                if (mDisableHorizontalMove && !mPreventForHorizontal && (Math.abs(offsetX) > mPagingTouchSlop && Math.abs(offsetX) > Math.abs(offsetY))) {
                    if (mRefreshIndicator.isInStartPosition()) {
                        mPreventForHorizontal = true;
                    }
                }
                if (mPreventForHorizontal) {
                    return super.dispatchTouchEvent(ev);
                }
                //手指往下移动
                boolean moveDown = offsetY > 0;
                boolean moveUp = !moveDown;
                boolean canMoveUp = mRefreshIndicator.hasLeftStartPosition();//头部是否有移动

                if (mRefresh != null) {
                    if (mRefresh.canRefresh(this, mHeaderView, mContentView)) {//到头了
                        if (moveDown || (moveUp && canMoveUp)) {
                            movePos(offsetY);//可以移动到制定位置
                            return true;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL://手放开
                mRefreshIndicator.onTouchUp();//指示器设置没有动作
                if (mRefreshIndicator.hasLeftStartPosition()) {//头部是否有移动
                    onRelease(false);
                    if (mRefreshIndicator.hasMovedAfterPressedDown()) {// TODO: 6/25/16
                        sendCancelEvent();
                        return true;
                    }
                }
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }


    /**
     * please DO REMEMBER resume the hook
     *
     * @param hook
     */

    public void setRefreshCompleteHook(UIRefreshHook hook) {
        mRefreshCompleteHook = hook;
        hook.setResumeAction(new Runnable() {
            @Override
            public void run() {
                notifyUIRefreshComplete(true);
            }
        });
    }

    /**
     * 释放头部
     *
     * @param stayForLoading 是否保持当前位置加载
     */
    private void onRelease(boolean stayForLoading) {
        tryToPerformRefresh();
        if (mStatus == REFRESH_STATUS_LOADING) {
            // keep header for fresh
            if (mKeepHeaderWhenRefresh) {
                // scroll header back
                if (mRefreshIndicator.isOverOffsetToKeepHeaderWhileLoading() && !stayForLoading) {
                    mScrollWrapper.tryToScrollTo(mRefreshIndicator.getOffsetToKeepHeaderWhileLoading(), mDurationToClose);
                } else {
                    // do nothing
                }
            } else {
                scrollBackToTop();
            }
        } else {
            if (mStatus == REFRESH_STATUS_COMPLETE) {
                notifyUIRefreshComplete(false);
            } else {
                scrollBackToTop();
            }
        }
    }

    //尝试执行刷新　需要判断
    private boolean tryToPerformRefresh() {
        if (mStatus != REFRESH_STATUS_PREPARE) {
            return false;
        }

        //
        if ((mRefreshIndicator.isOverOffsetToKeepHeaderWhileLoading() && isAutoRefresh()) || mRefreshIndicator.isOverOffsetToRefresh()) {
            mStatus = REFRESH_STATUS_LOADING;
            performRefresh();
        }
        return false;
    }

    /**
     * 开始执行刷新
     */
    private void performRefresh() {
        mLoadingStartTime = System.currentTimeMillis();
        if (mPtrUIHandlerHolder.hasHandler()) {
            mPtrUIHandlerHolder.onUIRefreshBegin(this);
        }
        if (mRefresh != null) {
            mRefresh.onRefreshBegin(this);
        }
    }

    public boolean isRefreshing() {
        return mStatus == REFRESH_STATUS_LOADING;
    }

    //默认状态　自动调用刷新
    public boolean isAutoRefresh() {
        return this.isAutoRefresh;
    }

    /**
     * 滚回到顶部　没有触摸事件
     */
    private void scrollBackToTop() {
        if (!mRefreshIndicator.isTouch()) {
            mScrollWrapper.tryToScrollTo(RefreshIndicator.POS_START, mDurationToCloseHeader);
        }
    }

    /**
     * Do real refresh work. If there is a hook, execute the hook first.
     *
     * @param ignoreHook
     */
    private void notifyUIRefreshComplete(boolean ignoreHook) {
        /**
         * After hook operation is done, {@link #notifyUIRefreshComplete} will be call in resume action to ignore hook.
         */
        if (mRefreshIndicator.hasLeftStartPosition() && !ignoreHook && mRefreshCompleteHook != null) {
            mRefreshCompleteHook.takeOver();
            return;
        }
        if (mPtrUIHandlerHolder.hasHandler()) {
            mPtrUIHandlerHolder.onUIRefreshComplete(this);
        }
        mRefreshIndicator.onUIRefreshComplete();
        scrollBackToTop();
        notifyReset();
    }

    /**
     * If at the top and not in loading, reset
     */
    private boolean notifyReset() {
        if ((mStatus == REFRESH_STATUS_COMPLETE || mStatus == REFRESH_STATUS_PREPARE) && mRefreshIndicator.isInStartPosition()) {
            if (mPtrUIHandlerHolder.hasHandler()) {
                mPtrUIHandlerHolder.onUIReset(this);
            }
            mStatus = REFRESH_STATUS_INIT;
            clearFlag();
            return true;
        }
        return false;
    }

    private void clearFlag() {
        // remove auto fresh flag
        isAutoRefresh = false;
    }

    /**
     * if deltaY > 0, move the content down
     *
     * @param deltaY
     */
    private void movePos(float deltaY) {
        // has reached the top
        if ((deltaY < 0 && mRefreshIndicator.isInStartPosition())) {
            return;
        }

        //需要达到的点
        int to = mRefreshIndicator.getCurrentPosY() + (int) deltaY;

        // over top
        if (mRefreshIndicator.willOverTop(to)) {
            //如果到达开始点　则不再移动
            to = RefreshIndicator.POS_START;
        }

        //重新更新当前点
        mRefreshIndicator.setCurrentPos(to);
        //获取改变的距离
        int change = to - mRefreshIndicator.getLastPosY();
        //移动
        //没有移动
        if (change == 0) {
            return;
        }

        boolean isTouch = mRefreshIndicator.isTouch();

        // once moved, cancel event will be sent to child
        if (isTouch && !mHasSendCancelEvent && mRefreshIndicator.hasMovedAfterPressedDown()) {
            mHasSendCancelEvent = true;
            sendCancelEvent();
        }

        // leave initiated position or just refresh complete
        if ((mRefreshIndicator.hasJustLeftStartPosition() && mStatus == REFRESH_STATUS_INIT) || (mRefreshIndicator.goDownCrossFinishPosition() && mStatus == REFRESH_STATUS_COMPLETE && isEnableContinueRefresh())) {
            //预备状态
            mStatus = REFRESH_STATUS_PREPARE;
            mPtrUIHandlerHolder.onUIRefreshPrepare(this);
        }

        // back to initiated position
        if (mRefreshIndicator.hasJustBackToStartPosition()) {
            //回到开始点　重置
            notifyReset();

            // recover event to children
            if (isTouch) {
                sendDownEvent();
            }
        }

        // Pull to Refresh
        if (mStatus == REFRESH_STATUS_PREPARE) {
            // reach fresh height while moving from top to bottom
            if (isTouch && !isAutoRefresh() && mPullToRefresh && mRefreshIndicator.crossRefreshLineFromTopToBottom()) {
                tryToPerformRefresh();
            }
            // reach header height while auto refresh
            if (performAutoRefreshButLater() && mRefreshIndicator.hasJustReachedHeaderHeightFromTopToBottom()) {
                tryToPerformRefresh();
            }
        }

        mHeaderView.offsetTopAndBottom(change);
        if (!isPinContent()) {
            mContentView.offsetTopAndBottom(change);
        }
        invalidate();

        if (mPtrUIHandlerHolder.hasHandler()) {
            mPtrUIHandlerHolder.onUIPositionChange(this, isTouch, mStatus, mRefreshIndicator);
        }
        onPositionChange(isTouch, mStatus, mRefreshIndicator);
    }

    protected void onPositionChange(boolean isInTouching, byte status, RefreshIndicator mRefreshIndicator) {
    }

    private boolean performAutoRefreshButLater() {
        return this.isAutoRefreshButLater;
    }

    //改变事件给内容
    private void sendDownEvent() {
        final MotionEvent last = mLastMoveEvent;
        MotionEvent e = MotionEvent.obtain(last.getDownTime(), last.getEventTime(), MotionEvent.ACTION_DOWN, last.getX(), last.getY(), last.getMetaState());
        super.dispatchTouchEvent(e);
    }

    //将释放之后的事件交由系统处理
    private void sendCancelEvent() {
        // The ScrollChecker will update position and lead to send cancel event when mLastMoveEvent is null.
        // fix #104, #80, #92
        if (mLastMoveEvent == null) {
            return;
        }
        MotionEvent last = mLastMoveEvent;
        MotionEvent e = MotionEvent.obtain(last.getDownTime(), last.getEventTime() + ViewConfiguration.getLongPressTimeout(), MotionEvent.ACTION_CANCEL, last.getX(), last.getY(), last.getMetaState());
        super.dispatchTouchEvent(e);
    }

    class ScrollWrapper implements Runnable {
        private Scroller mScroller;
        private int mLastFlingY;
        private boolean mIsRunning = false;
        private int mStart;
        private int mTo;

        public ScrollWrapper() {
            mScroller = new Scroller(getContext());
        }

        @Override
        public void run() {
            //滚动是否完成
            boolean finish = !mScroller.computeScrollOffset() || mScroller.isFinished();
            int curY = mScroller.getCurrY();
            //滚动的偏移量
            int deltaY = curY - mLastFlingY;
            if (!finish) {
                //滚动未完成
                mLastFlingY = curY;
                movePos(deltaY);
                post(this);
            } else {
                finish();
            }
        }

        private void finish() {
            reset();
            onPtrScrollFinish();
        }

        //重置Scroller
        private void reset() {
            mIsRunning = false;
            mLastFlingY = 0;
            removeCallbacks(this);
        }

        private void destroy() {
            reset();
            if (!mScroller.isFinished()) {
                mScroller.forceFinished(true);
            }
        }

        //中断操作
        public void abortIfWorking() {
            if (mIsRunning) {
                if (!mScroller.isFinished()) {
                    mScroller.forceFinished(true);
                }
                onPtrScrollAbort();
                reset();
            }
        }

        //滚动到指定距离
        public void tryToScrollTo(int to, int duration) {
            //已经在这个位置
            if (mRefreshIndicator.isAlreadyHere(to)) {
                return;
            }
            mStart = mRefreshIndicator.getCurrentPosY();
            mTo = to;
            int distance = to - mStart;
            removeCallbacks(this);

            mLastFlingY = 0;
            if (!mScroller.isFinished()) {
                mScroller.forceFinished(true);
            }
            mScroller.startScroll(0, 0, 0, distance, duration);
            post(this);
            mIsRunning = true;
        }

    }

    protected void onPtrScrollAbort() {
        if (mRefreshIndicator.hasLeftStartPosition() && isAutoRefresh()) {
            onRelease(true);
        }
    }

    /**
     * Call this when data is loaded.
     * The UI will perform complete at once or after a delay, depends on the time elapsed is greater then {@link #mLoadingMinTime} or not.
     */
    final public void refreshComplete() {

        if (mRefreshCompleteHook != null) {
            mRefreshCompleteHook.reset();
        }

        int delay = (int) (mLoadingMinTime - (System.currentTimeMillis() - mLoadingStartTime));
        if (delay <= 0) {
            performRefreshComplete();
        } else {
            postDelayed(mPerformRefreshCompleteDelay, delay);
        }
    }

    /**
     * Do refresh complete work when time elapsed is greater than {@link #mLoadingMinTime}
     */
    private void performRefreshComplete() {
        mStatus = REFRESH_STATUS_COMPLETE;

        // if is auto refresh do nothing, wait scroller stop
        if (mScrollWrapper.mIsRunning && isAutoRefresh()) {
            // do nothing
            return;
        }
        notifyUIRefreshComplete(false);
    }

    protected void onPtrScrollFinish() {
        if (mRefreshIndicator.hasLeftStartPosition() && isAutoRefresh()) {
            onRelease(true);
        }
    }


    //立即刷新
    public void autoRefresh() {
        autoRefresh(true, mDurationToCloseHeader);
    }

    public void autoRefresh(boolean atOnce) {
        autoRefresh(atOnce, mDurationToCloseHeader);
    }

    public void autoRefresh(boolean atOnce, int duration) {
        if (mStatus != REFRESH_STATUS_INIT) {
            return;
        }
        mStatus = REFRESH_STATUS_PREPARE;
        if (mPtrUIHandlerHolder.hasHandler()) {
            mPtrUIHandlerHolder.onUIRefreshPrepare(this);
        }
        mScrollWrapper.tryToScrollTo(mRefreshIndicator.getOffsetToRefresh(), duration);
        if (atOnce) {
            mStatus = REFRESH_STATUS_LOADING;
            performRefresh();
        }
    }


    /**
     * If @param enable has been set to true. The user can perform next PTR at once.
     *  不需要返回顶部就可以重新刷新
     * @param enable
     */
    public void enableContinueRefresh(boolean enable) {
        this.isEnableContinueRefresh = enable;
    }

    public boolean isEnableContinueRefresh() {
        return this.isEnableContinueRefresh;
    }

    /**
     * The content view will now move when {@param pinContent} set to true.
     *
     * @param pinContent
     */
    public void setPinContent(boolean pinContent) {
        this.isPinContent = pinContent;
    }

    //固定content　view
    public boolean isPinContent() {
        return this.isPinContent;
    }

    /**
     * It's useful when working with viewpager.
     *
     * @param disable
     */
    public void disableWhenHorizontalMove(boolean disable) {
        mDisableHorizontalMove = disable;
    }

    /**
     * loading will last at least for so long
     *
     * @param time
     */
    public void setLoadingMinTime(int time) {
        mLoadingMinTime = time;
    }

    /**
     * Not necessary any longer. Once moved, cancel event will be sent to child.
     *
     * @param yes
     */
    @Deprecated
    public void setInterceptEventWhileWorking(boolean yes) {
    }

    @SuppressWarnings({"unused"})
    public View getContentView() {
        return mContentView;
    }

    public void setPtrHandler(Refresh ptrHandler) {
        mRefresh = ptrHandler;
    }

    public void addPtrUIHandler(UIRefresh ptrUIHandler) {
        UIRefreshHolder.addHandler(mPtrUIHandlerHolder, ptrUIHandler);
    }

    @SuppressWarnings({"unused"})
    public void removePtrUIHandler(UIRefresh ptrUIHandler) {
        mPtrUIHandlerHolder = UIRefreshHolder.removeHandler(mPtrUIHandlerHolder, ptrUIHandler);
    }

    public void setPtrIndicator(RefreshIndicator slider) {
        if (mRefreshIndicator != null && mRefreshIndicator != slider) {
            slider.convertFrom(mRefreshIndicator);
        }
        mRefreshIndicator = slider;
    }

    @SuppressWarnings({"unused"})
    public float getResistance() {
        return mRefreshIndicator.getResistance();
    }

    public void setResistance(float resistance) {
        mRefreshIndicator.setResistance(resistance);
    }

    @SuppressWarnings({"unused"})
    public float getDurationToClose() {
        return mDurationToClose;
    }

    @SuppressWarnings({"unused"})
    public long getDurationToCloseHeader() {
        return mDurationToCloseHeader;
    }

    /**
     * The duration to close time
     *
     * @param duration
     */
    public void setDurationToCloseHeader(int duration) {
        mDurationToCloseHeader = duration;
    }

    public void setRatioOfHeaderHeightToRefresh(float ratio) {
        mRefreshIndicator.setRatioOfHeaderHeightToRefresh(ratio);
    }

    public int getOffsetToRefresh() {
        return mRefreshIndicator.getOffsetToRefresh();
    }

    @SuppressWarnings({"unused"})
    public void setOffsetToRefresh(int offset) {
        mRefreshIndicator.setOffsetToRefresh(offset);
    }

    @SuppressWarnings({"unused"})
    public float getRatioOfHeaderToHeightRefresh() {
        return mRefreshIndicator.getRatioOfHeaderToHeightRefresh();
    }

    @SuppressWarnings({"unused"})
    public int getOffsetToKeepHeaderWhileLoading() {
        return mRefreshIndicator.getOffsetToKeepHeaderWhileLoading();
    }

    @SuppressWarnings({"unused"})
    public void setOffsetToKeepHeaderWhileLoading(int offset) {
        mRefreshIndicator.setOffsetToKeepHeaderWhileLoading(offset);
    }

    @SuppressWarnings({"unused"})
    public boolean isKeepHeaderWhenRefresh() {
        return mKeepHeaderWhenRefresh;
    }

    public void setKeepHeaderWhenRefresh(boolean keepOrNot) {
        mKeepHeaderWhenRefresh = keepOrNot;
    }

    public boolean isPullToRefresh() {
        return mPullToRefresh;
    }

    public void setPullToRefresh(boolean pullToRefresh) {
        mPullToRefresh = pullToRefresh;
    }

    public int getHeaderHeight() {
        return mHeaderHeight;
    }

    @SuppressWarnings({"unused"})
    public View getHeaderView() {
        return mHeaderView;
    }

    public void setHeaderView(View header) {
        if (mHeaderView != null && header != null && mHeaderView != header) {
            removeView(mHeaderView);
        }
        ViewGroup.LayoutParams lp = header.getLayoutParams();
        if (lp == null) {
            lp = new MarginLayoutParams(-1, -2);
            header.setLayoutParams(lp);
        }
        mHeaderView = header;
        addView(header);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mScrollWrapper != null) {
            mScrollWrapper.destroy();
        }
        if (mPerformRefreshCompleteDelay != null) {
            removeCallbacks(mPerformRefreshCompleteDelay);
        }
        super.onDetachedFromWindow();
    }
}
