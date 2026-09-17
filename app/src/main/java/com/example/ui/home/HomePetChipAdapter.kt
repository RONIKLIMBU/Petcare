package com.example.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.data.local.entity.PetEntity
import com.example.databinding.ItemHomePetChipBinding

class HomePetChipAdapter(
    private val onPetClicked: (PetEntity) -> Unit
) : ListAdapter<PetEntity, HomePetChipAdapter.PetChipViewHolder>(PetDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetChipViewHolder {
        val binding = ItemHomePetChipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PetChipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PetChipViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PetChipViewHolder(
        private val binding: ItemHomePetChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pet: PetEntity) {
            binding.tvPetName.text = pet.name
            binding.tvPetSpeciesBreed.text = "${pet.species} • ${pet.breed.ifBlank { "Pet" }}"
            binding.tvPetAgeWeight.text = "${pet.age} yrs • ${pet.weight} kg"

            binding.tvSpeciesEmoji.text = when (pet.species.lowercase()) {
                "dog" -> "🐶"
                "cat" -> "🐱"
                "bird" -> "🦜"
                "rabbit" -> "🐰"
                "hamster" -> "🐹"
                "fish" -> "🐠"
                else -> "🐾"
            }

            binding.root.setOnClickListener {
                onPetClicked(pet)
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
