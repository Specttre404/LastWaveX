package com.lastwave.app.ui.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastwave.app.data.model.ChartCategory
import com.lastwave.app.data.model.ChartEntry
import com.lastwave.app.data.model.ChartScope
import com.lastwave.app.data.music.InnerTubeMusicApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChartStatus {
    LOADING,
    SUCCESS,
    EMPTY,
    UNSUPPORTED,
    ERROR,
}

data class ChartsUiState(
    val scope: ChartScope = ChartScope.GLOBAL,
    val category: ChartCategory = ChartCategory.ARTISTS,
    val entries: List<ChartEntry> = emptyList(),
    val status: ChartStatus = ChartStatus.LOADING,
    val error: String? = null,
)

@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val innerTube: InnerTubeMusicApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartsUiState())
    val uiState: StateFlow<ChartsUiState> = _uiState.asStateFlow()

    init {
        loadCharts()
    }

    fun setScope(scope: ChartScope) {
        if (_uiState.value.scope == scope) return
        _uiState.update { it.copy(scope = scope) }
        loadCharts()
    }

    fun setCategory(category: ChartCategory) {
        if (_uiState.value.category == category) return
        _uiState.update { it.copy(category = category) }
        loadCharts()
    }

    fun loadCharts() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = ChartStatus.LOADING, error = null) }
            val scope = _uiState.value.scope
            val category = _uiState.value.category
            when (val res = innerTube.fetchChartEntries(countryCode = scope.id, category = category)) {
                is com.lastwave.app.data.model.ChartLoadResult.Success -> {
                    _uiState.update {
                        it.copy(
                            entries = res.entries,
                            status = ChartStatus.SUCCESS,
                            error = null,
                        )
                    }
                }
                is com.lastwave.app.data.model.ChartLoadResult.Empty -> {
                    _uiState.update {
                        it.copy(
                            entries = emptyList(),
                            status = ChartStatus.EMPTY,
                            error = null,
                        )
                    }
                }
                is com.lastwave.app.data.model.ChartLoadResult.Unsupported -> {
                    _uiState.update {
                        it.copy(
                            entries = emptyList(),
                            status = ChartStatus.UNSUPPORTED,
                            error = null,
                        )
                    }
                }
                is com.lastwave.app.data.model.ChartLoadResult.Error -> {
                    _uiState.update {
                        it.copy(
                            entries = emptyList(),
                            status = ChartStatus.ERROR,
                            error = res.message,
                        )
                    }
                }
            }
        }
    }
}
