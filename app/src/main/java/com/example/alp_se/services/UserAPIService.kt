package com.example.alp_se.services


import com.example.alp_se.models.GeneralResponseModel
import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

interface UserAPIService {
    @PUT("api/logout")
    fun logout(@Header("X-API-TOKEN") token: String): Call<GeneralResponseModel>
}
