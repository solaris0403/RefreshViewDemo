package com.tony.refreshview.core;

import com.tony.refreshview.indicator.RefreshIndicator;

/**
 * 刷新的ui接口
 */
public interface UIRefresh {
    /**
     * Content 重新回到顶部， Header 消失，整个下拉刷新过程完全结束以后，重置 View。
     *
     * @param refreshLayout
     */
    void onUIReset(RefreshLayout refreshLayout);

    /**
     * 准备刷新，Header 将要出现时调用。
     *
     * @param refreshLayout
     */
    void onUIRefreshPrepare(RefreshLayout refreshLayout);

    /**
     * 开始刷新，Header 进入刷新状态之前调用。
     *
     * @param refreshLayout
     */
    void onUIRefreshBegin(RefreshLayout refreshLayout);

    /**
     * 刷新结束，Header 开始向上移动之前调用。
     *
     * @param refreshLayout
     */
    void onUIRefreshComplete(RefreshLayout refreshLayout);

    /**
     * 下拉过程中位置变化回调。
     *
     * @param refreshLayout
     * @param isTouch
     * @param status
     * @param refreshIndicator
     */
    void onUIPositionChange(RefreshLayout refreshLayout, boolean isTouch, byte status, RefreshIndicator refreshIndicator);
}
