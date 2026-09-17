package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.PetCareApplication
import com.example.ui.auth.AuthViewModel

class ViewModelFactory(private val app: PetCareApplication) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(app.repository, app.sessionManager) as T
            }
            modelClass.isAssignableFrom(PetCareViewModel::class.java) -> {
                PetCareViewModel(app.repository, app.sessionManager) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
