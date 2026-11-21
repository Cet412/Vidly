package com.vidly.app.ui.history

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidly.app.data.local.HistoryEntity
import com.vidly.app.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class HistoryState(
    val items: List<HistoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedPlatform: String = "Semua",
    val showDeleteDialog: Boolean = false,
    val itemToDelete: HistoryEntity? = null,
    val dontShowAgain: Boolean = false
)

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    private val prefs by lazy { null } // placeholder

    init {
        observeItems()
    }

    private fun observeItems() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val flow = when {
                _state.value.searchQuery.isNotBlank() && _state.value.selectedPlatform != "Semua" ->
                    repository.searchAndFilter(_state.value.searchQuery, _state.value.selectedPlatform)
                _state.value.searchQuery.isNotBlank() ->
                    repository.search(_state.value.searchQuery)
                _state.value.selectedPlatform != "Semua" ->
                    repository.filterByPlatform(_state.value.selectedPlatform)
                else ->
                    repository.getAll()
            }
            flow.collect { items ->
                _state.value = _state.value.copy(items = items, isLoading = false)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        observeItems()
    }

    fun onFilterChanged(platform: String) {
        _state.value = _state.value.copy(selectedPlatform = platform)
        observeItems()
    }

    fun requestDelete(item: HistoryEntity) {
        val prefs = _state.value // placeholder
        // Check if user opted out of confirmation
        // For simplicity, always show dialog for now
        _state.value = _state.value.copy(showDeleteDialog = true, itemToDelete = item)
    }

    fun confirmDelete() {
        val item = _state.value.itemToDelete ?: return
        viewModelScope.launch {
            repository.delete(item)
            _state.value = _state.value.copy(showDeleteDialog = false, itemToDelete = null)
        }
    }

    fun cancelDelete() {
        _state.value = _state.value.copy(showDeleteDialog = false, itemToDelete = null)
    }

    fun setDontShowAgain(value: Boolean) {
        _state.value = _state.value.copy(dontShowAgain = value)
    }
}
