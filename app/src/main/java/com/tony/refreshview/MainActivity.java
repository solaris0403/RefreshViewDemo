package com.tony.refreshview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.tony.refreshview.core.ClassicRefreshLayout;
import com.tony.refreshview.core.DefaultRefresh;
import com.tony.refreshview.core.UIRefresh;
import com.tony.refreshview.core.UIRefreshHook;
import com.tony.refreshview.core.RefreshLayout;
import com.tony.refreshview.indicator.RefreshIndicator;

public class MainActivity extends AppCompatActivity {
    private ClassicRefreshLayout mClassicRefreshLayout;
    private ScrollView mCrl;
    private TextView mTvFirst;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTvFirst = (TextView) findViewById(R.id.tv_first);
        mClassicRefreshLayout = (ClassicRefreshLayout) findViewById(R.id.crl_refresh);
        mCrl = (ScrollView) findViewById(R.id.scrollView);
        mClassicRefreshLayout.setLastUpdateTimeRelateObject(this);
        mClassicRefreshLayout.setResistance(2.2f);
        mClassicRefreshLayout.addPtrUIHandler(new UIRefresh() {
            @Override
            public void onUIReset(RefreshLayout refreshLayout) {
                mTvFirst.setText("onUIReset");
            }

            @Override
            public void onUIRefreshPrepare(RefreshLayout refreshLayout) {

            }

            @Override
            public void onUIRefreshBegin(RefreshLayout refreshLayout) {

            }

            @Override
            public void onUIRefreshComplete(RefreshLayout refreshLayout) {
                mTvFirst.setText("onUIRefreshComplete");
            }

            @Override
            public void onUIPositionChange(RefreshLayout refreshLayout, boolean isTouch, byte status, RefreshIndicator refreshIndicator) {
                mTvFirst.setText(refreshIndicator.getCurrentPosY()+"");
            }
        });
        mClassicRefreshLayout.setRefreshCompleteHook(new UIRefreshHook() {
            @Override
            public void run() {
                resume();
            }
        });
        mClassicRefreshLayout.setPtrHandler(new DefaultRefresh() {
            @Override
            public void onRefreshBegin(RefreshLayout refreshLayout) {
                refreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mClassicRefreshLayout.refreshComplete();
                    }
                }, 1500);
            }
        });
        mTvFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClassicRefreshLayout.autoRefresh(true);
            }
        });
    }
}
