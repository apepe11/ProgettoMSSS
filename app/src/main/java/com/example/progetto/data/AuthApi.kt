package com.example.progetto.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/api/users/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("/api/users/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<LoginResponse>
}