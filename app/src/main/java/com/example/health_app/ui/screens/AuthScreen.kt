package com.example.health_app.ui.screens

import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.health_app.R
import com.example.health_app.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoginMode by rememberSaveable { mutableStateOf(true) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val invalidEmailText = stringResource(R.string.neveljaven_email)
    val shortPasswordText = stringResource(R.string.geslo_prekratko)
    val googleFailedText = stringResource(R.string.google_sign_in_failed)
    val googleNoTokenText = stringResource(R.string.google_sign_in_no_token)
    val googleCancelledText = stringResource(R.string.google_sign_in_cancelled)
    val googleConfigErrorText = stringResource(R.string.google_sign_in_config_error)

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                authViewModel.setError(googleNoTokenText)
            } else {
                authViewModel.signInWithGoogle(idToken)
            }
        } catch (e: ApiException) {
            val message = when (e.statusCode) {
                10 -> googleConfigErrorText
                12501 -> googleCancelledText
                else -> "$googleFailedText (${e.statusCode})"
            }
            authViewModel.setError(message)
        }
    }

    LaunchedEffect(uiState.currentUser) {
        if (uiState.currentUser != null) {
            onAuthSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isLoginMode) stringResource(R.string.prijava) else stringResource(R.string.registracija),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.email)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.geslo)) },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Text(
                            if (showPassword) stringResource(R.string.skrij_geslo)
                            else stringResource(R.string.pokazi_geslo)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val trimmedEmail = email.trim()
                    when {
                        !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> {
                            authViewModel.setError(invalidEmailText)
                        }
                        password.length < 6 -> {
                            authViewModel.setError(shortPasswordText)
                        }
                        isLoginMode -> authViewModel.login(trimmedEmail, password)
                        else -> authViewModel.register(trimmedEmail, password)
                    }
                },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isLoginMode) stringResource(R.string.prijava) else stringResource(R.string.registracija))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    launcher.launch(googleSignInClient.signInIntent)
                },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.height(18.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.prijava_z_google))
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { isLoginMode = !isLoginMode }) {
                Text(
                    if (isLoginMode) stringResource(R.string.nimas_racuna)
                    else stringResource(R.string.imas_racun)
                )
            }
        }
    }
}




