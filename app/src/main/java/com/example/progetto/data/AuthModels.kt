package com.example.progetto.data

import com.google.gson.annotations.SerializedName

/**
 * L'oggetto che inviamo al backend Python per il login
 */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * L'oggetto che inviamo al backend per registrare un nuovo utente
 */
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    @SerializedName("device_id") val deviceId: String
)

/**
 * L'oggetto che il backend ci restituisce in caso di successo
 * Usiamo String per userId perché il backend usa UUID.
 * Rendiamo username opzionale perché non sempre presente nella risposta di registrazione.
 */
data class LoginResponse(
    val message: String,
    @SerializedName("user_id") val userId: String,
    val username: String? = null
)

/**
 * L'oggetto per gli errori
 */
data class ErrorResponse(
    val error: String
)
