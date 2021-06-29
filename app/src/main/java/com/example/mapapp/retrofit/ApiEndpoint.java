package com.example.mapapp.retrofit;

import com.example.mapapp.ModelAPI;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiEndpoint {
    @GET("cluster2016")
    Call<List<ModelAPI>> getCluster2016();
}
