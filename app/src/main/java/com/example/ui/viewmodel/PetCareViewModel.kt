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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PetCareViewModel(
    private val repository: PetCareRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val currentUserId: Long get() = sessionManager.getUserId()
    val currentUsername: String get() = sessionManager.getUsername()

    // Pets stream
    val pets: StateFlow<List<PetEntity>> = repository.getPetsForOwner(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filter by pet ID (null = show all)
    private val _selectedPetFilter = MutableStateFlow<Long?>(null)
    val selectedPetFilter: StateFlow<Long?> = _selectedPetFilter.asStateFlow()

    // Tasks stream based on filter
    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<CareTaskWithPet>> = _selectedPetFilter.flatMapLatest { petId ->
        if (petId == null) {
            repository.getAllTasksForOwner(currentUserId)
        } else {
            repository.getTasksForPetAndOwner(currentUserId, petId)
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
        if (name.isBlank() || species.isBlank()) return
        viewModelScope.launch {
            val pet = PetEntity(
                ownerId = currentUserId,
                name = name.trim(),
                species = species.trim(),
                breed = breed.trim(),
                age = age,
                weight = weight
            )
            repository.insertPet(pet)
            _eventFlow.emit("Added ${pet.name} successfully!")
        }
    }

    fun updatePet(pet: PetEntity) {
        viewModelScope.launch {
            repository.updatePet(pet)
            _eventFlow.emit("Updated ${pet.name}")
        }
    }

    fun deletePet(pet: PetEntity) {
        viewModelScope.launch {
            repository.deletePet(pet)
            _eventFlow.emit("Removed ${pet.name}")
        }
    }

    // --- Care Task CRUD ---

    fun addTask(petId: Long, name: String, category: String, scheduleTime: String, notes: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val task = CareTaskEntity(
                petId = petId,
                taskName = name.trim(),
                category = category.trim(),
                scheduleTime = scheduleTime.trim(),
                notes = notes.trim(),
                isCompleted = false
            )
            repository.insertTask(task)
            _eventFlow.emit("Added task: ${task.taskName}")
        }
    }

    fun updateTask(task: CareTaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
            _eventFlow.emit("Updated task: ${task.taskName}")
        }
    }

    fun deleteTask(task: CareTaskEntity) {
        lastDeletedTask = task
        viewModelScope.launch {
            repository.deleteTask(task)
            _eventFlow.emit("Deleted \"${task.taskName}\"")
        }
    }

    fun undoDeleteTask() {
        val taskToRestore = lastDeletedTask ?: return
        viewModelScope.launch {
            repository.insertTask(taskToRestore)
            lastDeletedTask = null
            _eventFlow.emit("Restored \"${taskToRestore.taskName}\"")
        }
    }

    fun setTaskCompletion(taskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.setTaskCompletion(taskId, isCompleted)
        }
    }

    fun toggleTaskCompletion(task: CareTaskEntity) {
        viewModelScope.launch {
            repository.setTaskCompletion(task.taskId, !task.isCompleted)
        }
    }

    // --- Shake Gesture: Reset all tasks for owner ---
    fun resetAllTasksForToday() {
        viewModelScope.launch {
            val count = repository.resetAllTasksForOwner(currentUserId)
            _eventFlow.emit("Shake detected! Reset $count checklist tasks for today.")
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
    }
}
