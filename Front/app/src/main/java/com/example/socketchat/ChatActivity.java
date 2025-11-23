package com.example.socketchat;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class ChatActivity extends AppCompatActivity {

    TextView tvChatTitle;
    RecyclerView rvMessages;
    EditText edtMessage;
    Button btnSend, btnBack;
    String targetNickname, targetPhone, myPhone;
    List<Message> messages;
    MessageAdapter msgAdapter;

    OkHttpClient wsClient;
    WebSocket webSocket;

    UserDBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        tvChatTitle = findViewById(R.id.tvChatTitle);
        rvMessages = findViewById(R.id.rvMessages);
        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);

        dbHelper = new UserDBHelper(this);

        // MainActivity에서 넘어온 값 받기
        targetNickname = getIntent().getStringExtra("targetNickname");
        targetPhone = getIntent().getStringExtra("targetPhone");
        if (targetNickname == null) targetNickname = "알 수 없는 사용자";

        tvChatTitle.setText(targetNickname + " 님과의 대화");

        // 로그인 시 저장해둔 내 전화번호 가져오기
        SharedPreferences prefs = getSharedPreferences("chat_prefs", MODE_PRIVATE);
        myPhone = prefs.getString("current_phone", null);

        if (myPhone == null) {
            Toast.makeText(this,
                    "로그인 정보가 없습니다. 다시 로그인하세요.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // RecyclerView + 메시지 리스트 준비
        messages = new ArrayList<>();
        msgAdapter = new MessageAdapter(messages);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // 아래부터 쌓이도록
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(msgAdapter);

        // ★ DB에서 이 대화방 히스토리 로드
        loadMessagesFromDb();

        // WebSocket 연결
        connectWebSocket();

        // 전송 버튼
        btnSend.setOnClickListener(v -> {
            String msgText = edtMessage.getText().toString().trim();
            if (TextUtils.isEmpty(msgText)) {
                return;
            }

            // UI에 내 메시지 먼저 추가
            Message myMsg = new Message(msgText, true);
            messages.add(myMsg);
            msgAdapter.notifyItemInserted(messages.size() - 1);
            rvMessages.scrollToPosition(messages.size() - 1);
            edtMessage.setText("");

            // ★ DB에 내 메시지 저장
            saveMessageToDb(myPhone, targetPhone, myPhone, msgText);

            // 서버로 전송 (JSON 형식)
            if (webSocket != null) {
                try {
                    JSONObject json = new JSONObject();
                    json.put("type", "message");
                    json.put("to", targetPhone);        // 상대 전화번호
                    json.put("text", msgText);
                    json.put("clientMsgId", String.valueOf(System.currentTimeMillis()));
                    webSocket.send(json.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(ChatActivity.this,
                            "메시지 전송 중 오류가 발생했습니다.",
                            Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(ChatActivity.this,
                        "서버와의 연결이 없습니다.",
                        Toast.LENGTH_LONG).show();
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * 현재 대화방의 과거 메시지를 DB에서 읽어서 messages 리스트에 채워 넣는다.
     * (myPhone, targetPhone 기준)
     */
    private void loadMessagesFromDb() {
        if (myPhone == null || targetPhone == null) return;

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery(
                    "SELECT sender_phone, content FROM " + UserDBHelper.TABLE_MESSAGES +
                            " WHERE my_phone=? AND partner_phone=? " +
                            " ORDER BY created_at ASC, _id ASC",
                    new String[]{myPhone, targetPhone}
            );

            messages.clear();

            while (cursor.moveToNext()) {
                String sender = cursor.getString(0);
                String content = cursor.getString(1);
                boolean isMe = myPhone.equals(sender);
                messages.add(new Message(content, isMe));
            }

            msgAdapter.notifyDataSetChanged();
            if (!messages.isEmpty()) {
                rvMessages.scrollToPosition(messages.size() - 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }

    /**
     * 메시지 1건을 DB에 저장
     *
     * @param myPhone      내 전화번호
     * @param partnerPhone 상대 전화번호
     * @param senderPhone  실제 보낸 사람 전화번호 (myPhone 또는 partnerPhone)
     * @param content      메시지 내용
     */
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

    // WebSocket 연결
    private void connectWebSocket() {
        wsClient = new OkHttpClient();

        String wsUrl = "ws://10.0.2.2:8000/ws/chat?userId=" + myPhone;

        Request request = new Request.Builder()
                .url(wsUrl)
                .build();

        webSocket = wsClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                runOnUiThread(() ->
                        Toast.makeText(ChatActivity.this,
                                "채팅 서버에 연결되었습니다.",
                                Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                // 서버에서 온 메시지(JSON) 처리
                handleIncomingMessage(text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                // 바이너리는 사용 안 함
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                t.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(ChatActivity.this,
                                "서버 연결 실패: " + t.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }
        });

        // wsClient.dispatcher().executorService().shutdown();  // 여러 액티비티에서 재사용할 거면 닫지 말 것
    }

    private void handleIncomingMessage(String text) {
        try {
            JSONObject json = new JSONObject(text);
            String type = json.optString("type", "");

            if ("message".equals(type)) {
                String from = json.optString("from");
                String to = json.optString("to");
                String msgText = json.optString("text", "");

                // 이 채팅방에서 보는 건 "상대 → 나" 메시지만
                // (from == targetPhone && to == myPhone 인 경우)
                if (targetPhone != null && myPhone != null &&
                        targetPhone.equals(from) && myPhone.equals(to)) {

                    ensureChatPartnerInDb(myPhone, from);

                    saveMessageToDb(myPhone, targetPhone, from, msgText);

                    runOnUiThread(() -> {
                        Message otherMsg = new Message(msgText, false);
                        messages.add(otherMsg);
                        msgAdapter.notifyItemInserted(messages.size() - 1);
                        rvMessages.scrollToPosition(messages.size() - 1);
                    });
                }

            }
            // type이 "system", "typing", "delivered" 등인 경우는 지금은 무시
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocket != null) {
            webSocket.close(1000, "Activity destroyed");
            webSocket = null;
        }
        if (wsClient != null) {
            // wsClient.dispatcher().executorService().shutdown(); // 필요 시
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
}
