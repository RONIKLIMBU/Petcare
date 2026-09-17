package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CareTaskDao
import com.example.data.local.dao.PetDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.CareTaskEntity
import com.example.data.local.entity.PetEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [UserEntity::class, PetEntity::class, CareTaskEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun petDao(): PetDao
    abstract fun careTaskDao(): CareTaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "petcare_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: AppDatabase) {
                val userDao = database.userDao()
                val petDao = database.petDao()
                val careTaskDao = database.careTaskDao()

                // Default demo user: demo / demo123
                val demoUserId = userDao.insertUser(
                    UserEntity(
                        username = "demo",
                        password_hash = "demo123",
                        email = "demo@petcare.app"
                    )
                )

                // Default pets
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

                // Default Care Tasks for Buddy
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
                        notes = "Use harness and red leash. 30 minutes in the park."
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

                // Default Care Tasks for Luna
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
}
