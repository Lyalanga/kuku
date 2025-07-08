package com.example.fowltyphoidmonitor.data.api;

import com.example.fowltyphoidmonitor.data.models.DiseaseInfo;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface DiseaseInfoApi {
    @GET("diseaseinfo")
    Call<List<DiseaseInfo>> getAllDiseaseInfo();

    // Supabase expects filtering via query param: ?category=eq.symptoms
    @GET("diseaseinfo")
    Call<List<DiseaseInfo>> getDiseaseInfoByCategory(@Query("category") String categoryFilter);

    @POST("diseaseinfo")
    Call<Void> saveDiseaseInfo(@Body DiseaseInfo info);
}
