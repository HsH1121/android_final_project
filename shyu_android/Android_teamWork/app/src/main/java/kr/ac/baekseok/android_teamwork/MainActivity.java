package kr.ac.baekseok.android_teamwork;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnNews = findViewById(R.id.btnNews);
        Button btnAnalyze = findViewById(R.id.btnAnalyze);
        Button btnChat = findViewById(R.id.btnChat); // 팀원 채팅 기능 버튼

        btnNews.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, NewsActivity.class))
        );

        btnAnalyze.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AnalyzeActivity.class))
        );

        btnChat.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ChatActivity.class))
        );
    }
}
