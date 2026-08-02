package com.resonance.music.ui.screens.lidarr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Discovery + acquisition drill-down (search → discography → merged results). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LidarrContent(viewModel: LidarrViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // header / search
        if (s.stage == Stage.SEARCH) {
            OutlinedTextField(
                value = s.query, onValueChange = viewModel::onQueryChange,
                label = { Text("Search artists") }, singleLine = true,
                trailingIcon = { IconButton(onClick = { viewModel.search() }) { Icon(Icons.Default.Search, "Search") } },
                keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        } else {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text(
                    if (s.stage == Stage.DISCOG) s.artist?.artistName ?: "" else s.album?.title ?: "",
                    style = MaterialTheme.typography.titleMedium, maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        s.busy?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp)) }

        when {
            s.loading -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator() }
            s.error != null -> Text(s.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
            s.stage == Stage.SEARCH -> ArtistList(s, viewModel)
            s.stage == Stage.DISCOG -> DiscogList(s, viewModel)
            s.stage == Stage.RESULTS -> ResultList(s, viewModel)
        }
    }

    if (s.pending != null) TrackSelectDialog(s, viewModel)
}

@Composable
private fun TrackSelectDialog(s: LidarrUiState, vm: LidarrViewModel) {
    val files = s.pending?.files ?: return
    AlertDialog(
        onDismissRequest = { vm.cancelSelection() },
        title = { Text("Select tracks (${s.selected.size}/${files.size})") },
        text = {
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                itemsIndexed(files) { i, f ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.toggleTrack(i) }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = i in s.selected, onCheckedChange = { vm.toggleTrack(i) })
                        Text(f.leaf, style = MaterialTheme.typography.bodySmall, maxLines = 2,
                            modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.confirmSelection() }) { Text("Download") } },
        dismissButton = { TextButton(onClick = { vm.cancelSelection() }) { Text("Cancel") } }
    )
}

@Composable
private fun ArtistList(s: LidarrUiState, vm: LidarrViewModel) {
    if (s.artists.isEmpty()) { Hint("Search Lidarr for an artist."); return }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(s.artists) { a ->
            ElevatedCard(Modifier.fillMaxWidth().clickable { vm.openArtist(a) }) {
                Column(Modifier.padding(12.dp)) {
                    Text(a.artistName ?: "Unknown", style = MaterialTheme.typography.titleMedium)
                    a.overview?.takeIf { it.isNotBlank() }?.let {
                        Text(it.take(140), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscogList(s: LidarrUiState, vm: LidarrViewModel) {
    if (s.albums.isEmpty()) { Hint("No releases."); return }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(s.albums) { al ->
            ElevatedCard(Modifier.fillMaxWidth().clickable { vm.openAlbum(al) }) {
                Text(al.title ?: "Untitled", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun ResultList(s: LidarrUiState, vm: LidarrViewModel) {
    if (s.results.isEmpty()) { Hint("No releases found."); return }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(s.results) { r ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Badge(if (r.source == AcqSource.LIDARR) "Lidarr" else "Prowlarr",
                                if (r.source == AcqSource.LIDARR) Color(0xFF2E7D32) else Color(0xFF1565C0))
                            Text(r.quality, style = MaterialTheme.typography.labelMedium)
                            Text("· ${r.seeders} seeders", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = { vm.grab(r) }) { Text("Grab") }
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Surface(color = color, shape = MaterialTheme.shapes.small) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun Hint(t: String) =
    Text(t, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp))
