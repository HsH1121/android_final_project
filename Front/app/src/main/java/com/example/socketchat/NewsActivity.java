package com.example.socketchat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.socketchat.network.ApiService;
import com.example.socketchat.network.NewsItem;
import com.example.socketchat.network.NewsResponse;
import com.example.socketchat.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsActivity extends AppCompatActivity {

    private EditText edtQuery;
    private Button btnSearch;
    private ListView listNews;

    private ArrayList<String> titles = new ArrayList<>();
    private ArrayList<String> links = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        edtQuery = findViewById(R.id.edtQuery);
        btnSearch = findViewById(R.id.btnSearch);
        listNews = findViewById(R.id.listNews);

        btnSearch.setOnClickListener(v -> {
            String q = edtQuery.getText().toString().trim();
            if (TextUtils.isEmpty(q)) {
                Toast.makeText(this, "검색어를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            searchNews(q);
        });

        listNews.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= links.size()) return;
            String url = links.get(position);
            if (!TextUtils.isEmpty(url)) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });
    }

    private void searchNews(String query) {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.searchNews(query, 10).enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(NewsActivity.this, "검색 실패", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<NewsItem> news = response.body().news;
                if (news == null || news.isEmpty()) {
                    Toast.makeText(NewsActivity.this, "결과 없음", Toast.LENGTH_SHORT).show();
                    return;
                }

                titles.clear();
                links.clear();
                for (NewsItem item : news) {
                    titles.add(item.title);
                    links.add(item.link);
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        NewsActivity.this,
                        android.R.layout.simple_list_item_1,
                        titles
                );
                listNews.setAdapter(adapter);
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                Toast.makeText(NewsActivity.this,
                        "오류: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
