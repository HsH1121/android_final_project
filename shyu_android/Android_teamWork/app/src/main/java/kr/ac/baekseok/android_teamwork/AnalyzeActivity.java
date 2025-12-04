package kr.ac.baekseok.android_teamwork;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import kr.ac.baekseok.android_teamwork.models.AnalyzeRequest;
import kr.ac.baekseok.android_teamwork.models.AnalyzeResponse;
import kr.ac.baekseok.android_teamwork.network.ApiService;
import kr.ac.baekseok.android_teamwork.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnalyzeActivity extends AppCompatActivity {

    private ApiService api;
    private EditText edtInput;
    private TextView txtResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyze);

        api = RetrofitClient.getClient().create(ApiService.class);

        edtInput = findViewById(R.id.edtInput);
        Button btnAnalyze = findViewById(R.id.btnAnalyze);
        txtResult = findViewById(R.id.txtResult);

        btnAnalyze.setOnClickListener(v -> analyze());
    }

    private void analyze() {
        String text = edtInput.getText().toString();

        if (text.isEmpty()) {
            Toast.makeText(this, "텍스트를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        api.analyzeText(new AnalyzeRequest(text)).enqueue(new Callback<AnalyzeResponse>() {
            @Override
            public void onResponse(Call<AnalyzeResponse> call, Response<AnalyzeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    txtResult.setText(response.body().analysis);
                } else {
                    txtResult.setText("분석 실패");
                }
            }

            @Override
            public void onFailure(Call<AnalyzeResponse> call, Throwable t) {
                txtResult.setText("오류: " + t.getMessage());
            }
        });
    }
}
