package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import com.example.data.local.entity.CareTaskEntity
import com.example.data.local.entity.PetEntity

object SmsHelper {

    fun formatRoutineMessage(pet: PetEntity, tasks: List<CareTaskEntity>): String {
        val sb = StringBuilder()
        sb.append("🐾 PetCare Routine for ${pet.name} (${pet.species}")
        if (pet.breed.isNotBlank()) sb.append(" - ${pet.breed}")
        sb.append("):\n")
        sb.append("Age: ${pet.age} yrs | Weight: ${pet.weight} kg\n\n")

        val feedingTasks = tasks.filter { it.category.equals("Feeding", ignoreCase = true) }
        val medicationTasks = tasks.filter { it.category.equals("Medication", ignoreCase = true) }
        val otherTasks = tasks.filter { 
            !it.category.equals("Feeding", ignoreCase = true) && 
            !it.category.equals("Medication", ignoreCase = true) 
        }

        if (feedingTasks.isNotEmpty()) {
            sb.append("🍽️ FEEDING SCHEDULE:\n")
            feedingTasks.forEach { task ->
                sb.append("• ${task.scheduleTime} - ${task.taskName}")
                if (task.notes.isNotBlank()) sb.append(" (${task.notes})")
                sb.append("\n")
            }
            sb.append("\n")
        }

        if (medicationTasks.isNotEmpty()) {
            sb.append("💊 MEDICATION INSTRUCTIONS:\n")
            medicationTasks.forEach { task ->
                sb.append("• ${task.scheduleTime} - ${task.taskName}")
                if (task.notes.isNotBlank()) sb.append(" (${task.notes})")
                sb.append("\n")
            }
            sb.append("\n")
        }

        if (otherTasks.isNotEmpty()) {
            sb.append("📋 OTHER ROUTINES:\n")
            otherTasks.forEach { task ->
                sb.append("• ${task.scheduleTime} [${task.category}] - ${task.taskName}")
                if (task.notes.isNotBlank()) sb.append(" (${task.notes})")
                sb.append("\n")
            }
            sb.append("\n")
        }

        if (tasks.isEmpty()) {
            sb.append("No active care routines scheduled.\n")
        }

        sb.append("— Sent via PetCare App")
        return sb.toString()
    }

    fun sendDirectSms(context: Context, phoneNumber: String, message: String): Result<Unit> {
        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createSmsIntent(phoneNumber: String, message: String): Intent {
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${Uri.encode(phoneNumber.trim())}")
            putExtra("sms_body", message)
        }
    }
}
