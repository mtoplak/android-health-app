package com.example.health_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.health_app.data.Meritev
import com.example.health_app.data.MeritevRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MeritevViewModel(private val repository: MeritevRepository) : ViewModel() {

    val vseMeritve: StateFlow<List<Meritev>> = repository.vseMeritve
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _zadnjaShranjenaId = MutableStateFlow<Int?>(null)
    val zadnjaShranjenaId: StateFlow<Int?> = _zadnjaShranjenaId.asStateFlow()

    private val _operationResult = MutableStateFlow<OperationResult?>(null)
    val operationResult: StateFlow<OperationResult?> = _operationResult.asStateFlow()

    fun getMeritevById(id: Int): Flow<Meritev?> = repository.getMeritevById(id)

    fun vstavi(meritev: Meritev, onInserted: (Int) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val insertedId = repository.vstavi(meritev).toInt()
                _zadnjaShranjenaId.value = insertedId
                onInserted(insertedId)
                _operationResult.value = OperationResult.INSERTED
            } catch (e: Exception) {
                _operationResult.value = OperationResult.ERROR
            }
        }
    }

    fun posodobi(meritev: Meritev) {
        viewModelScope.launch {
            try {
                repository.posodobi(meritev)
                _operationResult.value = OperationResult.UPDATED
            } catch (e: Exception) {
                _operationResult.value = OperationResult.ERROR
            }
        }
    }

    fun izbrisi(meritev: Meritev) {
        viewModelScope.launch {
            try {
                repository.izbrisi(meritev)
                _operationResult.value = OperationResult.DELETED
            } catch (e: Exception) {
                _operationResult.value = OperationResult.ERROR
            }
        }
    }

    fun clearOperationResult() {
        _operationResult.value = null
    }

    enum class OperationResult {
        INSERTED, UPDATED, DELETED, ERROR
    }
}

class MeritevViewModelFactory(
    private val repository: MeritevRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeritevViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MeritevViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

