package com.tony.refreshview.core;

import android.view.View;
import android.widget.AbsListView;

/**
 * 常见 View 是否可以下拉的判断方法。
 */
public abstract class DefaultRefresh implements Refresh {
    /**
     * 如果 Content 不是 ViewGroup，返回 true,表示可以下拉</br>
     * 例如：TextView，ImageView
     */
    public static boolean canChildScrollUp(View view) {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (view instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) view;
                return absListView.getChildCount() > 0 && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0).getTop() < absListView.getPaddingTop());
            } else {
                return view.getScrollY() > 0;
            }
        } else {
            return view.canScrollVertically(-1);
        }
    }

    /**
     * Default implement for check can perform pull to refresh
     *
     * @param frame
     * @param content
     * @param header
     * @return
     */
    public static boolean checkContentCanBePulledDown(RefreshLayout frame, View header, View content) {
        return !canChildScrollUp(content);
    }

    @Override
    public boolean canRefresh(RefreshLayout refreshLayout, View header, View content) {
        return checkContentCanBePulledDown(refreshLayout, header, content);
    }
}
