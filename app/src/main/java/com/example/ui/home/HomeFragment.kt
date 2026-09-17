package com.example.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.databinding.FragmentHomeBinding
import com.example.ui.routines.CareTaskAdapter
import com.example.ui.routines.SwipeGestureCallback
import com.example.ui.viewmodel.PetCareViewModel
import com.example.ui.viewmodel.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PetCareViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as PetCareApplication)
    }

    private lateinit var petChipAdapter: HomePetChipAdapter
    private lateinit var taskAdapter: CareTaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvGreeting.text = getString(R.string.home_greeting, viewModel.currentUsername.ifBlank { "Pet Lover" })

        setupPetRecyclerView()
        setupTaskRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupPetRecyclerView() {
        petChipAdapter = HomePetChipAdapter { pet ->
            // Switch to routines filtered by this pet or navigate to Pets tab
            (activity as? MainActivity)?.navigateToRoutinesWithPet(pet.petId)
        }
        binding.rvHomePets.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvHomePets.adapter = petChipAdapter
    }

    private fun setupTaskRecyclerView() {
        taskAdapter = CareTaskAdapter(
            onToggleCompletion = { task ->
                viewModel.toggleTaskCompletion(task)
            },
            onEditTask = { careTaskWithPet ->
                showEditTaskDialog(careTaskWithPet)
            }
        )

        binding.rvHomeTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHomeTasks.adapter = taskAdapter

        // ItemTouchHelper: Swipe Left to Delete, Swipe Right to Complete
        val swipeCallback = SwipeGestureCallback(
            context = requireContext(),
            onSwipeLeft = { position ->
                val item = taskAdapter.getTaskWithPetAt(position)
                viewModel.deleteTask(item.task)
                Snackbar.make(binding.root, "Deleted \"${item.task.taskName}\"", Snackbar.LENGTH_LONG)
                    .setAction(R.string.routines_undo) {
                        viewModel.undoDeleteTask()
                    }
                    .show()
            },
            onSwipeRight = { position ->
                val item = taskAdapter.getTaskWithPetAt(position)
                viewModel.setTaskCompletion(item.task.taskId, true)
                Snackbar.make(binding.root, "Completed \"${item.task.taskName}\"!", Snackbar.LENGTH_SHORT)
                    .show()
            }
        )
        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvHomeTasks)
    }

    private fun setupListeners() {
        binding.btnViewAllPets.setOnClickListener {
            (activity as? MainActivity)?.navigateToPets()
        }

        binding.btnTestShake.setOnClickListener {
            viewModel.resetAllTasksForToday()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userNameFlow.collect { username ->
                        binding.tvGreeting.text = getString(
                            R.string.home_greeting,
                            username.ifBlank { "Pet Lover" }
                        )
                    }
                }

                launch {
                    viewModel.pets.collect { pets ->
                        petChipAdapter.submitList(pets)
                        binding.tvNoPets.visibility = if (pets.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.tasks.collect { tasks ->
                        taskAdapter.submitList(tasks)
                        binding.layoutEmptyTasks.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
                        updateProgressStats(tasks)
                    }
                }
            }
        }
    }

    private fun updateProgressStats(tasks: List<CareTaskWithPet>) {
        val total = tasks.size
        val completed = tasks.count { it.task.isCompleted }

        binding.tvProgressText.text = getString(R.string.home_stats_completed, completed, total)

        if (total > 0) {
            val progressPercent = ((completed.toFloat() / total.toFloat()) * 100).toInt()
            binding.progressIndicator.setProgressCompat(progressPercent, true)
        } else {
            binding.progressIndicator.setProgressCompat(0, true)
        }
    }

    private fun showEditTaskDialog(item: CareTaskWithPet) {
        val task = item.task
        val dialogBinding = DialogAddEditTaskBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTaskTitle.text = getString(R.string.routines_edit_task)
        dialogBinding.etTaskName.setText(task.taskName)
        dialogBinding.etScheduleTime.setText(task.scheduleTime)
        dialogBinding.etNotes.setText(task.notes)

        val categories = listOf("Feeding", "Medication", "Grooming", "Walking", "Vet Check", "Playtime")
        val categoryAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        dialogBinding.actvTaskCategory.setAdapter(categoryAdapter)
        dialogBinding.actvTaskCategory.setText(task.category, false)

        val currentPets = viewModel.pets.value
        val petNames = currentPets.map { it.name }
        val petAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, petNames)
        dialogBinding.actvTaskPet.setAdapter(petAdapter)
        val selectedPetName = currentPets.find { it.petId == task.petId }?.name ?: ""
        dialogBinding.actvTaskPet.setText(selectedPetName, false)

        dialogBinding.etScheduleTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(8)
                .setMinute(0)
                .setTitleText("Select Routine Time")
                .build()

            picker.addOnPositiveButtonClickListener {
                val hour = picker.hour
                val minute = picker.minute
                val amPm = if (hour >= 12) "PM" else "AM"
                val displayHour = if (hour % 12 == 0) 12 else hour % 12
                val formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
                dialogBinding.etScheduleTime.setText(formattedTime)
            }
            picker.show(parentFragmentManager, "time_picker_edit")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = dialogBinding.etTaskName.text?.toString() ?: ""
                val category = dialogBinding.actvTaskCategory.text?.toString() ?: "Feeding"
                val time = dialogBinding.etScheduleTime.text?.toString() ?: "08:00 AM"
                val notes = dialogBinding.etNotes.text?.toString() ?: ""

                val petNameChosen = dialogBinding.actvTaskPet.text?.toString() ?: ""
                val petId = currentPets.find { it.name == petNameChosen }?.petId ?: task.petId

                viewModel.updateTask(
                    task.copy(
                        petId = petId,
                        taskName = name.ifBlank { task.taskName },
                        category = category,
                        scheduleTime = time,
                        notes = notes
                    )
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
