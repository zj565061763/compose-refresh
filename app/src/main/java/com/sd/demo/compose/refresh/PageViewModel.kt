package com.sd.demo.compose.refresh

import androidx.compose.foundation.MutatorMutex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PageViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DemoUIState())
    private val _list = mutableListOf<String>()

    private val _mutator = MutatorMutex()

    val uiState = _uiState.asStateFlow()

    /**
     * 刷新
     */
    fun refresh(count: Int) {
        viewModelScope.launch {
            _mutator.mutate { refreshInternal(count = count) }
        }
    }

    /**
     * 加载更多
     */
    fun loadMore() {
        viewModelScope.launch {
            _mutator.mutate { loadMoreInternal() }
        }
    }

    private suspend fun refreshInternal(count: Int) {
        try {
            _uiState.update { it.copy(isRefreshing = true) }

            delay(1000)
            _list.clear()
            repeat(count) { _list.add(it.toString()) }

            _uiState.update { it.copy(list = _list.toList()) }
        } finally {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun loadMoreInternal() {
        try {
            _uiState.update { it.copy(isLoadingMore = true) }

            delay(1000)
            repeat(10) { _list.add(it.toString()) }

            _uiState.update { it.copy(list = _list.toList()) }
        } finally {
            _uiState.update { it.copy(isLoadingMore = false) }
        }
    }
}

data class DemoUIState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val list: List<String> = listOf(),
)