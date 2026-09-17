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
}
