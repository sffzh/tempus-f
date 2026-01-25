package cn.sffzh.tempus.ui.activity.controller

import android.os.Bundle
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.cappielloantonio.tempo.ui.activity.MainActivity
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class NavigationController(
    private val activity: MainActivity,
    private val binding: ActivityMainBinding,
    private val bottomSheetController: BottomSheetController
) {

    val navController: NavController
    val bottomNavigationView: BottomNavigationView = binding.bottomNavigation

    init {
        val navHostFragment =
            activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                    as NavHostFragment
        navController = navHostFragment.navController

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        navController.addOnDestinationChangedListener { _: NavController, destination: NavDestination, _: Bundle? ->
            val id = destination.id
            val isMainTab =
                id == R.id.homeFragment ||
                        id == R.id.libraryFragment ||
                        id == R.id.downloadFragment

            if (isMainTab &&
                bottomSheetController.behavior.state == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            ) {
                bottomSheetController.collapseDelayed()
            }
        }

        NavigationUI.setupWithNavController(bottomNavigationView, navController)
    }

    fun setBottomNavigationBarVisibility(visible: Boolean) {
        bottomNavigationView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun goToLogin() {
        bottomSheetController.setInPeek(false)
        setBottomNavigationBarVisibility(false)
        bottomSheetController.setVisibility(false)

        when (navController.currentDestination?.id) {
            R.id.landingFragment ->
                navController.navigate(R.id.action_landingFragment_to_loginFragment)
            R.id.settingsFragment ->
                navController.navigate(R.id.action_settingsFragment_to_loginFragment)
            R.id.homeFragment ->
                navController.navigate(R.id.action_homeFragment_to_loginFragment)
        }
    }

    fun goToHome() {
        setBottomNavigationBarVisibility(true)

        when (navController.currentDestination?.id) {
            R.id.landingFragment ->
                navController.navigate(R.id.action_landingFragment_to_homeFragment)
            R.id.loginFragment ->
                navController.navigate(R.id.action_loginFragment_to_homeFragment)
        }
    }

    fun resetCurrentDestination() {
        val id = navController.currentDestination?.id ?: return
        navController.popBackStack(id, true)
        navController.navigate(id)
    }
}
