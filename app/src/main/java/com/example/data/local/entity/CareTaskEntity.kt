package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "care_tasks",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["petId"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["petId"])]
)
data class CareTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val taskId: Long = 0L,
    val petId: Long,
    val taskName: String,
    val category: String,
    val scheduleTime: String,
    val isCompleted: Boolean = false,
    val notes: String = ""
)
