package com.example.ui.routines

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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.MainActivity
import com.example.PetCareApplication
import com.example.R
import com.example.data.local.entity.CareTaskWithPet
import com.example.databinding.DialogAddEditTaskBinding
import com.example.databinding.FragmentRoutinesBinding
import com.example.ui.viewmodel.PetCareViewModel
import com.example.ui.viewmodel.ViewModelFactory
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.util.Locale

class RoutinesFragment : Fragment() {

    private var _binding: FragmentRoutinesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PetCareViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as PetCareApplication)
    }

    private lateinit var routineAdapter: CareTaskAdapter

    companion object {
        private const val ARG_PET_ID = "arg_pet_id"

        fun newInstance(petId: Long? = null): RoutinesFragment {
            val fragment = RoutinesFragment()
            if (petId != null) {
                val args = Bundle()
                args.putLong(ARG_PET_ID, petId)
                fragment.arguments = args
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoutinesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getLong(ARG_PET_ID)?.let { petId ->
            if (petId > 0) {
                viewModel.setPetFilter(petId)
            }
        }

        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupRecyclerView() {
        routineAdapter = CareTaskAdapter(
            onToggleCompletion = { task ->
                viewModel.toggleTaskCompletion(task)
            },
            onEditTask = { item ->
                showAddEditTaskDialog(item)
            }
        )
        binding.rvRoutines.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRoutines.adapter = routineAdapter

        // Setup swipe-to-delete (left) and swipe-to-complete (right)
        val swipeCallback = SwipeGestureCallback(
            context = requireContext(),
            onSwipeLeft = { position ->
                val item = routineAdapter.currentList[position]
                viewModel.deleteTask(item.task)
                showUndoSnackbar(item)
            },
            onSwipeRight = { position ->
                val item = routineAdapter.currentList[position]
                viewModel.setTaskCompletion(item.task.taskId, true)
                routineAdapter.notifyItemChanged(position)
            }
        )
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvRoutines)
    }

    private fun showUndoSnackbar(deletedItem: CareTaskWithPet) {
        Snackbar.make(
            binding.root,
            getString(R.string.routines_deleted) + ": " + deletedItem.task.taskName,
            Snackbar.LENGTH_LONG
        )
            .setAction(R.string.routines_undo) {
                viewModel.undoDeleteTask()
            }
            .show()
    }

    private fun setupListeners() {
        binding.fabAddRoutine.setOnClickListener {
            showAddEditTaskDialog(null)
        }
        binding.btnAddRoutineEmpty.setOnClickListener {
            showAddEditTaskDialog(null)
        }

        binding.chipFilterAll.setOnClickListener {
            viewModel.setPetFilter(null)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.pets.collect { pets ->
                        updatePetFilterChips(pets)
                    }
                }

                launch {
                    viewModel.tasks.collect { tasks ->
                        routineAdapter.submitList(tasks)
                        binding.layoutEmptyRoutines.visibility =
                            if (tasks.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun updatePetFilterChips(pets: List<com.example.data.local.entity.PetEntity>) {
        val currentFilter = viewModel.selectedPetFilter.value

        // Remove dynamic chips, keep "All"
        val count = binding.chipGroupFilter.childCount
        for (i in count - 1 downTo 1) {
            binding.chipGroupFilter.removeViewAt(i)
        }

        binding.chipFilterAll.isChecked = (currentFilter == null)

        pets.forEach { pet ->
            val chip = Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                text = pet.name
                isCheckable = true
                id = View.generateViewId()
                isChecked = (currentFilter == pet.petId)
                setOnClickListener {
                    viewModel.setPetFilter(pet.petId)
                }
            }
            binding.chipGroupFilter.addView(chip)
        }
    }

    private fun showAddEditTaskDialog(existingItem: CareTaskWithPet?) {
        val currentPets = viewModel.pets.value
        if (currentPets.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("No Pets Available")
                .setMessage("Please register a pet before scheduling care routines.")
                .setPositiveButton("Add Pet") { _, _ ->
                    (activity as? MainActivity)?.navigateToPets()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return
        }

        val dialogBinding = DialogAddEditTaskBinding.inflate(layoutInflater)
        val isEditing = existingItem != null

        dialogBinding.tvDialogTaskTitle.text = if (isEditing) {
            getString(R.string.routines_edit_task)
        } else {
            getString(R.string.routines_add_task)
        }

        val categories = listOf("Feeding", "Medication", "Grooming", "Walking", "Vet Check", "Playtime")
        dialogBinding.actvTaskCategory.setSimpleItems(categories.toTypedArray())
        dialogBinding.actvTaskCategory.setOnClickListener { dialogBinding.actvTaskCategory.showDropDown() }
        dialogBinding.tilTaskCategory.setOnClickListener { dialogBinding.actvTaskCategory.showDropDown() }

        val petNames = currentPets.map { it.name }
        dialogBinding.actvTaskPet.setSimpleItems(petNames.toTypedArray())
        dialogBinding.actvTaskPet.setOnClickListener { dialogBinding.actvTaskPet.showDropDown() }
        dialogBinding.tilTaskPet.setOnClickListener { dialogBinding.actvTaskPet.showDropDown() }

        if (isEditing && existingItem != null) {
            val task = existingItem.task
            dialogBinding.etTaskName.setText(task.taskName)
            dialogBinding.actvTaskCategory.setText(task.category, false)
            dialogBinding.etScheduleTime.setText(task.scheduleTime)
            dialogBinding.etNotes.setText(task.notes)

            val currentPetName = currentPets.find { it.petId == task.petId }?.name ?: currentPets[0].name
            dialogBinding.actvTaskPet.setText(currentPetName, false)
        } else {
            dialogBinding.actvTaskCategory.setText(categories[0], false)
            dialogBinding.etScheduleTime.setText("08:00 AM")
            val defaultPet = currentPets.find { it.petId == viewModel.selectedPetFilter.value } ?: currentPets[0]
            dialogBinding.actvTaskPet.setText(defaultPet.name, false)
        }

        dialogBinding.etScheduleTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(8)
                .setMinute(0)
                .setTitleText("Select Care Time")
                .build()

            picker.addOnPositiveButtonClickListener {
                val hour = picker.hour
                val minute = picker.minute
                val amPm = if (hour >= 12) "PM" else "AM"
                val displayHour = if (hour % 12 == 0) 12 else hour % 12
                val formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
                dialogBinding.etScheduleTime.setText(formattedTime)
            }
            picker.show(parentFragmentManager, "routine_time_picker")
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val name = dialogBinding.etTaskName.text?.toString()?.trim() ?: ""
                val category = dialogBinding.actvTaskCategory.text?.toString()?.trim() ?: "Feeding"
                val time = dialogBinding.etScheduleTime.text?.toString()?.trim() ?: "08:00 AM"
                val notes = dialogBinding.etNotes.text?.toString()?.trim() ?: ""

                val chosenPetName = dialogBinding.actvTaskPet.text?.toString()?.trim() ?: ""
                val chosenPet = currentPets.find { it.name == chosenPetName } ?: currentPets.firstOrNull()

                if (name.isBlank()) {
                    dialogBinding.tilTaskName.error = "Please enter task name"
                    return@setOnClickListener
                }
                dialogBinding.tilTaskName.error = null

                if (chosenPet == null) {
                    dialogBinding.tilTaskPet.error = "Please select a valid pet"
                    return@setOnClickListener
                }
                dialogBinding.tilTaskPet.error = null

                if (isEditing && existingItem != null) {
                    viewModel.updateTask(
                        existingItem.task.copy(
                            petId = chosenPet.petId,
                            taskName = name,
                            category = category,
                            scheduleTime = time,
                            notes = notes
                        )
                    )
                } else {
                    viewModel.addTask(
                        petId = chosenPet.petId,
                        name = name,
                        category = category,
                        scheduleTime = time,
                        notes = notes
                    )
                }
                dialog.dismiss()
            }
        }
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
