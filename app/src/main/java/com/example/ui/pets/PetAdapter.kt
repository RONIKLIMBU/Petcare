package com.example.ui.pets

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.data.local.entity.PetEntity
import com.example.databinding.ItemPetCardBinding

class PetAdapter(
    private val onEditClick: (PetEntity) -> Unit,
    private val onDeleteClick: (PetEntity) -> Unit
) : ListAdapter<PetEntity, PetAdapter.PetViewHolder>(PetDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val binding = ItemPetCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PetViewHolder(
        private val binding: ItemPetCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pet: PetEntity) {
            binding.tvPetName.text = pet.name
            binding.tvPetSpeciesBreed.text = "${pet.species} • ${pet.breed.ifBlank { "Companion" }}"
            binding.tvPetAge.text = "${pet.age} yrs old"
            binding.tvPetWeight.text = "${pet.weight} kg"

            binding.tvPetAvatar.text = when (pet.species.lowercase()) {
                "dog" -> "🐶"
                "cat" -> "🐱"
                "bird" -> "🦜"
                "rabbit" -> "🐰"
                "hamster" -> "🐹"
                "fish" -> "🐠"
                else -> "🐾"
            }

            binding.btnEditPet.setOnClickListener {
                onEditClick(pet)
            }

            binding.btnDeletePet.setOnClickListener {
                onDeleteClick(pet)
            }
        }
    }

    object PetDiffCallback : DiffUtil.ItemCallback<PetEntity>() {
        override fun areItemsTheSame(oldItem: PetEntity, newItem: PetEntity): Boolean =
            oldItem.petId == newItem.petId

        override fun areContentsTheSame(oldItem: PetEntity, newItem: PetEntity): Boolean =
            oldItem == newItem
    }
}
