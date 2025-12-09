package com.example.socketchat.network;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit;

    // FastAPIServer 주소 (에뮬레이터 기준)
    private static final String BASE_URL = "http://10.0.2.2:8000/";

    public static Retrofit getClient() {
        if (retrofit == null) {

            // 타임아웃 설정한 OkHttpClient
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    // 서버에 연결 시도 시간
                    .connectTimeout(10, TimeUnit.SECONDS)
                    // 요청 본문 전송 시간
                    .writeTimeout(10, TimeUnit.SECONDS)
                    // 서버 응답 대기 시간 (팩트체크용으로 넉넉하게)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient) // ← 타임아웃 설정 적용
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
