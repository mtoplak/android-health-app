package com.example.health_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.example.health_app.data.MeritevDatabase
import com.example.health_app.data.MeritevRepository
import com.example.health_app.ui.navigation.NavGraph
import com.example.health_app.ui.theme.HealthAppTheme
import com.example.health_app.viewmodel.MeritevViewModel
import com.example.health_app.viewmodel.MeritevViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize database, repository and ViewModel
        val database = MeritevDatabase.getDatabase(applicationContext)
        val repository = MeritevRepository(database.meritevDao())
        val viewModelFactory = MeritevViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[MeritevViewModel::class.java]

        setContent {
            HealthAppTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }
    }
}

