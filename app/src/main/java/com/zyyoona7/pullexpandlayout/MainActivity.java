package com.zyyoona7.pullexpandlayout;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.zyyoona7.pullexpandx.PullExpandLayout;
import com.zyyoona7.pullexpandx.listener.SimpleOnPullExpandChangedListener;
import com.zyyoona7.pullexpandx.listener.SimpleOnPullExpandStateListener;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PullExpandLayout expandLayout = findViewById(R.id.pull_expand_layout);
        expandLayout.setOnPullExpandChangedListener(new SimpleOnPullExpandChangedListener() {

        });

        expandLayout.setOnPullExpandStateListener(new SimpleOnPullExpandStateListener() {

        });
    }
}
