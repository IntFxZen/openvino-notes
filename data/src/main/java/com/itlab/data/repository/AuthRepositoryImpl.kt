package com.itlab.data.repository

import com.itlab.data.cloud.AuthManager
import com.itlab.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val authManager: AuthManager,
) : AuthRepository {
    override fun getCurrentUserId(): String? = authManager.getCurrentUserId()

    override suspend fun signOut(): Result<Unit> =
        runCatching {
            authManager.signOut()
        }
}
