package com.itlab.notes.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.itlab.notes.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun authScreen(
    viewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val webClientId = stringResource(R.string.default_web_client_id)
    val googleSignInEnabled = webClientId.isNotBlank()

    val googleSignInClient =
        remember(context, webClientId) {
            val options =
                GoogleSignInOptions
                    .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
            GoogleSignIn.getClient(context, options)
        }

    val googleLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Google Sign-In may return RESULT_CANCELED even after a successful account pick.
            val data = result.data
            if (data == null) {
                if (result.resultCode == Activity.RESULT_CANCELED) {
                    viewModel.clearError()
                } else {
                    viewModel.reportError("Google sign-in returned no data.")
                }
                return@rememberLauncherForActivityResult
            }
            try {
                val account =
                    GoogleSignIn
                        .getSignedInAccountFromIntent(data)
                        .getResult(ApiException::class.java)
                val token = account.idToken
                if (token.isNullOrBlank()) {
                    viewModel.reportError(
                        "Google sign-in did not return a token. Check Web Client ID in Firebase.",
                    )
                } else {
                    viewModel.signInWithGoogle(token)
                }
            } catch (error: ApiException) {
                if (error.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                    viewModel.clearError()
                } else {
                    viewModel.reportError(mapGoogleSignInError(error))
                }
            }
        }

    Scaffold { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.auth_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.auth_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    viewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.auth_email_label)) },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                enabled = !state.isLoading,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.auth_password_label)) },
                singleLine = true,
                visualTransformation =
                    if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector =
                                if (passwordVisible) {
                                    Icons.Rounded.VisibilityOff
                                } else {
                                    Icons.Rounded.Visibility
                                },
                            contentDescription =
                                if (passwordVisible) {
                                    stringResource(R.string.auth_hide_password)
                                } else {
                                    stringResource(R.string.auth_show_password)
                                },
                        )
                    }
                },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                enabled = !state.isLoading,
            )

            state.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (state.isSignUpMode) {
                        viewModel.signUpWithEmail(email, password)
                    } else {
                        viewModel.signInWithEmail(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text =
                            if (state.isSignUpMode) {
                                stringResource(R.string.auth_create_account)
                            } else {
                                stringResource(R.string.auth_sign_in)
                            },
                    )
                }
            }

            TextButton(
                onClick = { viewModel.toggleSignUpMode() },
                enabled = !state.isLoading,
            ) {
                Text(
                    text =
                        if (state.isSignUpMode) {
                            stringResource(R.string.auth_already_have_account)
                        } else {
                            stringResource(R.string.auth_need_account)
                        },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    viewModel.clearError()
                    googleLauncher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && googleSignInEnabled,
            ) {
                Text(stringResource(R.string.auth_google))
            }

            if (!googleSignInEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.auth_google_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { viewModel.continueOffline() },
                enabled = !state.isLoading,
            ) {
                Text(stringResource(R.string.auth_continue_offline))
            }
        }
    }
}

private fun mapGoogleSignInError(error: ApiException): String =
    when (error.statusCode) {
        GoogleSignInStatusCodes.DEVELOPER_ERROR ->
            "Google Sign-In configuration error. Add SHA-1 fingerprint in Firebase Console."
        else -> error.message ?: "Google sign-in failed (code ${error.statusCode})."
    }
