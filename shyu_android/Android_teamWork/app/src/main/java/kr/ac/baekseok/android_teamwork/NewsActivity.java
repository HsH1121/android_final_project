package kr.ac.baekseok.android_teamwork;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import kr.ac.baekseok.android_teamwork.models.NewsItem;
import kr.ac.baekseok.android_teamwork.models.NewsResponse;
import kr.ac.baekseok.android_teamwork.network.ApiService;
import kr.ac.baekseok.android_teamwork.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsActivity extends AppCompatActivity {

    private ApiService api;
    private EditText edtQuery;
    private ListView listNews;
    private ArrayList<NewsItem> currentNews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        api = RetrofitClient.getClient().create(ApiService.class);

        edtQuery = findViewById(R.id.edtQuery);
        Button btnSearch = findViewById(R.id.btnSearch);
        listNews = findViewById(R.id.listNews);

        btnSearch.setOnClickListener(v -> searchNews());

        listNews.setOnItemClickListener((parent, view, position, id) -> {
            String url = currentNews.get(position).link;
            if (url != null) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });
    }

    private void searchNews() {
        String q = edtQuery.getText().toString().trim();

        if (q.isEmpty()) {
            Toast.makeText(this, "검색어를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        api.searchNews(q, 10).enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentNews.clear();
                    currentNews.addAll(response.body().news);

                    ArrayList<String> titles = new ArrayList<>();
                    for (NewsItem item : response.body().news) {
                        titles.add(item.title);
                    }

                    ArrayAdapter<String> adapter =
                            new ArrayAdapter<>(NewsActivity.this,
                                    android.R.layout.simple_list_item_1,
                                    titles);

                    listNews.setAdapter(adapter);

                } else {
                    Toast.makeText(NewsActivity.this, "결과 없음", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                Toast.makeText(NewsActivity.this, "오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
