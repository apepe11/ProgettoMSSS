package com.example.progetto.data.repositories

import com.example.progetto.data.RetrofitClient
import com.example.progetto.utils.InsightsResponse
import retrofit2.Response

/**
 * Repository handling Insight and Statistical data operations.
 */
class InsightsRepository {
    private val playlistApi = RetrofitClient.playlistApiService

    suspend fun getInsights(userId: String): Response<InsightsResponse> {
        return playlistApi.getInsights(userId)
    }
}