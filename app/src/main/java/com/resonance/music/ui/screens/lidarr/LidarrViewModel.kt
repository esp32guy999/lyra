package com.resonance.music.ui.screens.lidarr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.music.data.lidarr.LidarrArtist
import com.resonance.music.data.lidarr.LidarrConfig
import com.resonance.music.data.lidarr.LidarrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LidarrUiState(
    val query: String = "",
    val results: List<LidarrArtist> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val configured: Boolean = true
)

@HiltViewModel
class LidarrViewModel @Inject constructor(
    private val repository: LidarrRepository,
    config: LidarrConfig
) : ViewModel() {

    private val _state = MutableStateFlow(LidarrUiState(configured = config.isConfigured))
    val state: StateFlow<LidarrUiState> = _state.asStateFlow()

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(query = q)
    }

    fun search() {
        val term = _state.value.query.trim()
        if (term.isEmpty()) return
        if (!_state.value.configured) {
            _state.value = _state.value.copy(
                error = "Lidarr isn't configured — add lidarr.url and lidarr.key to local.properties."
            )
            return
        }
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val hits = repository.lookupArtist(term)
                _state.value = _state.value.copy(loading = false, results = hits, error = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Search failed"
                )
            }
        }
    }
}
