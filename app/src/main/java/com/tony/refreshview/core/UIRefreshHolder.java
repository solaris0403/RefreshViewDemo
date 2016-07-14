package com.tony.refreshview.core;


import com.tony.refreshview.indicator.RefreshIndicator;

/**
 * A single linked list to wrap PtrUIHandler
 * <p/>
 * 实现 UI 接口 PtrUIHandler，封装了 PtrUIHandler，并将其组织成链表的形式。
 * 之所以封装成链表的目的是作者希望调用者可以像 Header 一样去实现 PtrUIHandler，
 * 能够捕捉到 onUIReset，onUIRefreshPrepare，onUIRefreshBegin，onUIRefreshComplete
 * 这几个时机去实现自己的逻辑或者 UI 效果，而它们统一由 PtrUIHandlerHolder 来管理，你只需要
 * 通过 addHandler 方法加入到链表中即可，这一点的抽象为那些希望去做一些处理的开发者还是相当方便的。
 */
public class UIRefreshHolder implements UIRefresh {

    private UIRefresh mRefresh;
    private UIRefreshHolder mNext;

    private boolean contains(UIRefresh handler) {
        return mRefresh != null && mRefresh == handler;
    }

    private UIRefreshHolder() {

    }

    public boolean hasHandler() {
        return mRefresh != null;
    }

    private UIRefresh getHandler() {
        return mRefresh;
    }

    public static void addHandler(UIRefreshHolder head, UIRefresh handler) {

        if (null == handler) {
            return;
        }
        if (head == null) {
            return;
        }
        if (null == head.mRefresh) {
            head.mRefresh = handler;
            return;
        }

        UIRefreshHolder current = head;
        for (; ; current = current.mNext) {

            // duplicated
            if (current.contains(handler)) {
                return;
            }
            if (current.mNext == null) {
                break;
            }
        }

        UIRefreshHolder newHolder = new UIRefreshHolder();
        newHolder.mRefresh = handler;
        current.mNext = newHolder;
    }

    public static UIRefreshHolder create() {
        return new UIRefreshHolder();
    }

    public static UIRefreshHolder removeHandler(UIRefreshHolder head, UIRefresh handler) {
        if (head == null || handler == null || null == head.mRefresh) {
            return head;
        }

        UIRefreshHolder current = head;
        UIRefreshHolder pre = null;
        do {

            // delete current: link pre to next, unlink next from current;
            // pre will no change, current move to next element;
            if (current.contains(handler)) {

                // current is head
                if (pre == null) {

                    head = current.mNext;
                    current.mNext = null;

                    current = head;
                } else {

                    pre.mNext = current.mNext;
                    current.mNext = null;
                    current = pre.mNext;
                }
            } else {
                pre = current;
                current = current.mNext;
            }

        } while (current != null);

        if (head == null) {
            head = new UIRefreshHolder();
        }
        return head;
    }

    @Override
    public void onUIReset(RefreshLayout frame) {
        UIRefreshHolder current = this;
        do {
            final UIRefresh handler = current.getHandler();
            if (null != handler) {
                handler.onUIReset(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIRefreshPrepare(RefreshLayout frame) {
        if (!hasHandler()) {
            return;
        }
        UIRefreshHolder current = this;
        do {
            final UIRefresh handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshPrepare(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIRefreshBegin(RefreshLayout frame) {
        UIRefreshHolder current = this;
        do {
            final UIRefresh handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshBegin(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIRefreshComplete(RefreshLayout frame) {
        UIRefreshHolder current = this;
        do {
            final UIRefresh handler = current.getHandler();
            if (null != handler) {
                handler.onUIRefreshComplete(frame);
            }
        } while ((current = current.mNext) != null);
    }

    @Override
    public void onUIPositionChange(RefreshLayout frame, boolean isUnderTouch, byte status, RefreshIndicator ptrIndicator) {
        UIRefreshHolder current = this;
        do {
            final UIRefresh handler = current.getHandler();
            if (null != handler) {
                handler.onUIPositionChange(frame, isUnderTouch, status, ptrIndicator);
            }
        } while ((current = current.mNext) != null);
    }
}
