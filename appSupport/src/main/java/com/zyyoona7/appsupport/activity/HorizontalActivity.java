package com.zyyoona7.appsupport.activity;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.ConvertUtils;
import com.zyyoona7.appsupport.R;
import com.zyyoona7.appsupport.adapter.HorizontalAdapter;
import com.zyyoona7.appsupport.transformer.ParallaxGamePullExpandTransformer;
import com.zyyoona7.pullexpand.PullExpandLayout;

public class HorizontalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        PullExpandLayout expandLayout = findViewById(R.id.pull_expand_layout);

        RecyclerView recyclerView = findViewById(R.id.rv_horizontal);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL);
        Drawable dividerDrawable = new ColorDrawable(Color.BLACK);
        dividerDrawable.setBounds(0, 0, 0, 5);
        dividerItemDecoration.setDrawable(dividerDrawable);
        recyclerView.addItemDecoration(dividerItemDecoration);
        HorizontalAdapter adapter = HorizontalAdapter.newInstance(20);

        createHeaderAndFooter(adapter);

        recyclerView.setAdapter(adapter);

        expandLayout.setPullExpandTransformer(new ParallaxGamePullExpandTransformer(ConvertUtils.dp2px(50),
                ConvertUtils.dp2px(130)));
    }

    private void createHeaderAndFooter(HorizontalAdapter adapter) {
        TextView textView = new TextView(this);
        TextView footerTv = new TextView(this);
        textView.setText("我是顶部");
        footerTv.setText("我是底部");
        textView.setGravity(Gravity.CENTER);
        footerTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(ConvertUtils.dp2px(50f),
                        ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER;
        textView.setLayoutParams(layoutParams);
        footerTv.setLayoutParams(layoutParams);
        textView.setBackgroundColor(Color.CYAN);
        footerTv.setBackgroundColor(Color.BLUE);
        adapter.setHeaderView(textView, 0, LinearLayout.HORIZONTAL);
        adapter.setFooterView(footerTv, 0, LinearLayout.HORIZONTAL);
    }
}
