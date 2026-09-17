package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class CareTaskWithPet(
    @Embedded
    val task: CareTaskEntity,

    @Relation(
        parentColumn = "petId",
        entityColumn = "petId"
    )
    val pet: PetEntity?
)
