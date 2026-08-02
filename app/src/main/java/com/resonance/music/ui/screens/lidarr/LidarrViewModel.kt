package com.resonance.music.ui.screens.lidarr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonance.music.data.lidarr.LidarrAlbum
import com.resonance.music.data.lidarr.LidarrArtist
import com.resonance.music.data.lidarr.LidarrConfig
import com.resonance.music.data.lidarr.LidarrRelease
import com.resonance.music.data.lidarr.LidarrRepository
import com.resonance.music.data.prowlarr.ProwlarrRepository
import com.resonance.music.data.prowlarr.ProwlarrResult
import com.resonance.music.data.qbit.PendingTorrent
import com.resonance.music.data.qbit.QbitConfig
import com.resonance.music.data.qbit.QbitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Stage { SEARCH, DISCOG, RESULTS }
enum class AcqSource { LIDARR, PROWLARR }

/** One merged acquisition result row (Lidarr-managed or Prowlarr-raw). */
data class AcqResult(
    val title: String,
    val quality: String,
    val seeders: Int,
    val source: AcqSource,
    val protocol: String,             // "torrent" | "usenet"
    val lidarr: LidarrRelease? = null,
    val prowlarr: ProwlarrResult? = null
)

data class LidarrUiState(
    val stage: Stage = Stage.SEARCH,
    val query: String = "",
    val artists: List<LidarrArtist> = emptyList(),
    val artist: LidarrArtist? = null,
    val albums: List<LidarrAlbum> = emptyList(),
    val album: LidarrAlbum? = null,
    val results: List<AcqResult> = emptyList(),
    val loading: Boolean = false,
    val busy: String? = null,          // transient status (e.g. "Grabbing…")
    val error: String? = null,
    val configured: Boolean = true,
    // track-select sheet (Prowlarr torrents only)
    val pending: PendingTorrent? = null,
    val selected: Set<Int> = emptySet()
)

