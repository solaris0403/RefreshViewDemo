package com.tony.refreshview.core;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by tony on 6/24/16.
 */
public class ClassicRefreshLayout extends RefreshLayout {
    private ClassicRefreshHeader mPtrClassicHeader;

    public ClassicRefreshLayout(Context context) {
        super(context);
        initViews();
    }

    public ClassicRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews();
    }

    public ClassicRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews();
    }

    private void initViews() {
        mPtrClassicHeader = new ClassicRefreshHeader(getContext());
        setHeaderView(mPtrClassicHeader);
        addPtrUIHandler(mPtrClassicHeader);
    }

    public ClassicRefreshHeader getHeader() {
        return mPtrClassicHeader;
    }

    /**
     * Specify the last update time by this key string
     *
     * @param key
     */
    public void setLastUpdateTimeKey(String key) {
        if (mPtrClassicHeader != null) {
            mPtrClassicHeader.setLastUpdateTimeKey(key);
        }
    }

    /**
     * Using an object to specify the last update time.
     *
     * @param object
     */
    public void setLastUpdateTimeRelateObject(Object object) {
        if (mPtrClassicHeader != null) {
            mPtrClassicHeader.setLastUpdateTimeRelateObject(object);
        }
    }
}
