package com.tony.refreshview.core;

import android.view.View;

/**
 * 刷新功能接口
 */
public interface Refresh {
    /**
     * 判断是否可以下拉刷新。Content 可以包含任何内容，用户在这里判断决定是否可以下拉。
     * 如果 Content 是 TextView，则可以直接返回 true，表示可以下拉刷新。
     * 如果 Content 是 ListView，当第一条在顶部时返回 true，表示可以下拉刷新。
     * 如果 Content 是 ScrollView，当滑动到顶部时返回 true，表示可以刷新。
     * @param refreshLayout
     * @param header
     * @param content
     * @return
     */
    boolean canRefresh(final RefreshLayout refreshLayout, final View header, final View content);

    /**
     * 刷新回调函数，用户在这里写自己的刷新功能实现，处理业务数据的刷新。
     * @param refreshLayout
     * @return
     */
    void onRefreshBegin(final RefreshLayout refreshLayout);
}
