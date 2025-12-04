package kr.ac.baekseok.android_teamwork.network;

import kr.ac.baekseok.android_teamwork.models.AnalyzeRequest;
import kr.ac.baekseok.android_teamwork.models.AnalyzeResponse;
import kr.ac.baekseok.android_teamwork.models.NewsResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @GET("news")
    Call<NewsResponse> searchNews(
            @Query("q") String query,
            @Query("display") int display
    );

    @POST("analyze")
    Call<AnalyzeResponse> analyzeText(
            @Body AnalyzeRequest body
    );
}
