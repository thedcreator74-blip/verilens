package com.example.data.network

import com.example.data.network.models.VerificationRequest
import com.example.data.network.models.VerificationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Prepared Retrofit service interface for future AI / backend verification engine.
 * No implementation or live backend is active in this foundation stage.
 */
interface VerificationApiService {

    @POST("v1/verify")
    suspend fun verifyContent(
        @Body request: VerificationRequest
    ): Response<VerificationResponse>

    @GET("v1/reports/{reportId}")
    suspend fun getReport(
        @Path("reportId") reportId: String
    ): Response<VerificationResponse>
}
