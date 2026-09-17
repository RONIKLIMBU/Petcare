package com.example.ui.sms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.MainActivity
import com.example.PetCareApplication
import com.example.R
import com.example.data.local.entity.PetEntity
import com.example.databinding.FragmentSmsDelegationBinding
import com.example.ui.viewmodel.PetCareViewModel
import com.example.ui.viewmodel.ViewModelFactory
import com.example.util.SmsHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SmsDelegationFragment : Fragment() {

    private var _binding: FragmentSmsDelegationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PetCareViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as PetCareApplication)
    }

    private lateinit var petSelectorAdapter: PetSelectorAdapter
    private var petList: List<PetEntity> = emptyList()
    private var selectedPet: PetEntity? = null
    private var formattedMessageText: String = ""

    private val requestSmsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                sendDirectSmsNow()
            } else {
                Toast.makeText(
                    requireContext(),
                    "SMS permission denied. You can still use 'Open in Messages App'.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSmsDelegationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPetSelector()
        setupListeners()
        observePets()
    }

    private fun setupPetSelector() {
        petSelectorAdapter = PetSelectorAdapter { pet ->
            selectPet(pet)
        }
        binding.rvPetSelector.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = petSelectorAdapter
        }
    }

    private fun setupListeners() {
        binding.btnSwitchPetDialog.setOnClickListener {
            showPetSelectionDialog()
        }

        binding.btnAddPetNow.setOnClickListener {
            (activity as? MainActivity)?.navigateToPets()
        }

        binding.btnSendDirectSms.setOnClickListener {
            val phoneNumber = binding.etPhoneNumber.text?.toString()?.trim() ?: ""
            if (phoneNumber.isBlank()) {
                binding.tilPhoneNumber.error = "Please enter recipient phone number"
                return@setOnClickListener
            }
            binding.tilPhoneNumber.error = null

            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                sendDirectSmsNow()
            } else {
                requestSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            }
        }

        binding.btnSendIntent.setOnClickListener {
            val phoneNumber = binding.etPhoneNumber.text?.toString()?.trim() ?: ""
            if (formattedMessageText.isBlank()) {
                Toast.makeText(requireContext(), "Please select a pet first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = SmsHelper.createSmsIntent(phoneNumber, formattedMessageText)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "No messaging app available: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPetSelectionDialog() {
        if (petList.isEmpty()) return
        val petNames = petList.map { "${it.name} (${it.species})" }.toTypedArray()
        val currentIndex = petList.indexOfFirst { it.petId == selectedPet?.petId }.coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sms_select_pet)
            .setSingleChoiceItems(petNames, currentIndex) { dialog, which ->
                if (which in petList.indices) {
                    selectPet(petList[which])
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun observePets() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pets.collect { pets ->
                    petList = pets

                    if (pets.isEmpty()) {
                        selectedPet = null
                        formattedMessageText = ""
                        binding.cardNoPets.visibility = View.VISIBLE
                        binding.llPetSelectorSection.visibility = View.GONE
                        binding.tvMessagePreview.text = "No pets available. Please add a pet in the Pets tab first."
                        petSelectorAdapter.submitList(emptyList(), null)
                    } else {
                        binding.cardNoPets.visibility = View.GONE
                        binding.llPetSelectorSection.visibility = View.VISIBLE
                        binding.btnSwitchPetDialog.visibility = if (pets.size > 1) View.VISIBLE else View.GONE

                        val current = selectedPet
                        val targetPet = pets.find { it.petId == current?.petId } ?: pets[0]
                        selectedPet = targetPet
                        petSelectorAdapter.submitList(pets, targetPet.petId)
                        generatePreview()
                    }
                }
            }
        }
    }

    private fun selectPet(pet: PetEntity) {
        selectedPet = pet
        petSelectorAdapter.setSelected(pet.petId)
        generatePreview()
    }

    private fun generatePreview() {
        val pet = selectedPet ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            formattedMessageText = viewModel.prepareSmsRoutine(pet.petId)
            binding.tvMessagePreview.text = formattedMessageText
        }
    }

    private fun sendDirectSmsNow() {
        val phoneNumber = binding.etPhoneNumber.text?.toString()?.trim() ?: ""
        if (phoneNumber.isBlank() || formattedMessageText.isBlank()) {
            Toast.makeText(requireContext(), "Missing phone number or routine message", Toast.LENGTH_SHORT).show()
            return
        }

        val result = SmsHelper.sendDirectSms(requireContext(), phoneNumber, formattedMessageText)
        if (result.isSuccess) {
            Toast.makeText(
                requireContext(),
                "Care routine sent to $phoneNumber via SMS! 🐾",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                requireContext(),
                "Failed to send SMS: ${result.exceptionOrNull()?.localizedMessage}. Try 'Open in Messages App'.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

