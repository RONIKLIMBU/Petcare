package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CareTaskEntity
import com.example.data.local.entity.PetEntity
import com.example.data.local.entity.UserEntity
import com.example.util.SmsHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAppNameString() {
        val appName = context.getString(R.string.app_name)
        assertEquals("PetCare", appName)
    }

    @Test
    fun testUserInsertAndQuery() = runBlocking {
        val userDao = database.userDao()
        val user = UserEntity(
            username = "alice",
            password_hash = "secret123",
            email = "alice@example.com"
        )
        val userId = userDao.insertUser(user)
        assertTrue(userId > 0)

        val fetched = userDao.getUserByUsername("alice")
        assertNotNull(fetched)
        assertEquals("alice@example.com", fetched?.email)
    }

    @Test
    fun testPetAndCareTaskWorkflow() = runBlocking {
        val userDao = database.userDao()
        val petDao = database.petDao()
        val careTaskDao = database.careTaskDao()

        // 1. Create owner
        val userId = userDao.insertUser(
            UserEntity(username = "bob", password_hash = "pass", email = "bob@test.com")
        )

        // 2. Add Pet
        val petId = petDao.insertPet(
            PetEntity(
                ownerId = userId,
                name = "Max",
                species = "Dog",
                breed = "Beagle",
                age = 4,
                weight = 12.0
            )
        )
        assertTrue(petId > 0)

        // 3. Add Care Routine Task
        val taskId = careTaskDao.insertTask(
            CareTaskEntity(
                petId = petId,
                taskName = "Morning Kibble",
                category = "Feeding",
                scheduleTime = "08:00 AM",
                isCompleted = false,
                notes = "1 cup"
            )
        )
        assertTrue(taskId > 0)

        // 4. Complete Task
        careTaskDao.setTaskCompletion(taskId, true)
        val completedTask = careTaskDao.getTaskByIdSync(taskId)
        assertTrue(completedTask?.isCompleted == true)

        // 5. Shake-to-reset: resets all tasks for owner to uncompleted
        val resetCount = careTaskDao.resetAllTasksForOwner(userId)
        assertEquals(1, resetCount)

        val resetTask = careTaskDao.getTaskByIdSync(taskId)
        assertFalse(resetTask?.isCompleted == true)
    }

    @Test
    fun testSmsRoutineFormatter() {
        val pet = PetEntity(
            petId = 1L,
            ownerId = 1L,
            name = "Milo",
            species = "Cat",
            breed = "Tabby",
            age = 2,
            weight = 4.5
        )
        val tasks = listOf(
            CareTaskEntity(
                taskId = 1L,
                petId = 1L,
                taskName = "Salmon Pate",
                category = "Feeding",
                scheduleTime = "07:30 AM",
                isCompleted = false,
                notes = "Half can"
            ),
            CareTaskEntity(
                taskId = 2L,
                petId = 1L,
                taskName = "Ear drops",
                category = "Medication",
                scheduleTime = "08:00 AM",
                isCompleted = false,
                notes = "2 drops in left ear"
            )
        )

        val message = SmsHelper.formatRoutineMessage(pet, tasks)
        assertTrue(message.contains("Milo"))
        assertTrue(message.contains("Cat"))
        assertTrue(message.contains("FEEDING SCHEDULE"))
        assertTrue(message.contains("MEDICATION INSTRUCTIONS"))
        assertTrue(message.contains("Ear drops"))
    }
}
