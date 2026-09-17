package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pets WHERE ownerId = :ownerId ORDER BY name ASC")
    fun getPetsForOwner(ownerId: Long): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets WHERE ownerId = :ownerId ORDER BY name ASC")
    suspend fun getPetsForOwnerSync(ownerId: Long): List<PetEntity>

    @Query("SELECT * FROM pets WHERE petId = :petId LIMIT 1")
    fun getPetById(petId: Long): Flow<PetEntity?>

    @Query("SELECT * FROM pets WHERE petId = :petId LIMIT 1")
    suspend fun getPetByIdSync(petId: Long): PetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: PetEntity): Long

    @Update
    suspend fun updatePet(pet: PetEntity)

    @Delete
    suspend fun deletePet(pet: PetEntity)

    @Query("DELETE FROM pets WHERE petId = :petId")
    suspend fun deletePetById(petId: Long)

    @Query("SELECT COUNT(*) FROM pets WHERE ownerId = :ownerId")
    fun getPetCount(ownerId: Long): Flow<Int>
}
