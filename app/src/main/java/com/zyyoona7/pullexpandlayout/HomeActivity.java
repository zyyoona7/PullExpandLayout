package com.zyyoona7.pullexpandlayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.zyyoona7.pullexpandlayout.activity.HorizontalActivity;
import com.zyyoona7.pullexpandlayout.activity.VerticalActivity;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        AppCompatButton horizontalBtn = findViewById(R.id.btn_horizontal);
        AppCompatButton verticalBtn = findViewById(R.id.btn_vertical);
        horizontalBtn.setOnClickListener(this);
        verticalBtn.setOnClickListener(this);
    }

    private <T> void startActivity(Class<T> acClass) {
        Intent intent = new Intent(this, acClass);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_horizontal:
                startActivity(HorizontalActivity.class);
                break;
            case R.id.btn_vertical:
                startActivity(VerticalActivity.class);
                break;
        }
    }
}
