package com.example.somu.activityswitcher;

import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class LauncherActivity extends AppCompatActivity {

    public void firstActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        final TextView countDown = findViewById(R.id.count);
        new CountDownTimer(3000, 1000) {
            int cd=3;
            @Override
            public void onTick(long l) {
                countDown.setText(Integer.toString(cd));
                cd--;
            }

            @Override
            public void onFinish() {
                countDown.setText(Integer.toString(cd));
                firstActivity();
                finish();
            }
        }.start();

    }
}
