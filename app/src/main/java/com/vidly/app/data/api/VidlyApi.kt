package com.vidly.app.data.api

import retrofit2.Response
import retrofit2.http.GET

data class HealthResponse(
    val status: String,
    val time: String
)

interface VidlyApi {
    @GET("health")
    suspend fun health(): Response<HealthResponse>
}
