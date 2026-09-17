package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.PetCareRepository
import com.example.data.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class PetCareApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy {
        PetCareRepository(
            database.userDao(),
            database.petDao(),
            database.careTaskDao()
        )
    }
    val sessionManager by lazy { SessionManager(this) }
}
