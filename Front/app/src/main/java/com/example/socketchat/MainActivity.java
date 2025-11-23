package com.example.socketchat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;

public class MainActivity extends AppCompatActivity {

    ListView listViewChats;
    Button btnNewChat, btnLogin, btnSignUp;

    UserDBHelper dbHelper;
    SQLiteDatabase sqlDB;

    ArrayList<String> chatPartnerNicknames;
    ArrayList<String> chatPartnerPhones;
    ArrayAdapter<String> chatAdapter;
    OkHttpClient httpClient = new OkHttpClient();
    private static final String BASE_URL = "http://10.0.2.2:8000";
    String myPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        listViewChats = findViewById(R.id.listViewChats);
        btnNewChat = findViewById(R.id.btnNewChat);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);

        dbHelper = new UserDBHelper(this);

        chatPartnerNicknames = new ArrayList<>();
        chatPartnerPhones = new ArrayList<>();
        chatAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                chatPartnerNicknames
        );
        listViewChats.setAdapter(chatAdapter);

        // 리스트 클릭 → ChatActivity 로 이동 (닉네임 + 전화번호 전달)
        listViewChats.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String nickname = chatPartnerNicknames.get(position);
                String phone = chatPartnerPhones.get(position);

                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                intent.putExtra("targetNickname", nickname);
                intent.putExtra("targetPhone", phone);
                startActivity(intent);
            }
        });

        // 새 채팅 시작 → NewChatActivity
        btnNewChat.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NewChatActivity.class);
            startActivity(intent);
        });

        // 로그인 / 회원가입
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 로그인 정보에서 내 전화번호 다시 읽어오기
        SharedPreferences prefs = getSharedPreferences("chat_prefs", MODE_PRIVATE);
        myPhone = prefs.getString("current_phone", null);

        if (myPhone == null) {
            // 로그인 안 된 상태면 목록을 비우고 리턴
            chatPartnerNicknames.clear();
            chatPartnerPhones.clear();
            chatAdapter.notifyDataSetChanged();
            return;
        }

        // 로그인 되어 있으면, 서버에 쌓여 있던 오프라인 메시지를 먼저 동기화
        syncOfflineMessages();

        // 로컬 DB에 이미 있는 대화 상대 목록도 바로 표시
        loadChatPartnersFromDb();
    }

    private void syncOfflineMessages() {
        if (myPhone == null) return;

        HttpUrl url = HttpUrl.parse(BASE_URL + "/sync")
                .newBuilder()
                .addQueryParameter("userId", myPhone)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 동기화 실패해도 앱 동작에는 치명적이지 않으므로 로그만 남기고 무시
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    // 에러 응답이면 그대로 종료
                    return;
                }

                String body = response.body() != null ? response.body().string() : null;
                if (body == null || body.isEmpty()) {
                    return;
                }

                try {
                    JSONObject root = new JSONObject(body);
                    JSONArray items = root.optJSONArray("items");
                    if (items == null) return;

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.optJSONObject(i);
                        if (item == null) continue;

                        String type = item.optString("type", "");
                        if (!"message".equals(type)) {
                            continue;   // message 타입만 처리
                        }

                        String from = item.optString("from", null);
                        String to = item.optString("to", null);
                        String text = item.optString("text", "");

                        if (from == null || to == null) continue;

                        // 이 sync는 myPhone 기준이라 to == myPhone 인 메시지가 대부분일 것
                        String partnerPhone;
                        if (myPhone.equals(from)) {
                            partnerPhone = to;
                        } else if (myPhone.equals(to)) {
                            partnerPhone = from;
                        } else {
                            // 나와 상관없는 메시지는 스킵
                            continue;
                        }

                        if (partnerPhone == null || partnerPhone.isEmpty()) continue;

                        // 1) 로컬 DB에 채팅방(대화상대) 등록
                        ensureChatPartnerInDb(myPhone, partnerPhone);
                        // 2) 메시지 저장
                        saveMessageToDb(myPhone, partnerPhone, from, text);
                    }

                    // DB 저장이 끝났다면 메인 스레드에서 목록을 다시 읽어온다.
                    runOnUiThread(() -> {
                        if (myPhone != null) {
                            loadChatPartnersFromDb();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void saveMessageToDb(String myPhone,
                                 String partnerPhone,
                                 String senderPhone,
                                 String content) {
        if (myPhone == null || partnerPhone == null || senderPhone == null) return;

        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.execSQL(
                    "INSERT INTO " + UserDBHelper.TABLE_MESSAGES +
                            " (my_phone, partner_phone, sender_phone, content, created_at) " +
                            "VALUES (?, ?, ?, ?, ?)",
                    new Object[]{
                            myPhone,
                            partnerPhone,
                            senderPhone,
                            content,
                            System.currentTimeMillis()
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (db != null) db.close();
        }
    }

    private void ensureChatPartnerInDb(String ownerPhone, String partnerPhone) {
        if (ownerPhone == null || partnerPhone == null) return;

        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.execSQL(
                    "INSERT OR IGNORE INTO " + UserDBHelper.TABLE_CHAT_PARTNERS +
                            " (owner_phone, phone, nickname) VALUES (?, ?, ?)",
                    new Object[]{ownerPhone, partnerPhone, partnerPhone}  // 닉네임은 일단 전화번호로
            );
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (db != null) db.close();
        }
    }


    private void loadChatPartnersFromDb() {
        chatPartnerNicknames.clear();
        chatPartnerPhones.clear();

        if (myPhone == null) {
            return;
        }

        sqlDB = dbHelper.getReadableDatabase();
        Cursor cursor = sqlDB.rawQuery(
                "SELECT nickname, phone FROM " + UserDBHelper.TABLE_CHAT_PARTNERS +
                        " WHERE owner_phone=? ORDER BY _id DESC",
                new String[]{myPhone}
        );

        while (cursor.moveToNext()) {
            String nickname = cursor.getString(0);
            String phone = cursor.getString(1);

            chatPartnerNicknames.add(nickname); // ★ 리스트에 뜨는 이름 = 상대방 닉네임
            chatPartnerPhones.add(phone);
        }

        cursor.close();
        sqlDB.close();

        chatAdapter.notifyDataSetChanged();
    }
}
