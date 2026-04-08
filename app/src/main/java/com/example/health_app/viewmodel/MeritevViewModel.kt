package com.example.health_app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.health_app.R
import com.example.health_app.data.FirestoreRepository
import com.example.health_app.data.Meritev
import com.example.health_app.data.MeritevRepository
import com.example.health_app.data.Statistics
import com.example.health_app.network.SensorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MeritevViewModel(
    application: Application,
    private val repository: MeritevRepository,
    private val firestoreRepository: FirestoreRepository,
    private val sensorRepository: SensorRepository
) : AndroidViewModel(application) {

    private val _currentUserId = MutableStateFlow<String?>(null)
    private val _statusMessage = MutableSharedFlow<String>()
    private val _zadnjaShranjenaId = MutableStateFlow<Int?>(null)
    private val _operationResult = MutableStateFlow<OperationResult?>(null)
    private val _lastSyncCount = MutableStateFlow<Int?>(null)

    val statusMessage = _statusMessage.asSharedFlow()
    val zadnjaShranjenaId: StateFlow<Int?> = _zadnjaShranjenaId.asStateFlow()
    val operationResult: StateFlow<OperationResult?> = _operationResult.asStateFlow()
    val lastSyncCount: StateFlow<Int?> = _lastSyncCount.asStateFlow()

    val vseMeritve: StateFlow<List<Meritev>> = _currentUserId
        .flatMapLatest { userId ->
            if (userId.isNullOrBlank()) emptyFlow() else repository.getMeritveByUser(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Real-time cloud meritve (Firestore SnapshotListener)
    val cloudMeritve: StateFlow<List<Meritev>> = _currentUserId
        .flatMapLatest { userId ->
            if (userId.isNullOrBlank()) emptyFlow() else firestoreRepository.getMeritveByUser(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Statistics computed from cloud data (real-time)
    val statistics: StateFlow<Statistics> = cloudMeritve
        .map { meritve -> Statistics.fromMeritve(meritve) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Statistics()
        )

    fun setCurrentUser(userId: String?) {
        _currentUserId.value = userId
    }

    fun getMeritevById(id: Int): Flow<Meritev?> = repository.getMeritevById(id)

    fun vstavi(meritev: Meritev, onInserted: (Int) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val userId = _currentUserId.value ?: ""
                val withUser = meritev.copy(userId = userId)
                val insertedId = repository.vstavi(withUser).toInt()

                if (userId.isNotBlank()) {
                    runCatching { firestoreRepository.insertMeritev(withUser) }
                        .onSuccess { firestoreId ->
                            repository.posodobi(withUser.copy(id = insertedId, firestoreId = firestoreId))
                        }
                        .onFailure {
                            _statusMessage.emit(
                                getApplication<Application>().getString(
                                    R.string.shranjeno_lokalno_oblak_nedosegljiv
                                )
                            )
                        }
                }

                _zadnjaShranjenaId.value = insertedId
                onInserted(insertedId)
                _operationResult.value = OperationResult.INSERTED
            } catch (_: Exception) {
                _operationResult.value = OperationResult.ERROR
            }
        }
    }

    fun posodobi(meritev: Meritev) {
        viewModelScope.launch {
            try {
                repository.posodobi(meritev)
                _operationResult.value = OperationResult.UPDATED
            } catch (_: Exception) {
                _operationResult.value = OperationResult.ERROR
            }
        }
    }

    fun izbrisi(meritev: Meritev) {
        viewModelScope.launch {
            try {
                repository.izbrisi(meritev)
                if (meritev.firestoreId.isNotBlank()) {
                    runCatching { firestoreRepository.deleteMeritev(meritev.firestoreId) }
                }
                _operationResult.value = OperationResult.DELETED
            } catch (_: Exception) {
                _operationResult.value = OperationResult.ERROR
            }
        }
    }

    fun syncFromFirestore() {
        val uid = _currentUserId.value ?: return
        viewModelScope.launch {
            runCatching {
                val remoteMeritve = firestoreRepository.fetchAllByUser(uid)
                repository.deleteAllByUser(uid)
                remoteMeritve.forEach { repository.vstavi(it.copy(id = 0, userId = uid)) }
                remoteMeritve.size
            }.onSuccess { syncedCount ->
                _lastSyncCount.value = syncedCount
                _operationResult.value = OperationResult.SYNCED
            }.onFailure {
                _operationResult.value = OperationResult.ERROR
            }
        }
    }

    fun preberiSrcniUtrip(onValue: (Int) -> Unit) {
        viewModelScope.launch {
            val value = if (sensorRepository.hasHeartRateSensor()) {
                sensorRepository.readHeartRateFromSensor() ?: sensorRepository.readHeartRateFromApi()
            } else {
                sensorRepository.readHeartRateFromApi()
            }
            onValue(value)
        }
    }

    fun preberiSpO2(onValue: (Int) -> Unit) {
        viewModelScope.launch {
            onValue(sensorRepository.readSpO2FromApi())
        }
    }

    fun preberiTemperaturo(onValue: (Double) -> Unit) {
        viewModelScope.launch {
            onValue(sensorRepository.readTemperatureFromApi())
        }
    }

    fun clearOperationResult() {
        _operationResult.value = null
        _lastSyncCount.value = null
    }

    enum class OperationResult {
        INSERTED, UPDATED, DELETED, SYNCED, ERROR
    }
}

class MeritevViewModelFactory(
    private val application: Application,
    private val repository: MeritevRepository,
    private val firestoreRepository: FirestoreRepository,
    private val sensorRepository: SensorRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeritevViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MeritevViewModel(
                application = application,
                repository = repository,
                firestoreRepository = firestoreRepository,
                sensorRepository = sensorRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

