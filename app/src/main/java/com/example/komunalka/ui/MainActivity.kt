package com.example.komunalka.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.komunalka.R
import com.example.komunalka.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Нижняя навигация переключает три верхнеуровневых экрана;
        // экраны деталей/добавления открываются поверх стека, без пункта в bottom nav.
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val topLevel = destination.id == R.id.billsListFragment ||
                destination.id == R.id.statisticsFragment ||
                destination.id == R.id.settingsFragment
            binding.bottomNav.visibility = if (topLevel) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
}
