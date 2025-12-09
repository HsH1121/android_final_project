package com.example.socketchat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button btnNews = findViewById(R.id.btnNews);
        Button btnAnalyze = findViewById(R.id.btnAnalyze);
        Button btnChat = findViewById(R.id.btnChat);

        btnNews.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, NewsActivity.class))
        );

        btnAnalyze.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, AnalyzeActivity.class))
        );

        // ★ 여기서 기존 Front의 채팅 메인(MainActivity)로 이동
        btnChat.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, MainActivity.class))
        );
    }
}
