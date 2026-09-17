package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CareTaskEntity
import com.example.data.local.entity.CareTaskWithPet
import com.example.data.local.entity.PetEntity
import com.example.data.repository.PetCareRepository
import com.example.data.session.SessionManager
import com.example.util.SmsHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PetCareViewModel(
    private val repository: PetCareRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // Reactive user session state
    private val _userIdFlow = MutableStateFlow(sessionManager.getUserId())
    val userIdFlow: StateFlow<Long> = _userIdFlow.asStateFlow()

    private val _userNameFlow = MutableStateFlow(sessionManager.getUsername())
    val userNameFlow: StateFlow<String> = _userNameFlow.asStateFlow()

    val currentUserId: Long get() = _userIdFlow.value
    val currentUsername: String get() = _userNameFlow.value

    fun refreshUserSession() {
        _userIdFlow.value = sessionManager.getUserId()
        _userNameFlow.value = sessionManager.getUsername()
    }

    // Filter by pet ID (null = show all)
    private val _selectedPetFilter = MutableStateFlow<Long?>(null)
    val selectedPetFilter: StateFlow<Long?> = _selectedPetFilter.asStateFlow()

    // Pets stream dynamically driven by active user
    @OptIn(ExperimentalCoroutinesApi::class)
    val pets: StateFlow<List<PetEntity>> = _userIdFlow.flatMapLatest { userId ->
        if (userId <= 0L) {
            flowOf(emptyList())
        } else {
            repository.getPetsForOwner(userId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Tasks stream driven by active user and selected pet filter
    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<CareTaskWithPet>> = combine(_userIdFlow, _selectedPetFilter) { userId, petId ->
        Pair(userId, petId)
    }.flatMapLatest { (userId, petId) ->
        if (userId <= 0L) {
            flowOf(emptyList())
        } else if (petId == null) {
            repository.getAllTasksForOwner(userId)
        } else {
            repository.getTasksForPetAndOwner(userId, petId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Events (e.g. snackbar messages, shake triggered)
    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    // Last deleted task for undo capability
    private var lastDeletedTask: CareTaskEntity? = null

    fun setPetFilter(petId: Long?) {
        _selectedPetFilter.value = petId
    }

    // --- Pet CRUD ---

    fun addPet(name: String, species: String, breed: String, age: Int, weight: Double) {
        val trimmedName = name.trim()
        val trimmedSpecies = species.trim()
        if (trimmedName.isBlank() || trimmedSpecies.isBlank()) return

        val ownerId = sessionManager.getUserId()
        if (ownerId <= 0L) {
            viewModelScope.launch {
                _eventFlow.emit("Please sign in before adding a pet.")
            }
            return
        }

        viewModelScope.launch {
            try {
                val pet = PetEntity(
                    ownerId = ownerId,
                    name = trimmedName,
                    species = trimmedSpecies,
                    breed = breed.trim(),
                    age = age,
                    weight = weight
                )
                repository.insertPet(pet)
                _userIdFlow.value = ownerId // trigger emission refresh
                _eventFlow.emit("Added ${pet.name} successfully!")
            } catch (e: Exception) {
                _eventFlow.emit("Failed to add pet: ${e.message}")
            }
        }
    }

    fun updatePet(pet: PetEntity) {
        viewModelScope.launch {
            try {
                repository.updatePet(pet)
                _eventFlow.emit("Updated ${pet.name}")
            } catch (e: Exception) {
                _eventFlow.emit("Failed to update pet: ${e.message}")
            }
        }
    }

    fun deletePet(pet: PetEntity) {
        viewModelScope.launch {
            try {
                repository.deletePet(pet)
                _eventFlow.emit("Removed ${pet.name}")
            } catch (e: Exception) {
                _eventFlow.emit("Failed to delete pet: ${e.message}")
            }
        }
    }

    // --- Care Task CRUD ---

    fun addTask(petId: Long, name: String, category: String, scheduleTime: String, notes: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        viewModelScope.launch {
            try {
                val task = CareTaskEntity(
                    petId = petId,
                    taskName = trimmedName,
                    category = category.trim(),
                    scheduleTime = scheduleTime.trim(),
                    notes = notes.trim(),
                    isCompleted = false
                )
                repository.insertTask(task)
                _eventFlow.emit("Added task: ${task.taskName}")
            } catch (e: Exception) {
                _eventFlow.emit("Failed to add task: ${e.message}")
            }
        }
    }

    fun updateTask(task: CareTaskEntity) {
        viewModelScope.launch {
            try {
                repository.updateTask(task)
                _eventFlow.emit("Updated task: ${task.taskName}")
            } catch (e: Exception) {
                _eventFlow.emit("Failed to update task: ${e.message}")
            }
        }
    }

    fun deleteTask(task: CareTaskEntity) {
        lastDeletedTask = task
        viewModelScope.launch {
            try {
                repository.deleteTask(task)
                _eventFlow.emit("Deleted \"${task.taskName}\"")
            } catch (e: Exception) {
                _eventFlow.emit("Failed to delete task: ${e.message}")
            }
        }
    }

    fun undoDeleteTask() {
        val taskToRestore = lastDeletedTask ?: return
        viewModelScope.launch {
            try {
                repository.insertTask(taskToRestore)
                lastDeletedTask = null
                _eventFlow.emit("Restored \"${taskToRestore.taskName}\"")
            } catch (e: Exception) {
                _eventFlow.emit("Failed to restore task: ${e.message}")
            }
        }
    }

    fun setTaskCompletion(taskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                repository.setTaskCompletion(taskId, isCompleted)
            } catch (e: Exception) {
                _eventFlow.emit("Failed to update task status: ${e.message}")
            }
        }
    }

    fun toggleTaskCompletion(task: CareTaskEntity) {
        viewModelScope.launch {
            try {
                repository.setTaskCompletion(task.taskId, !task.isCompleted)
            } catch (e: Exception) {
                _eventFlow.emit("Failed to update task status: ${e.message}")
            }
        }
    }

    // --- Shake Gesture: Reset all tasks for owner ---
    fun resetAllTasksForToday() {
        val ownerId = _userIdFlow.value
        if (ownerId <= 0L) return
        viewModelScope.launch {
            try {
                val count = repository.resetAllTasksForOwner(ownerId)
                _eventFlow.emit("Shake detected! Reset $count checklist tasks for today.")
            } catch (e: Exception) {
                _eventFlow.emit("Failed to reset tasks: ${e.message}")
            }
        }
    }

    // --- SMS Routine Delegation Helper ---
    suspend fun prepareSmsRoutine(petId: Long): String {
        val pet = repository.getPetByIdSync(petId) ?: return ""
        val tasks = repository.getTasksForPetSync(petId)
        return SmsHelper.formatRoutineMessage(pet, tasks)
    }

    fun logout() {
        sessionManager.logout()
        _userIdFlow.value = -1L
        _userNameFlow.value = ""
        _selectedPetFilter.value = null
    }
}
