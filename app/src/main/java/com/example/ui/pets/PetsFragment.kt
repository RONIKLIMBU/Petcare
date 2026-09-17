package com.example.ui.pets

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.PetCareApplication
import com.example.R
import com.example.data.local.entity.PetEntity
import com.example.databinding.DialogAddEditPetBinding
import com.example.databinding.FragmentPetsBinding
import com.example.ui.viewmodel.PetCareViewModel
import com.example.ui.viewmodel.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class PetsFragment : Fragment() {

    private var _binding: FragmentPetsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PetCareViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as PetCareApplication)
    }

    private lateinit var petAdapter: PetAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupRecyclerView() {
        petAdapter = PetAdapter(
            onEditClick = { pet -> showAddEditPetDialog(pet) },
            onDeleteClick = { pet -> showDeletePetConfirmation(pet) }
        )
        binding.rvPets.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPets.adapter = petAdapter
    }

    private fun setupListeners() {
        binding.fabAddPet.setOnClickListener {
            showAddEditPetDialog(null)
        }
        binding.btnAddPetEmpty.setOnClickListener {
            showAddEditPetDialog(null)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pets.collect { pets ->
                    petAdapter.submitList(pets)
                    binding.layoutEmptyPets.visibility = if (pets.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun showAddEditPetDialog(petToEdit: PetEntity?) {
        val dialogBinding = DialogAddEditPetBinding.inflate(layoutInflater)
        val isEditing = petToEdit != null

        dialogBinding.tvDialogTitle.text = if (isEditing) {
            getString(R.string.pets_edit_pet)
        } else {
            getString(R.string.pets_add_pet)
        }

        val speciesList = listOf("Dog", "Cat", "Bird", "Rabbit", "Hamster", "Fish", "Other")
        val speciesAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, speciesList)
        dialogBinding.actvSpecies.setSimpleItems(speciesList.toTypedArray())
        dialogBinding.actvSpecies.setOnClickListener { dialogBinding.actvSpecies.showDropDown() }
        dialogBinding.tilSpecies.setOnClickListener { dialogBinding.actvSpecies.showDropDown() }

        if (isEditing) {
            dialogBinding.etPetName.setText(petToEdit?.name)
            dialogBinding.actvSpecies.setText(petToEdit?.species, false)
            dialogBinding.etBreed.setText(petToEdit?.breed)
            dialogBinding.etAge.setText(petToEdit?.age.toString())
            dialogBinding.etWeight.setText(petToEdit?.weight.toString())
        } else {
            dialogBinding.actvSpecies.setText(speciesList[0], false)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val name = dialogBinding.etPetName.text?.toString()?.trim() ?: ""
                val species = dialogBinding.actvSpecies.text?.toString()?.trim() ?: "Dog"
                val breed = dialogBinding.etBreed.text?.toString()?.trim() ?: ""
                val age = dialogBinding.etAge.text?.toString()?.toIntOrNull() ?: 1
                val weight = dialogBinding.etWeight.text?.toString()?.toDoubleOrNull() ?: 5.0

                if (name.isBlank()) {
                    dialogBinding.tilPetName.error = "Please enter pet name"
                    return@setOnClickListener
                }
                dialogBinding.tilPetName.error = null

                if (isEditing && petToEdit != null) {
                    viewModel.updatePet(
                        petToEdit.copy(
                            name = name,
                            species = species,
                            breed = breed,
                            age = age,
                            weight = weight
                        )
                    )
                } else {
                    viewModel.addPet(name, species, breed, age, weight)
                }
                dialog.dismiss()
            }
        }
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.show()
    }

    private fun showDeletePetConfirmation(pet: PetEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete) + " " + pet.name)
            .setMessage(getString(R.string.pets_delete_confirm))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deletePet(pet)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
