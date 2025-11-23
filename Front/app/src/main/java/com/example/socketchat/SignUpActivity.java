// Front/app/src/main/java/com/example/socketchat/SignUpActivity.java
package com.example.socketchat;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignUpActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://10.0.2.2:8000";

    EditText edtSignUpId, edtSignUpPw, edtSignUpPhone, edtSignUpNickname;
    Button btnSignUpOk;

    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        edtSignUpId = findViewById(R.id.edtSignUpId);
        edtSignUpPw = findViewById(R.id.edtSignUpPw);
        edtSignUpPhone = findViewById(R.id.edtSignUpPhone);
        edtSignUpNickname = findViewById(R.id.edtSignUpNickname);
        btnSignUpOk = findViewById(R.id.btnSignUpOk);

        btnSignUpOk.setOnClickListener(v -> doSignUp());
    }

    private void doSignUp() {
        String id = edtSignUpId.getText().toString().trim();
        String pw = edtSignUpPw.getText().toString().trim();
        String phone = edtSignUpPhone.getText().toString().trim();
        String nickname = edtSignUpNickname.getText().toString().trim();

        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(pw) ||
                TextUtils.isEmpty(phone) || TextUtils.isEmpty(nickname)) {
            Toast.makeText(this, "모든 항목을 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("username", id);
            json.put("password", pw);
            json.put("phone", phone);
            json.put("nickname", nickname);

            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(json.toString(), mediaType);

            Request request = new Request.Builder()
                    .url(BASE_URL + "/signup")
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(SignUpActivity.this,
                                    "서버 연결 실패: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(SignUpActivity.this,
                                    "회원가입이 완료되었습니다.",
                                    Toast.LENGTH_SHORT).show();
                            finish();  // 메인으로 복귀
                        });
                    } else {
                        String msg = "회원가입 실패 (" + response.code() + ")";
                        String bodyStr = response.body() != null ? response.body().string() : null;
                        if (bodyStr != null) {
                            try {
                                JSONObject err = new JSONObject(bodyStr);
                                if (err.has("detail")) {
                                    msg = err.getString("detail");
                                }
                            } catch (Exception ignored) {}
                        }
                        final String finalMsg = msg;
                        runOnUiThread(() ->
                                Toast.makeText(SignUpActivity.this,
                                        finalMsg,
                                        Toast.LENGTH_LONG).show());
                    }
                }
            });

        } catch (Exception e) {
            Toast.makeText(this,
                    "요청 생성 중 오류: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}
