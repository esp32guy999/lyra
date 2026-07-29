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
    val configured: Boolean = true,
    val adding: Set<String> = emptySet(),   // foreignArtistIds being added
    val added: Set<String> = emptySet()     // foreignArtistIds added this session
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

    /** Add the artist to Lidarr (monitored + search). Tracks per-artist progress. */
    fun addArtist(artist: LidarrArtist) {
        val fid = artist.foreignArtistId ?: return
        val s = _state.value
        if (fid in s.adding || fid in s.added) return
        _state.value = s.copy(adding = s.adding + fid, error = null)
        viewModelScope.launch {
            try {
                repository.addArtist(artist)
                val cur = _state.value
                _state.value = cur.copy(adding = cur.adding - fid, added = cur.added + fid)
            } catch (e: Exception) {
                val cur = _state.value
                _state.value = cur.copy(
                    adding = cur.adding - fid,
                    error = "Couldn't add ${artist.artistName}: ${e.message}"
                )
            }
        }
    }
}
