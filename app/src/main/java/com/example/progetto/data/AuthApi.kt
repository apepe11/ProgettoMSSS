package com.example.progetto.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApi {

    @POST("/api/users/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("/api/users/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<LoginResponse>

    @POST("/api/users/forgot-password")
    suspend fun forgotPassword(
        @Body request: Map<String, String>
    ): Response<Map<String, String>>

    @POST("/api/users/reset-password")
    suspend fun resetPassword(
        @Body request: Map<String, String>
    ): Response<Map<String, String>>

    @POST("/api/reviews")
    suspend fun saveReview(
        @Body request: SongReviewRequest
    ): Response<Unit>

    @GET("/api/users/{user_id}/reviews")
    suspend fun getReviews(
        @Path("user_id") userId: String
    ): Response<UserReviewsResponse>
}
