package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pets",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["ownerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["ownerId"])]
)
data class PetEntity(
    @PrimaryKey(autoGenerate = true)
    val petId: Long = 0L,
    val ownerId: Long,
    val name: String,
    val species: String,
    val breed: String,
    val age: Int,
    val weight: Double
)
