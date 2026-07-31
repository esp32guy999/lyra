package com.resonance.music.ui.screens.lidarr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Lidarr artist search + add/monitor. Hosted inside the Manage tab (no Scaffold of its own). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LidarrContent(
    viewModel: LidarrViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            label = { Text("Search artists") },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { viewModel.search() }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            },
            keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Spacer(Modifier.height(8.dp))

        when {
            state.loading -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Text(
                state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
            state.results.isEmpty() -> Text(
                "Search Lidarr for an artist to add or monitor.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp)
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.results) { artist ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    artist.artistName ?: "Unknown",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                artist.overview?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        it.take(160),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            val fid = artist.foreignArtistId
                            when {
                                fid != null && fid in state.added -> Text(
                                    "Added ✓",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                fid != null && fid in state.adding ->
                                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                else -> FilledTonalButton(onClick = { viewModel.addArtist(artist) }) {
                                    Text("Add")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
