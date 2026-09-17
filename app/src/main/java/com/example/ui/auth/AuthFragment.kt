package com.example.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.MainActivity
import com.example.PetCareApplication
import com.example.R
import com.example.databinding.FragmentAuthBinding
import com.example.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as PetCareApplication)
    }

    private var isRegisterMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupModeToggle()
        setupSubmitButton()
        observeViewModel()
    }

    private fun setupModeToggle() {
        binding.toggleAuthMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isRegisterMode = (checkedId == R.id.btnTabRegister)
                updateUiForMode()
            }
        }
    }

    private fun updateUiForMode() {
        if (isRegisterMode) {
            binding.tvSubtitle.text = getString(R.string.auth_register_subtitle)
            binding.tilEmail.visibility = View.VISIBLE
            binding.btnSubmit.text = getString(R.string.auth_register_button)
        } else {
            binding.tvSubtitle.text = getString(R.string.auth_login_subtitle)
            binding.tilEmail.visibility = View.GONE
            binding.btnSubmit.text = getString(R.string.auth_login_button)
        }
        authViewModel.resetState()
    }

    private fun setupSubmitButton() {
        binding.btnSubmit.setOnClickListener {
            val username = binding.etUsername.text?.toString() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""

            if (isRegisterMode) {
                val email = binding.etEmail.text?.toString() ?: ""
                authViewModel.register(username, email, password)
            } else {
                authViewModel.login(username, password)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnSubmit.isEnabled = true
                        }
                        is AuthUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnSubmit.isEnabled = false
                        }
                        is AuthUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnSubmit.isEnabled = true
                            Toast.makeText(
                                requireContext(),
                                "Welcome, ${state.user.username}!",
                                Toast.LENGTH_SHORT
                            ).show()
                            (activity as? MainActivity)?.onUserAuthenticated()
                        }
                        is AuthUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnSubmit.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
