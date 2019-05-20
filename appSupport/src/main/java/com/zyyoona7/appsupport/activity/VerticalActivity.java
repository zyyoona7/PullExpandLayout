package com.zyyoona7.appsupport.activity;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.ConvertUtils;
import com.zyyoona7.appsupport.R;
import com.zyyoona7.appsupport.adapter.VerticalAdapter;
import com.zyyoona7.appsupport.transformer.ParallaxGamePullExpandTransformer;
import com.zyyoona7.pullexpand.PullExpandLayout;
import com.zyyoona7.pullexpand.listener.SimpleOnPullExpandChangedListener;
import com.zyyoona7.pullexpand.listener.SimpleOnPullExpandStateListener;

public class VerticalActivity extends AppCompatActivity {

    private static final String TAG = "VerticalActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final PullExpandLayout expandLayout = findViewById(R.id.pull_expand_layout);
        expandLayout.setDebug(true);
        expandLayout.setPullExpandTransformer(
                new ParallaxGamePullExpandTransformer(ConvertUtils.dp2px(50f),
                        ConvertUtils.dp2px(130f)));
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        final TextView header1 = findViewById(R.id.tv_header_1);
        final TextView header2 = findViewById(R.id.tv_header_2);
        final SwitchCompat headerSc=findViewById(R.id.sc_header);
        final SwitchCompat footerSc=findViewById(R.id.sc_footer);

        header1.postDelayed(new Runnable() {
            @Override
            public void run() {
                header1.setVisibility(View.GONE);
                header2.setVisibility(View.VISIBLE);
            }
        }, 3000);

        headerSc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandLayout.getCurrentHeaderState()== PullExpandLayout.STATE_EXPANDED) {
                    expandLayout.setHeaderExpanded(false);
                }else if (expandLayout.getCurrentHeaderState()==PullExpandLayout.STATE_COLLAPSED){
                    expandLayout.setHeaderExpanded(true);
                }
            }
        });

        footerSc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandLayout.getCurrentFooterState()== PullExpandLayout.STATE_EXPANDED) {
                    expandLayout.setFooterExpanded(false);
                }else if (expandLayout.getCurrentFooterState()==PullExpandLayout.STATE_COLLAPSED){
                    expandLayout.setFooterExpanded(true);
                }
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        Drawable dividerDrawable = new ColorDrawable(Color.BLACK);
        dividerDrawable.setBounds(0, 0, 0, 5);
        dividerItemDecoration.setDrawable(dividerDrawable);
        recyclerView.addItemDecoration(dividerItemDecoration);
        VerticalAdapter adapter = VerticalAdapter.newInstance(20);

        createHeaderAndFooter(adapter);

        recyclerView.setAdapter(adapter);
        expandLayout.addOnPullExpandChangedListener(new SimpleOnPullExpandChangedListener() {

            @Override
            public void onHeaderMoving(int orientation, float percent, int offset, int heightOrWidth, int maxDragDistance) {
                Log.d(TAG, "onHeaderMoving percent:" + percent + ",offset:" + offset + ",height:" + heightOrWidth);
            }

            @Override
            public void onFooterMoving(int orientation, float percent, int offset, int heightOrWidth, int maxDragDistance) {
                Log.d(TAG, "onFooterMoving percent:" + percent + ",offset:" + offset + ",height:" + heightOrWidth);
            }

            @Override
            public void onHeaderStateChanged(PullExpandLayout layout, int state) {
                Log.d(TAG, "onHeaderStateChanged state:" + state);
            }

            @Override
            public void onReleased(PullExpandLayout layout, int currentOffset) {
                Log.d(TAG, "onReleased: currentOffset:"
                        + currentOffset + ",header height:" + layout.getHeaderHeight());
                if (layout.isHeaderExpanded() && currentOffset < 0
                        && Math.abs(currentOffset) > layout.getHeaderHeight() + 10) {
                    layout.setHeaderExpanded(false);
                }
            }
        });

        expandLayout.addOnPullExpandStateListener(new SimpleOnPullExpandStateListener() {
            @Override
            public void onHeaderStateChanged(PullExpandLayout layout, int state) {
                super.onHeaderStateChanged(layout, state);
                if (state== PullExpandLayout.STATE_EXPANDED) {
                    headerSc.setChecked(true);
                    headerSc.setText("关闭 Header");
                }else if (state==PullExpandLayout.STATE_COLLAPSED){
                    headerSc.setChecked(false);
                    headerSc.setText("打开 Header");
                }
            }

            @Override
            public void onFooterStateChanged(PullExpandLayout layout, int state) {
                super.onFooterStateChanged(layout, state);
                if (state== PullExpandLayout.STATE_EXPANDED) {
                    footerSc.setChecked(true);
                    footerSc.setText("关闭 Footer");
                }else if (state==PullExpandLayout.STATE_COLLAPSED){
                    footerSc.setChecked(false);
                    footerSc.setText("打开 Footer");
                }
            }
        });
    }

    private void createHeaderAndFooter(VerticalAdapter adapter) {
        TextView textView = new TextView(this);
        TextView footerTv = new TextView(this);
        textView.setText("我是顶部");
        footerTv.setText("我是底部");
        textView.setGravity(Gravity.CENTER);
        footerTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ConvertUtils.dp2px(50f));
        layoutParams.gravity = Gravity.CENTER;
        textView.setLayoutParams(layoutParams);
        footerTv.setLayoutParams(layoutParams);
        textView.setBackgroundColor(Color.CYAN);
        footerTv.setBackgroundColor(Color.BLUE);
        adapter.setHeaderView(textView);
        adapter.setFooterView(footerTv);
    }
}
