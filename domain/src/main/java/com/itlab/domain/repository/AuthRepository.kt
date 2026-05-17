package com.itlab.domain.repository

interface AuthRepository {
    fun getCurrentUserId(): String?

    suspend fun signOut(): Result<Unit>
}
