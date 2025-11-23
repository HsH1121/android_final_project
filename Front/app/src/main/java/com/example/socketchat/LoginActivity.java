// Front/app/src/main/java/com/example/socketchat/LoginActivity.java
package com.example.socketchat;

import android.content.Intent;
import android.content.SharedPreferences;
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

public class LoginActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://10.0.2.2:8000";

    EditText edtLoginId, edtLoginPw;
    Button btnLoginOk;

    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtLoginId = findViewById(R.id.edtLoginId);
        edtLoginPw = findViewById(R.id.edtLoginPw);
        btnLoginOk = findViewById(R.id.btnLoginOk);

        btnLoginOk.setOnClickListener(v -> doLogin());
    }

    private void doLogin() {
        String id = edtLoginId.getText().toString().trim();
        String pw = edtLoginPw.getText().toString().trim();

        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(pw)) {
            Toast.makeText(this, "아이디와 비밀번호를 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("username", id);
            json.put("password", pw);

            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(json.toString(), mediaType);

            Request request = new Request.Builder()
                    .url(BASE_URL + "/login")
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this,
                                    "서버 연결 실패: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String bodyStr = response.body() != null ? response.body().string() : null;
                        String phone = null;
                        String nickname = null;

                        try {
                            if (bodyStr != null) {
                                JSONObject obj = new JSONObject(bodyStr);
                                phone = obj.optString("phone", null);
                                nickname = obj.optString("nickname", null);
                            }
                        } catch (Exception ignored) {}

                        String finalPhone = phone;
                        String finalNickname = nickname;

                        runOnUiThread(() -> {
                            if (finalPhone == null) {
                                Toast.makeText(LoginActivity.this,
                                        "로그인 응답이 올바르지 않습니다.",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            // WebSocket userId로 쓸 내 전화번호 저장
                            SharedPreferences prefs = getSharedPreferences("chat_prefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("current_phone", finalPhone);
                            if (finalNickname != null) {
                                editor.putString("current_nickname", finalNickname);
                            }
                            editor.apply();

                            Toast.makeText(LoginActivity.this,
                                    "로그인 성공" + (finalNickname != null ? (": " + finalNickname) : ""),
                                    Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish(); // 메인으로 복귀
                        });
                    } else {
                        String msg = "로그인 실패 (" + response.code() + ")";
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
                                Toast.makeText(LoginActivity.this,
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
