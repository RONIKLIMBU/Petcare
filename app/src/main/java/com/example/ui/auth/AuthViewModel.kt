package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.UserEntity
import com.example.data.repository.PetCareRepository
import com.example.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: UserEntity) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val repository: PetCareRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter both username and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val user = repository.getUserByUsername(username.trim())
                if (user != null && user.password_hash == password.trim()) {
                    sessionManager.createLoginSession(user.id, user.username, user.email)
                    _uiState.value = AuthUiState.Success(user)
                } else {
                    _uiState.value = AuthUiState.Error("Invalid username or password")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Login failed")
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all fields")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address")
            return
        }

        if (password.length < 4) {
            _uiState.value = AuthUiState.Error("Password must be at least 4 characters")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val existingUser = repository.getUserByUsername(username.trim())
                if (existingUser != null) {
                    _uiState.value = AuthUiState.Error("Username already taken")
                    return@launch
                }

                val existingEmail = repository.getUserByEmail(email.trim())
                if (existingEmail != null) {
                    _uiState.value = AuthUiState.Error("Email already registered")
                    return@launch
                }

                val newUser = UserEntity(
                    username = username.trim(),
                    email = email.trim(),
                    password_hash = password.trim()
                )
                val newId = repository.registerUser(newUser)
                val createdUser = newUser.copy(id = newId)
                sessionManager.createLoginSession(newId, createdUser.username, createdUser.email)
                _uiState.value = AuthUiState.Success(createdUser)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Registration failed")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
