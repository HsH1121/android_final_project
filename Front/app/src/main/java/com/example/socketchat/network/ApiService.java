package com.example.socketchat.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // --- 뉴스 검색 ---
    @GET("news")
    Call<NewsResponse> searchNews(
            @Query("q") String query,
            @Query("display") int display
    );

    // --- 일반 문장 분석 ---
    @POST("analyze")
    Call<AnalyzeResponse> analyzeText(
            @Body AnalyzeRequest body
    );

    // --- 팩트체크 ---
    @POST("fact-check")
    Call<FactCheckResponse> factCheck(
            @Body FactCheckRequest body
    );
}
