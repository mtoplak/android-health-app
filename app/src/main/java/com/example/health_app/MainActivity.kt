package com.example.health_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.health_app.data.AuthRepository
import com.example.health_app.data.FirestoreRepository
import androidx.navigation.compose.rememberNavController
import com.example.health_app.data.MeritevDatabase
import com.example.health_app.data.MeritevRepository
import com.example.health_app.ml.GeminiHelper
import com.example.health_app.ml.HealthClassifier
import com.example.health_app.network.SensorRepository
import com.example.health_app.ui.navigation.NavGraph
import com.example.health_app.ui.theme.HealthAppTheme
import com.example.health_app.viewmodel.AuthViewModel
import com.example.health_app.viewmodel.AuthViewModelFactory
import com.example.health_app.viewmodel.MeritevViewModel
import com.example.health_app.viewmodel.MeritevViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize database, repository and ViewModel
        val database = MeritevDatabase.getDatabase(applicationContext)
        val repository = MeritevRepository(database.meritevDao())
        val firestoreRepository = FirestoreRepository()
        val sensorRepository = SensorRepository(applicationContext)
        val healthClassifier = HealthClassifier(applicationContext)
        val geminiHelper = GeminiHelper()
        val viewModelFactory = MeritevViewModelFactory(
            application = application,
            repository = repository,
            firestoreRepository = firestoreRepository,
            sensorRepository = sensorRepository,
            healthClassifier = healthClassifier,
            geminiHelper = geminiHelper
        )
        val viewModel = ViewModelProvider(this, viewModelFactory)[MeritevViewModel::class.java]

        val authRepository = AuthRepository()
        val authViewModelFactory = AuthViewModelFactory(authRepository)
        val authViewModel = ViewModelProvider(this, authViewModelFactory)[AuthViewModel::class.java]

        setContent {
            HealthAppTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    viewModel = viewModel,
                    authViewModel = authViewModel
                )
            }
        }
    }
}

