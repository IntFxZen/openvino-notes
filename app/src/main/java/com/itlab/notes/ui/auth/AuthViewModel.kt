package com.itlab.notes.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val isSignedIn: Boolean = false,
    val continueOffline: Boolean = false,
    val isLoading: Boolean = false,
    val isSignUpMode: Boolean = false,
    val errorMessage: String? = null,
)

class AuthViewModel(
    private val firebaseAuth: FirebaseAuth,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState(isSignedIn = firebaseAuth.currentUser != null))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val authStateListener =
        FirebaseAuth.AuthStateListener { auth ->
            _uiState.update {
                it.copy(
                    isSignedIn = auth.currentUser != null,
                    isLoading = false,
                )
            }
        }

    init {
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        firebaseAuth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }

    fun toggleSignUpMode() {
        _uiState.update { it.copy(isSignUpMode = !it.isSignUpMode, errorMessage = null) }
    }

    fun continueOffline() {
        _uiState.update { it.copy(continueOffline = true, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun reportError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    fun signInWithEmail(
        email: String,
        password: String,
    ) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Email and password are required.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                firebaseAuth.signInWithEmailAndPassword(trimmedEmail, password).await()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = mapAuthError(error),
                    )
                }
            }
        }
    }

    fun signUpWithEmail(
        email: String,
        password: String,
    ) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Email and password are required.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = mapAuthError(error),
                    )
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuth.signInWithCredential(credential).await()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = mapAuthError(error),
                    )
                }
            }
        }
    }

    private fun mapAuthError(error: Throwable): String =
        when (error) {
            is FirebaseAuthInvalidUserException ->
                "No account found for this email. Try creating an account."
            is FirebaseAuthInvalidCredentialsException ->
                "Invalid email or password."
            is FirebaseAuthWeakPasswordException ->
                "Password must be at least 6 characters."
            is FirebaseAuthUserCollisionException ->
                "An account with this email already exists. Sign in instead."
            else -> error.message ?: "Authentication failed. Please try again."
        }
}
