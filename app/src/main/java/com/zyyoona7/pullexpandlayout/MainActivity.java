package com.zyyoona7.pullexpandlayout;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zyyoona7.pullexpandx.PullExpandLayout;
import com.zyyoona7.pullexpandx.listener.SimpleOnPullExpandChangedListener;
import com.zyyoona7.pullexpandx.listener.SimpleOnPullExpandStateListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PullExpandLayout expandLayout = findViewById(R.id.pull_expand_layout);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        final TextView header1=findViewById(R.id.tv_header_1);
        final TextView header2=findViewById(R.id.tv_header_2);

        header1.postDelayed(new Runnable() {
            @Override
            public void run() {
                header1.setVisibility(View.GONE);
                header2.setVisibility(View.VISIBLE);
            }
        },3000);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        Drawable dividerDrawable = new ColorDrawable(Color.BLACK);
        dividerDrawable.setBounds(0, 0, 0, 5);
        dividerItemDecoration.setDrawable(dividerDrawable);
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setAdapter(MainAdapter.newInstance(30));
        expandLayout.setOnPullExpandChangedListener(new SimpleOnPullExpandChangedListener() {

            @Override
            public void onHeaderMoving(int orientation, float percent, int offset, int heightOrWidth, int maxDragDistance) {
                Log.d(TAG,"onHeaderMoving percent:"+percent+",offset:"+offset+",height:"+heightOrWidth);
            }

            @Override
            public void onHeaderStateChanged(PullExpandLayout layout, int state) {
                Log.d(TAG,"onHeaderStateChanged state:"+state);
            }
        });

        expandLayout.setOnPullExpandStateListener(new SimpleOnPullExpandStateListener() {

        });
    }
}
