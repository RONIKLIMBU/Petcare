package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.CareTaskEntity
import com.example.data.local.entity.CareTaskWithPet
import kotlinx.coroutines.flow.Flow

@Dao
interface CareTaskDao {
    @Transaction
    @Query("""
        SELECT care_tasks.* FROM care_tasks 
        INNER JOIN pets ON care_tasks.petId = pets.petId 
        WHERE pets.ownerId = :ownerId 
        ORDER BY care_tasks.isCompleted ASC, care_tasks.scheduleTime ASC
    """)
    fun getAllTasksForOwner(ownerId: Long): Flow<List<CareTaskWithPet>>

    @Transaction
    @Query("""
        SELECT care_tasks.* FROM care_tasks 
        INNER JOIN pets ON care_tasks.petId = pets.petId 
        WHERE pets.ownerId = :ownerId AND care_tasks.petId = :petId
        ORDER BY care_tasks.isCompleted ASC, care_tasks.scheduleTime ASC
    """)
    fun getTasksForPetAndOwner(ownerId: Long, petId: Long): Flow<List<CareTaskWithPet>>

    @Query("SELECT * FROM care_tasks WHERE petId = :petId ORDER BY scheduleTime ASC")
    fun getTasksForPet(petId: Long): Flow<List<CareTaskEntity>>

    @Query("SELECT * FROM care_tasks WHERE petId = :petId ORDER BY scheduleTime ASC")
    suspend fun getTasksForPetSync(petId: Long): List<CareTaskEntity>

    @Transaction
    @Query("SELECT * FROM care_tasks WHERE taskId = :taskId LIMIT 1")
    fun getTaskWithPetById(taskId: Long): Flow<CareTaskWithPet?>

    @Query("SELECT * FROM care_tasks WHERE taskId = :taskId LIMIT 1")
    suspend fun getTaskByIdSync(taskId: Long): CareTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: CareTaskEntity): Long

    @Update
    suspend fun updateTask(task: CareTaskEntity)

    @Delete
    suspend fun deleteTask(task: CareTaskEntity)

    @Query("DELETE FROM care_tasks WHERE taskId = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("UPDATE care_tasks SET isCompleted = :isCompleted WHERE taskId = :taskId")
    suspend fun setTaskCompletion(taskId: Long, isCompleted: Boolean)

    @Query("""
        UPDATE care_tasks 
        SET isCompleted = 0 
        WHERE petId IN (SELECT petId FROM pets WHERE ownerId = :ownerId)
    """)
    suspend fun resetAllTasksForOwner(ownerId: Long): Int
}
