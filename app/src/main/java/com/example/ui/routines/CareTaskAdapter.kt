package com.example.ui.routines

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.local.entity.CareTaskEntity
import com.example.data.local.entity.CareTaskWithPet
import com.example.databinding.ItemCareTaskBinding

class CareTaskAdapter(
    private val onToggleCompletion: (CareTaskEntity) -> Unit,
    private val onEditTask: (CareTaskWithPet) -> Unit
) : ListAdapter<CareTaskWithPet, CareTaskAdapter.CareTaskViewHolder>(TaskDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CareTaskViewHolder {
        val binding = ItemCareTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CareTaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CareTaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getTaskWithPetAt(position: Int): CareTaskWithPet = getItem(position)

    inner class CareTaskViewHolder(
        private val binding: ItemCareTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CareTaskWithPet) {
            val task = item.task
            val pet = item.pet

            binding.tvTaskName.text = task.taskName
            binding.tvScheduleTime.text = task.scheduleTime

            // Pet name badge
            binding.tvPetBadge.text = if (pet != null) "🐾 ${pet.name}" else "🐾 Pet"

            // Category styling
            binding.tvCategoryBadge.text = task.category
            applyCategoryStyle(task.category)

            // Notes
            if (task.notes.isNotBlank()) {
                binding.tvNotes.text = task.notes
                binding.tvNotes.visibility = View.VISIBLE
            } else {
                binding.tvNotes.visibility = View.GONE
            }

            // Checkbox and completion state
            binding.cbCompleted.setOnCheckedChangeListener(null)
            binding.cbCompleted.isChecked = task.isCompleted

            if (task.isCompleted) {
                binding.tvTaskName.paintFlags =
                    binding.tvTaskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvTaskName.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.outline)
                )
                binding.cardTask.alpha = 0.75f
            } else {
                binding.tvTaskName.paintFlags =
                    binding.tvTaskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvTaskName.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.on_surface)
                )
                binding.cardTask.alpha = 1.0f
            }

            binding.cbCompleted.setOnCheckedChangeListener { _, _ ->
                onToggleCompletion(task)
            }

            binding.btnEditTask.setOnClickListener {
                onEditTask(item)
            }
        }

        private fun applyCategoryStyle(category: String) {
            val context = binding.root.context
            val (textColor, bgColor) = when (category.lowercase()) {
                "feeding" -> Pair(R.color.cat_feeding, R.color.cat_feeding_bg)
                "medication" -> Pair(R.color.cat_medication, R.color.cat_medication_bg)
                "grooming" -> Pair(R.color.cat_grooming, R.color.cat_grooming_bg)
                "walking" -> Pair(R.color.cat_walking, R.color.cat_walking_bg)
                "vet", "veterinary" -> Pair(R.color.cat_vet, R.color.cat_vet_bg)
                else -> Pair(R.color.primary, R.color.primary_container)
            }
            binding.tvCategoryBadge.setTextColor(ContextCompat.getColor(context, textColor))
            binding.tvCategoryBadge.setBackgroundColor(ContextCompat.getColor(context, bgColor))
        }
    }

    object TaskDiffCallback : DiffUtil.ItemCallback<CareTaskWithPet>() {
        override fun areItemsTheSame(oldItem: CareTaskWithPet, newItem: CareTaskWithPet): Boolean =
            oldItem.task.taskId == newItem.task.taskId

        override fun areContentsTheSame(oldItem: CareTaskWithPet, newItem: CareTaskWithPet): Boolean =
            oldItem == newItem
    }
}
