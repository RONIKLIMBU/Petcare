package com.example

import android.content.Context
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.databinding.ActivityMainBinding
import com.example.ui.auth.AuthFragment
import com.example.ui.home.HomeFragment
import com.example.ui.pets.PetsFragment
import com.example.ui.routines.RoutinesFragment
import com.example.ui.sms.SmsDelegationFragment
import com.example.ui.viewmodel.PetCareViewModel
import com.example.ui.viewmodel.ViewModelFactory
import com.example.util.ShakeDetector
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val petCareViewModel: PetCareViewModel by viewModels {
        ViewModelFactory(application as PetCareApplication)
    }

    private var shakeDetector: ShakeDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupToolbar()
        setupBottomNavigation()
        setupShakeDetector()
        observeEvents()

        val sessionManager = (application as PetCareApplication).sessionManager
        if (savedInstanceState == null) {
            if (sessionManager.isLoggedIn()) {
                petCareViewModel.refreshUserSession()
                showMainApp()
            } else {
                showAuthScreen()
            }
        } else {
            // Restore visibility state
            if (sessionManager.isLoggedIn()) {
                petCareViewModel.refreshUserSession()
                binding.bottomNav.visibility = View.VISIBLE
            } else {
                binding.bottomNav.visibility = View.GONE
            }
        }
    }

    private fun setupToolbar() {
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_shake_reset -> {
                    triggerShakeAction()
                    true
                }
                R.id.action_logout -> {
                    petCareViewModel.logout()
                    showAuthScreen()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment(), "HOME")
                    binding.topAppBar.title = getString(R.string.nav_home)
                    true
                }
                R.id.nav_pets -> {
                    replaceFragment(PetsFragment(), "PETS")
                    binding.topAppBar.title = getString(R.string.nav_pets)
                    true
                }
                R.id.nav_routines -> {
                    replaceFragment(RoutinesFragment(), "ROUTINES")
                    binding.topAppBar.title = getString(R.string.nav_routines)
                    true
                }
                R.id.nav_sms -> {
                    replaceFragment(SmsDelegationFragment(), "SMS")
                    binding.topAppBar.title = getString(R.string.nav_sms)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupShakeDetector() {
        shakeDetector = ShakeDetector {
            triggerShakeAction()
        }
    }

    private fun triggerShakeAction() {
        vibratePhone()
        petCareViewModel.resetAllTasksForToday()
    }

    @Suppress("DEPRECATION")
    private fun vibratePhone() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    vibrator.vibrate(150)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration error if device has no vibrator
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.coordinatorLayout) { _, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            // Padding top of AppBarLayout so the toolbar is below the status bar
            binding.appBarLayout.updatePadding(top = systemBars.top)

            // Padding bottom of BottomNavigationView so items are above the gesture navigation bar
            if (binding.bottomNav.visibility == View.VISIBLE) {
                binding.bottomNav.updatePadding(bottom = systemBars.bottom)
            } else {
                binding.bottomNav.updatePadding(bottom = 0)
            }

            // Adjust fragmentContainer bottom margin so it doesn't overlap bottomNav or keyboard
            val isBottomNavVisible = binding.bottomNav.visibility == View.VISIBLE
            val bottomNavMeasuredHeight = if (isBottomNavVisible) {
                if (binding.bottomNav.height > 0) {
                    binding.bottomNav.height
                } else {
                    (80 * resources.displayMetrics.density).toInt()
                }
            } else {
                0
            }

            val requiredBottom = if (ime.bottom > 0) {
                ime.bottom
            } else {
                bottomNavMeasuredHeight
            }

            val lp = binding.fragmentContainer.layoutParams as? ViewGroup.MarginLayoutParams
            if (lp != null && lp.bottomMargin != requiredBottom) {
                lp.bottomMargin = requiredBottom
                binding.fragmentContainer.layoutParams = lp
            }

            windowInsets
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                petCareViewModel.eventFlow.collect { message ->
                    val snackbar = Snackbar.make(binding.coordinatorLayout, message, Snackbar.LENGTH_SHORT)
                    if (binding.bottomNav.visibility == View.VISIBLE) {
                        snackbar.anchorView = binding.bottomNav
                    }
                    snackbar.show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sensorManager?.let { shakeDetector?.start(it) }
    }

    override fun onPause() {
        super.onPause()
        shakeDetector?.stop()
    }

    fun onUserAuthenticated() {
        petCareViewModel.refreshUserSession()
        showMainApp()
    }

    fun navigateToPets() {
        binding.bottomNav.selectedItemId = R.id.nav_pets
    }

    fun navigateToRoutinesWithPet(petId: Long) {
        petCareViewModel.setPetFilter(petId)
        binding.bottomNav.selectedItemId = R.id.nav_routines
    }

    private fun showAuthScreen() {
        binding.bottomNav.visibility = View.GONE
        updateContainerMargin(0)
        ViewCompat.requestApplyInsets(binding.coordinatorLayout)
        binding.topAppBar.title = getString(R.string.app_name)
        binding.topAppBar.menu.findItem(R.id.action_logout)?.isVisible = false
        binding.topAppBar.menu.findItem(R.id.action_shake_reset)?.isVisible = false
        replaceFragment(AuthFragment(), "AUTH")
    }

    private fun showMainApp() {
        binding.bottomNav.visibility = View.VISIBLE
        ViewCompat.requestApplyInsets(binding.coordinatorLayout)
        binding.topAppBar.menu.findItem(R.id.action_logout)?.isVisible = true
        binding.topAppBar.menu.findItem(R.id.action_shake_reset)?.isVisible = true
        binding.bottomNav.selectedItemId = R.id.nav_home
    }

    private fun updateContainerMargin(bottomMargin: Int) {
        val lp = binding.fragmentContainer.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        if (lp.bottomMargin != bottomMargin) {
            lp.bottomMargin = bottomMargin
            binding.fragmentContainer.layoutParams = lp
        }
    }

    private fun replaceFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .commit()
    }
}