@HiltViewModel
class LidarrViewModel @Inject constructor(
    private val lidarr: LidarrRepository,
    private val prowlarr: ProwlarrRepository,
    private val qbit: QbitRepository,
    config: LidarrConfig,
    private val qbitConfig: QbitConfig
) : ViewModel() {

    private val _state = MutableStateFlow(LidarrUiState(configured = config.isConfigured))
    val state: StateFlow<LidarrUiState> = _state.asStateFlow()
    private fun set(f: (LidarrUiState) -> LidarrUiState) { _state.value = f(_state.value) }

    fun onQueryChange(q: String) = set { it.copy(query = q) }

    fun search() {
        val term = _state.value.query.trim()
        if (term.isEmpty()) return
        if (!_state.value.configured) { set { it.copy(error = "Lidarr not configured (local.properties).") }; return }
        set { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val hits = lidarr.lookupArtist(term)
                set { it.copy(loading = false, artists = hits, stage = Stage.SEARCH) }
            } catch (e: Exception) { set { it.copy(loading = false, error = e.message ?: "Search failed") } }
        }
    }

    /** Open an artist → its discography (adds to Lidarr first if needed for an id). */
    fun openArtist(a: LidarrArtist) {
        set { it.copy(loading = true, error = null, artist = a) }
        viewModelScope.launch {
            try {
                val artist = if (a.id != null) a else lidarr.addArtist(a)
                val albums = lidarr.getAlbums(artist.id ?: error("no artist id"))
                set { it.copy(loading = false, stage = Stage.DISCOG, artist = artist, albums = albums) }
            } catch (e: Exception) { set { it.copy(loading = false, error = "Couldn't load discography: ${e.message}") } }
        }
    }

    /** Open an album → merged Lidarr + Prowlarr results. */
    fun openAlbum(al: LidarrAlbum) {
        set { it.copy(loading = true, error = null, album = al, results = emptyList(), stage = Stage.RESULTS) }
        viewModelScope.launch {
            val merged = mutableListOf<AcqResult>()
            try {
                lidarr.searchAlbum(al.id ?: error("no album id")).forEach { r ->
                    merged += AcqResult(r.title ?: "?", r.qualityLabel, r.seeders ?: 0,
                        AcqSource.LIDARR, r.protocol ?: "torrent", lidarr = r)
                }
            } catch (_: Exception) {}
            try {
                val q = "${_state.value.artist?.artistName.orEmpty()} ${al.title.orEmpty()}".trim()
                prowlarr.search(q).forEach { r ->
                    merged += AcqResult(r.title ?: "?", inferQuality(r.title), r.seeders ?: 0,
                        AcqSource.PROWLARR, r.protocol ?: "torrent", prowlarr = r)
                }
            } catch (_: Exception) {}
            merged.sortByDescending { it.seeders }
            set { it.copy(loading = false, results = merged,
                error = if (merged.isEmpty()) "No releases found." else null) }
        }
    }

    fun grab(r: AcqResult) {
        if (r.source == AcqSource.PROWLARR) { grabProwlarr(r); return }
        set { it.copy(busy = "Grabbing ${r.title.take(40)}…", error = null) }
        viewModelScope.launch {
            try {
                r.lidarr?.let { lidarr.grab(it) }
                set { it.copy(busy = "Sent to Lidarr ✓ — downloading + importing.") }
            } catch (e: Exception) { set { it.copy(busy = null, error = "Grab failed: ${e.message}") } }
        }
    }

    /** Prowlarr torrent → add paused in qBit, open the track-select sheet. */
    private fun grabProwlarr(r: AcqResult) {
        val url = r.prowlarr?.downloadUrl
        if (url.isNullOrBlank()) { set { it.copy(error = "No download URL for this release.") }; return }
        if (!qbitConfig.isConfigured) { set { it.copy(error = "qBittorrent not configured (local.properties).") }; return }
        set { it.copy(busy = "Adding to qBittorrent…", error = null) }
        viewModelScope.launch {
            try {
                val pt = qbit.addForSelection(url)
                // default: keep everything (indices 0..n-1)
                set { it.copy(busy = null, pending = pt, selected = pt.files.indices.toSet()) }
            } catch (e: Exception) { set { it.copy(busy = null, error = "qBit add failed: ${e.message}") } }
        }
    }

    fun toggleTrack(i: Int) = set {
        it.copy(selected = if (i in it.selected) it.selected - i else it.selected + i)
    }

    fun cancelSelection() {
        set { it.copy(pending = null, selected = emptySet()) }
    }

    /** Apply the track selection and start the torrent. */
    fun confirmSelection() {
        val pt = _state.value.pending ?: return
        val keep = _state.value.selected
        if (keep.isEmpty()) { set { it.copy(error = "Select at least one track.") }; return }
        set { it.copy(busy = "Starting ${keep.size}/${pt.files.size} tracks…", pending = null) }
        viewModelScope.launch {
            try {
                qbit.startWithSelection(pt.hash, pt.files.size, keep)
                set { it.copy(busy = "Downloading ${keep.size} track(s) ✓", selected = emptySet()) }
            } catch (e: Exception) {
                set { it.copy(busy = null, pending = pt, error = "Start failed: ${e.message}") }
            }
        }
    }

    fun back() = set {
        when (it.stage) {
            Stage.RESULTS -> it.copy(stage = Stage.DISCOG, results = emptyList(), busy = null, error = null)
            Stage.DISCOG -> it.copy(stage = Stage.SEARCH, albums = emptyList(), error = null)
            Stage.SEARCH -> it
        }
    }

    private fun inferQuality(title: String?): String {
        val t = (title ?: "").uppercase()
        return when {
            "FLAC" in t || "LOSSLESS" in t -> "FLAC"
            "320" in t -> "MP3 320"
            "256" in t -> "MP3 256"
            "V0" in t -> "MP3 V0"
            "MP3" in t -> "MP3"
            else -> "?"
        }
    }
}
