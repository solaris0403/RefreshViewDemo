package com.tony.refreshview.indicator;

import android.graphics.PointF;

/**
 * 刷新的指示器　记录操作数据
 */
public class RefreshIndicator {
    public final static int POS_START = 0;//开始位置
    private PointF mPtLastMove = new PointF();//最近移动的距离
    private float mOffsetX;
    private float mOffsetY;
    private int mCurrentPos = 0;//当前位置
    private int mLastPos = 0;//上一次位置
    private int mHeaderHeight;//头的高度
    private int mPressedPos = 0;//按下的位置

    private float mRatioOfHeaderHeightToRefresh = 1.2f;//
    private float mResistance = 1.7f;//阻尼系数
    private boolean mIsTouch = false;//是不是在触摸状态
    protected int mOffsetToRefresh = 0;//达到刷新的偏移量
    private int mOffsetToKeepHeaderWhileLoading = -1;//达到刷新的偏移量
    // record the refresh complete position
    private int mRefreshCompleteY = 0;//刷新完成的点　可以开始下一次刷新　通常跟POS_START一样

    public boolean isTouch() {
        return this.mIsTouch;
    }
    public void onTouchUp() {
        this.mIsTouch = false;
    }

    public float getResistance() {
        return this.mResistance;
    }

    public void setResistance(float resistance) {
        this.mResistance = resistance;
    }

    public void onUIRefreshComplete() {
        mRefreshCompleteY = mCurrentPos;
    }

    public boolean goDownCrossFinishPosition() {
        return mCurrentPos >= mRefreshCompleteY;
    }

    public void setRatioOfHeaderHeightToRefresh(float ratio) {
        mRatioOfHeaderHeightToRefresh = ratio;
        mOffsetToRefresh = (int) (mHeaderHeight * ratio);
    }

    public float getRatioOfHeaderToHeightRefresh() {
        return mRatioOfHeaderHeightToRefresh;
    }

    public int getOffsetToRefresh() {
        return mOffsetToRefresh;
    }

    public void setOffsetToRefresh(int offset) {
        mRatioOfHeaderHeightToRefresh = mHeaderHeight * 1f / offset;
        mOffsetToRefresh = offset;
    }

    public void onTouchDown(float x, float y) {
        mIsTouch = true;
        mPressedPos = mCurrentPos;
        mPtLastMove.set(x, y);
    }

    public final void onTouchMove(float x, float y) {
        float offsetX = x - mPtLastMove.x;
        float offsetY = (y - mPtLastMove.y);
        setOffset(offsetX, offsetY / mResistance);
        mPtLastMove.set(x, y);
    }

    //记录每次move导致视图的偏移量
    protected void setOffset(float x, float y) {
        mOffsetX = x;
        mOffsetY = y;
    }

    public float getOffsetX() {
        return mOffsetX;
    }

    public float getOffsetY() {
        return mOffsetY;
    }

    public int getLastPosY() {
        return mLastPos;
    }

    public int getCurrentPosY() {
        return mCurrentPos;
    }

    /**
     * Update current position before update the UI
     */
    public final void setCurrentPos(int current) {
        mLastPos = mCurrentPos;
        mCurrentPos = current;
        onUpdatePos(current, mLastPos);
    }

    protected void onUpdatePos(int current, int last) {

    }

    public int getHeaderHeight() {
        return mHeaderHeight;
    }

    public void setHeaderHeight(int height) {
        mHeaderHeight = height;
        updateHeight();
    }

    protected void updateHeight() {
        mOffsetToRefresh = (int) (mRatioOfHeaderHeightToRefresh * mHeaderHeight);
    }

    public void convertFrom(RefreshIndicator ptrSlider) {
        mCurrentPos = ptrSlider.mCurrentPos;
        mLastPos = ptrSlider.mLastPos;
        mHeaderHeight = ptrSlider.mHeaderHeight;
    }

    //有移动
    public boolean hasLeftStartPosition() {
        return mCurrentPos > POS_START;
    }

    //刚离开起始点
    public boolean hasJustLeftStartPosition() {
        return mLastPos == POS_START && hasLeftStartPosition();
    }

    public boolean hasJustBackToStartPosition() {
        return mLastPos != POS_START && isInStartPosition();
    }

    public boolean isOverOffsetToRefresh() {
        return mCurrentPos >= getOffsetToRefresh();
    }

    //是否有过移动
    public boolean hasMovedAfterPressedDown() {
        return mCurrentPos != mPressedPos;
    }

    //是不是在开始的位置
    public boolean isInStartPosition() {
        return mCurrentPos == POS_START;
    }

    //当前位置刚过刷新线
    public boolean crossRefreshLineFromTopToBottom() {
        return mLastPos < getOffsetToRefresh() && mCurrentPos >= getOffsetToRefresh();
    }

    //当前位置刚过头的高度
    public boolean hasJustReachedHeaderHeightFromTopToBottom() {
        return mLastPos < mHeaderHeight && mCurrentPos >= mHeaderHeight;
    }

    //是否过了需要加载的距离
    public boolean isOverOffsetToKeepHeaderWhileLoading() {
        return mCurrentPos > getOffsetToKeepHeaderWhileLoading();
    }

    //刷新时候　头部的偏移量
    public void setOffsetToKeepHeaderWhileLoading(int offset) {
        mOffsetToKeepHeaderWhileLoading = offset;
    }

    public int getOffsetToKeepHeaderWhileLoading() {
        return mOffsetToKeepHeaderWhileLoading >= 0 ? mOffsetToKeepHeaderWhileLoading : mHeaderHeight;
    }

    public boolean isAlreadyHere(int to) {
        return mCurrentPos == to;
    }

    //上一次的位置所在头的百分比
    public float getLastPercent() {
        final float oldPercent = mHeaderHeight == 0 ? 0 : mLastPos * 1f / mHeaderHeight;
        return oldPercent;
    }

    //上一次的位置所在头的百分比
    public float getCurrentPercent() {
        final float currentPercent = mHeaderHeight == 0 ? 0 : mCurrentPos * 1f / mHeaderHeight;
        return currentPercent;
    }

    public boolean willOverTop(int to) {
        return to < POS_START;
    }
}
