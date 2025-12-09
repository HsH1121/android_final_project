package com.example.socketchat;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.socketchat.network.AnalyzeRequest;
import com.example.socketchat.network.AnalyzeResponse;
import com.example.socketchat.network.ApiService;
import com.example.socketchat.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnalyzeActivity extends AppCompatActivity {

    private EditText edtInput;
    private Button btnAnalyze;
    private TextView txtResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyze);

        edtInput = findViewById(R.id.edtInput);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        txtResult = findViewById(R.id.txtResult);

        btnAnalyze.setOnClickListener(v -> {
            String text = edtInput.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(this, "텍스트를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            requestAnalyze(text);
        });
    }

    private void requestAnalyze(String text) {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        AnalyzeRequest body = new AnalyzeRequest(text);

        api.analyzeText(body).enqueue(new Callback<AnalyzeResponse>() {
            @Override
            public void onResponse(Call<AnalyzeResponse> call, Response<AnalyzeResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    txtResult.setText("분석 실패");
                    return;
                }
                txtResult.setText(response.body().analysis);
            }

            @Override
            public void onFailure(Call<AnalyzeResponse> call, Throwable t) {
                txtResult.setText("오류: " + t.getMessage());
            }
        });
    }
}
