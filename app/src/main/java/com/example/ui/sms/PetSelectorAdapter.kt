package com.example.ui.sms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.local.entity.PetEntity
import com.example.databinding.ItemPetSelectorBinding

class PetSelectorAdapter(
    private val onPetSelected: (PetEntity) -> Unit
) : RecyclerView.Adapter<PetSelectorAdapter.PetSelectorViewHolder>() {

    private var pets: List<PetEntity> = emptyList()
    var selectedPetId: Long? = null
        private set

    fun submitList(newPets: List<PetEntity>, currentSelectedId: Long?) {
        pets = newPets
        selectedPetId = currentSelectedId
        notifyDataSetChanged()
    }

    fun setSelected(petId: Long) {
        selectedPetId = petId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetSelectorViewHolder {
        val binding = ItemPetSelectorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PetSelectorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PetSelectorViewHolder, position: Int) {
        holder.bind(pets[position])
    }

    override fun getItemCount(): Int = pets.size

    inner class PetSelectorViewHolder(
        private val binding: ItemPetSelectorBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pet: PetEntity) {
            val context = binding.root.context
            val isSelected = pet.petId == selectedPetId

            binding.tvPetName.text = pet.name
            binding.tvPetDetails.text = if (pet.breed.isNotBlank()) {
                "${pet.species} • ${pet.breed}"
            } else {
                pet.species
            }

            if (isSelected) {
                binding.cardPetSelector.strokeColor = ContextCompat.getColor(context, R.color.primary)
                binding.cardPetSelector.strokeWidth = (2 * context.resources.displayMetrics.density).toInt()
                binding.cardPetSelector.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.primary_container)
                )
                binding.ivCheckMark.visibility = View.VISIBLE
                binding.tvPetName.setTextColor(ContextCompat.getColor(context, R.color.on_primary_container))
                binding.tvPetDetails.setTextColor(ContextCompat.getColor(context, R.color.on_primary_container))
            } else {
                binding.cardPetSelector.strokeColor = ContextCompat.getColor(context, R.color.outline_variant)
                binding.cardPetSelector.strokeWidth = (1.5 * context.resources.displayMetrics.density).toInt()
                binding.cardPetSelector.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.surface)
                )
                binding.ivCheckMark.visibility = View.GONE
                binding.tvPetName.setTextColor(ContextCompat.getColor(context, R.color.on_surface))
                binding.tvPetDetails.setTextColor(ContextCompat.getColor(context, R.color.secondary))
            }

            binding.cardPetSelector.setOnClickListener {
                val previousId = selectedPetId
                if (previousId != pet.petId) {
                    selectedPetId = pet.petId
                    notifyDataSetChanged()
                    onPetSelected(pet)
                }
            }
        }
    }
}
