package com.example.health_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.health_app.data.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val currentUser: FirebaseUser? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(currentUser = repository.currentUser))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.login(email.trim(), password) }
                .onSuccess { user ->
                    _uiState.update { it.copy(currentUser = user, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.localizedMessage) }
                }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.register(email.trim(), password) }
                .onSuccess { user ->
                    _uiState.update { it.copy(currentUser = user, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.localizedMessage) }
                }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
    }
}

class AuthViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


