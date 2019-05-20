package com.zyyoona7.pullexpandlayout.activity;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ConvertUtils;
import com.zyyoona7.pullexpandlayout.R;
import com.zyyoona7.pullexpandlayout.adapter.VerticalAdapter;
import com.zyyoona7.pullexpandlayout.transformer.ParallaxGamePullExpandTransformer;
import com.zyyoona7.pullexpandx.PullExpandLayout;
import com.zyyoona7.pullexpandx.listener.SimpleOnPullExpandChangedListener;
import com.zyyoona7.pullexpandx.listener.SimpleOnPullExpandStateListener;

public class VerticalActivity extends AppCompatActivity {

    private static final String TAG = "VerticalActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PullExpandLayout expandLayout = findViewById(R.id.pull_expand_layout);
        expandLayout.setPullExpandTransformer(
                new ParallaxGamePullExpandTransformer(ConvertUtils.dp2px(50f),
                        ConvertUtils.dp2px(130f)));
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        final TextView header1 = findViewById(R.id.tv_header_1);
        final TextView header2 = findViewById(R.id.tv_header_2);

        header1.postDelayed(new Runnable() {
            @Override
            public void run() {
                header1.setVisibility(View.GONE);
                header2.setVisibility(View.VISIBLE);
            }
        }, 3000);

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

        });
    }

    private void createHeaderAndFooter(VerticalAdapter adapter){
        TextView textView = new TextView(this);
        TextView footerTv=new TextView(this);
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