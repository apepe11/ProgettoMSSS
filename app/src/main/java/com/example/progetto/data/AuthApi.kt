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

    // ✅ FIXED: Changed Response<Unit> to Response<SongReviewResponse>
    @POST("/api/reviews")
    suspend fun saveReview(
        @Body request: SongReviewRequest
    ): Response<SongReviewResponse>

    @GET("/api/users/{user_id}/reviews")
    suspend fun getReviews(
        @Path("user_id") userId: String
    ): Response<UserReviewsResponse>
}