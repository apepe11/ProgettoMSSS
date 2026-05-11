package com.example.progetto.data.repositories

import com.example.progetto.data.*
import retrofit2.Response

class AuthRepository {
    private val authApi = RetrofitClient.authApiService

    suspend fun login(request: LoginRequest): Response<LoginResponse> {
        return authApi.login(request)
    }

    suspend fun register(request: RegisterRequest): Response<LoginResponse> {
        return authApi.register(request)
    }

    suspend fun getReviews(userId: String): Response<UserReviewsResponse> {
        return authApi.getReviews(userId)
    }

    // This is likely where the mismatch was.
    // We make it return exactly what the authApi returns.
    suspend fun saveReview(request: SongReviewRequest): Response<SongReviewResponse> {
        return authApi.saveReview(request)
    }
}