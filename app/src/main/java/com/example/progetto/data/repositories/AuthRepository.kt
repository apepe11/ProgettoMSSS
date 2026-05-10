package com.example.progetto.data.repositories

import com.example.progetto.data.*
import retrofit2.Response

/**
 * Repository handling Authentication data operations.
 * Separates the ViewModel from the data source (Retrofit).
 */
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

    suspend fun saveReview(request: SongReviewRequest): Response<SongReviewResponse> {
        return authApi.saveReview(request)
    }
}