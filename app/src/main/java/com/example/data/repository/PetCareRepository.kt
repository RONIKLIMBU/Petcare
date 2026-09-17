package com.example.data.repository

import com.example.data.local.dao.CareTaskDao
import com.example.data.local.dao.PetDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.CareTaskEntity
import com.example.data.local.entity.CareTaskWithPet
import com.example.data.local.entity.PetEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PetCareRepository(
    private val userDao: UserDao,
    private val petDao: PetDao,
    private val careTaskDao: CareTaskDao
) {
    // Auth operations
    suspend fun getUserByUsername(username: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserByUsername(username)
    }

    suspend fun getUserByEmail(email: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserByEmail(email)
    }

    suspend fun registerUser(user: UserEntity): Long = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    fun getUserById(userId: Long): Flow<UserEntity?> = userDao.getUserById(userId)

    // Pet operations
    fun getPetsForOwner(ownerId: Long): Flow<List<PetEntity>> =
        petDao.getPetsForOwner(ownerId)

    suspend fun getPetsForOwnerSync(ownerId: Long): List<PetEntity> = withContext(Dispatchers.IO) {
        petDao.getPetsForOwnerSync(ownerId)
    }

    fun getPetById(petId: Long): Flow<PetEntity?> =
        petDao.getPetById(petId)

    suspend fun getPetByIdSync(petId: Long): PetEntity? = withContext(Dispatchers.IO) {
        petDao.getPetByIdSync(petId)
    }

    suspend fun insertPet(pet: PetEntity): Long = withContext(Dispatchers.IO) {
        petDao.insertPet(pet)
    }

    suspend fun updatePet(pet: PetEntity) = withContext(Dispatchers.IO) {
        petDao.updatePet(pet)
    }

    suspend fun deletePet(pet: PetEntity) = withContext(Dispatchers.IO) {
        petDao.deletePet(pet)
    }

    suspend fun deletePetById(petId: Long) = withContext(Dispatchers.IO) {
        petDao.deletePetById(petId)
    }

    // Care Task operations
    fun getAllTasksForOwner(ownerId: Long): Flow<List<CareTaskWithPet>> =
        careTaskDao.getAllTasksForOwner(ownerId)

    fun getTasksForPetAndOwner(ownerId: Long, petId: Long): Flow<List<CareTaskWithPet>> =
        careTaskDao.getTasksForPetAndOwner(ownerId, petId)

    fun getTasksForPet(petId: Long): Flow<List<CareTaskEntity>> =
        careTaskDao.getTasksForPet(petId)

    suspend fun getTasksForPetSync(petId: Long): List<CareTaskEntity> = withContext(Dispatchers.IO) {
        careTaskDao.getTasksForPetSync(petId)
    }

    suspend fun insertTask(task: CareTaskEntity): Long = withContext(Dispatchers.IO) {
        careTaskDao.insertTask(task)
    }

    suspend fun updateTask(task: CareTaskEntity) = withContext(Dispatchers.IO) {
        careTaskDao.updateTask(task)
    }

    suspend fun deleteTask(task: CareTaskEntity) = withContext(Dispatchers.IO) {
        careTaskDao.deleteTask(task)
    }

    suspend fun deleteTaskById(taskId: Long) = withContext(Dispatchers.IO) {
        careTaskDao.deleteTaskById(taskId)
    }

    suspend fun setTaskCompletion(taskId: Long, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        careTaskDao.setTaskCompletion(taskId, isCompleted)
    }

    suspend fun resetAllTasksForOwner(ownerId: Long): Int = withContext(Dispatchers.IO) {
        careTaskDao.resetAllTasksForOwner(ownerId)
    }

    suspend fun ensureInitialData() = withContext(Dispatchers.IO) {
        val existingDemo = userDao.getUserByUsername("demo")
        if (existingDemo == null) {
            val demoUserId = userDao.insertUser(
                UserEntity(
                    username = "demo",
                    password_hash = "demo123",
                    email = "demo@petcare.app"
                )
            )

            val buddyId = petDao.insertPet(
                PetEntity(
                    ownerId = demoUserId,
                    name = "Buddy",
                    species = "Dog",
                    breed = "Golden Retriever",
                    age = 3,
                    weight = 28.5
                )
            )

            val lunaId = petDao.insertPet(
                PetEntity(
                    ownerId = demoUserId,
                    name = "Luna",
                    species = "Cat",
                    breed = "Siamese",
                    age = 2,
                    weight = 4.2
                )
            )

            careTaskDao.insertTask(
                CareTaskEntity(
                    petId = buddyId,
                    taskName = "Morning Kibble & Omega-3",
                    category = "Feeding",
                    scheduleTime = "08:00 AM",
                    isCompleted = false,
                    notes = "1.5 cups dry food mixed with salmon oil supplement"
                )
            )

            careTaskDao.insertTask(
                CareTaskEntity(
                    petId = buddyId,
                    taskName = "Neighborhood Walk & Fetch",
                    category = "Walking",
                    scheduleTime = "09:30 AM",
                    isCompleted = false,
                    notes = "Use harness and red leash. 30 minutes in park."
                )
            )

            careTaskDao.insertTask(
                CareTaskEntity(
                    petId = buddyId,
                    taskName = "Joint Health Chew",
                    category = "Medication",
                    scheduleTime = "01:00 PM",
                    isCompleted = false,
                    notes = "1 Glucosamine tablet after afternoon meal"
                )
            )

            careTaskDao.insertTask(
                CareTaskEntity(
                    petId = lunaId,
                    taskName = "Wet Food & Fresh Water",
                    category = "Feeding",
                    scheduleTime = "08:30 AM",
                    isCompleted = false,
                    notes = "Half can salmon pate, clean water fountain"
                )
            )

            careTaskDao.insertTask(
                CareTaskEntity(
                    petId = lunaId,
                    taskName = "Coat Brushing",
                    category = "Grooming",
                    scheduleTime = "06:00 PM",
                    isCompleted = false,
                    notes = "Gentle de-shedding brush on back and belly"
                )
            )
        }
    }
}
