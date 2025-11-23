// Front/app/src/main/java/com/example/socketchat/NewChatActivity.java
package com.example.socketchat;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
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
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NewChatActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://10.0.2.2:8000";

    EditText edtContact;
    Button btnSaveContact, btnBack;
    UserDBHelper dbHelper;
    OkHttpClient httpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_chat);

        edtContact = findViewById(R.id.edtContact);
        btnSaveContact = findViewById(R.id.btnSaveContact);
        btnBack = findViewById(R.id.btnBack);

        dbHelper = new UserDBHelper(this);

        btnSaveContact.setOnClickListener(v -> saveContact());

        btnBack.setOnClickListener(v -> finish());
    }

    private void saveContact() {
        String targetPhone = edtContact.getText().toString().trim();
        if (TextUtils.isEmpty(targetPhone)) {
            Toast.makeText(this, "상대방 전화번호를 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1) 서버에 이 전화번호로 가입된 유저가 있는지 확인
        HttpUrl url = HttpUrl.parse(BASE_URL + "/users/exists")
                .newBuilder()
                .addQueryParameter("phone", targetPhone)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(NewChatActivity.this,
                                "서버 연결 실패: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    final String msg = "서버 응답 오류 (" + response.code() + ")";
                    runOnUiThread(() ->
                            Toast.makeText(NewChatActivity.this,
                                    msg,
                                    Toast.LENGTH_LONG).show());
                    return;
                }

                String bodyStr = response.body() != null ? response.body().string() : null;
                boolean exists = false;
                String nickname = null;

                try {
                    if (bodyStr != null) {
                        JSONObject obj = new JSONObject(bodyStr);
                        exists = obj.optBoolean("exists", false);
                        nickname = obj.optString("nickname", null);
                    }
                } catch (Exception ignored) {
                }

                if (!exists) {
                    runOnUiThread(() ->
                            Toast.makeText(NewChatActivity.this,
                                    "서버에 가입되지 않은 전화번호입니다.",
                                    Toast.LENGTH_LONG).show());
                    return;
                }

                // exists=true 인 경우, nickname 이 null 일 수도 있으니 예외 처리
                if (nickname == null || nickname.isEmpty()) {
                    nickname = targetPhone;
                }

                String finalNickname = nickname;

                runOnUiThread(() -> {
                    // ★ 1) 로그인한 내 번호 가져오기
                    SharedPreferences prefs = getSharedPreferences("chat_prefs", MODE_PRIVATE);
                    String myPhone = prefs.getString("current_phone", null);
                    if (myPhone == null) {
                        Toast.makeText(NewChatActivity.this,
                                "로그인 정보가 없습니다. 다시 로그인해 주세요.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // ★ 2) 로컬 chat_partners 테이블에 저장 (owner_phone 포함)
                    SQLiteDatabase sqlDB = null;
                    try {
                        sqlDB = dbHelper.getWritableDatabase();
                        sqlDB.execSQL(
                                "INSERT OR IGNORE INTO " + UserDBHelper.TABLE_CHAT_PARTNERS +
                                        " (owner_phone, phone, nickname) VALUES (?, ?, ?)",
                                new Object[]{myPhone, targetPhone, finalNickname}
                        );
                        Toast.makeText(NewChatActivity.this,
                                "대화 상대로 등록되었습니다.",
                                Toast.LENGTH_SHORT).show();
                        finish();  // 메인으로 복귀 → onResume에서 목록 갱신
                    } catch (Exception e) {
                        Toast.makeText(NewChatActivity.this,
                                "이미 등록된 상대이거나 오류가 발생했습니다.",
                                Toast.LENGTH_LONG).show();
                    } finally {
                        if (sqlDB != null) {
                            sqlDB.close();
                        }
                    }
                });
            }
        });
    }
}
