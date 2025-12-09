package com.example.socketchat;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.socketchat.network.ApiService;
import com.example.socketchat.network.FactCheckArticle;
import com.example.socketchat.network.FactCheckRequest;
import com.example.socketchat.network.FactCheckResponse;
import com.example.socketchat.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FactCheckActivity extends AppCompatActivity {

    private EditText etClaim;
    private Button btnCheck;
    private TextView tvVerdict;
    private TextView tvAnalysis;
    private LinearLayout layoutArticles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fact_check);

        etClaim = findViewById(R.id.etClaim);
        btnCheck = findViewById(R.id.btnCheck);
        tvVerdict = findViewById(R.id.tvVerdict);
        tvAnalysis = findViewById(R.id.tvAnalysis);
        layoutArticles = findViewById(R.id.layoutArticles);

        // 채팅에서 롱클릭으로 넘어온 텍스트
        String initialText = getIntent().getStringExtra("CLAIM_TEXT");
        if (!TextUtils.isEmpty(initialText)) {
            etClaim.setText(initialText);
        }

        btnCheck.setOnClickListener(v -> {
            String claim = etClaim.getText().toString().trim();
            if (TextUtils.isEmpty(claim)) {
                Toast.makeText(this, "검증할 문장을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            requestFactCheck(claim);
        });
    }

    private void requestFactCheck(String claim) {
        tvVerdict.setText("");
        tvAnalysis.setText("");
        layoutArticles.removeAllViews();

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        FactCheckRequest body = new FactCheckRequest(claim);

        api.factCheck(body).enqueue(new Callback<FactCheckResponse>() {
            @Override
            public void onResponse(Call<FactCheckResponse> call, Response<FactCheckResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(FactCheckActivity.this, "서버 오류", Toast.LENGTH_LONG).show();
                    return;
                }

                FactCheckResponse res = response.body();
                tvVerdict.setText("판단: " + res.verdict);
                tvAnalysis.setText(res.analysis);

                if (res.articles != null) {
                    for (FactCheckArticle article : res.articles) {
                        TextView tv = new TextView(FactCheckActivity.this);
                        tv.setText("• " + article.title + "\n" + article.link);
                        layoutArticles.addView(tv);
                    }
                }
            }

            @Override
            public void onFailure(Call<FactCheckResponse> call, Throwable t) {
                Toast.makeText(FactCheckActivity.this,
                        "네트워크 오류: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
