package com.resonance.music.ui.screens.manage

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.resonance.music.ui.screens.lidarr.LidarrContent

/**
 * Mission-control container for all search / request / download features.
 * One bottom-nav tab; sub-tabs appear as more sources are added
 * (Lidarr → Add · qBittorrent → Downloads · Prowlarr → Indexers).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen() {
    val tabs = listOf("Add")   // future: "Downloads", "Indexers"
    var selected by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Manage") }) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (tabs.size > 1) {
                TabRow(selectedTabIndex = selected) {
                    tabs.forEachIndexed { i, title ->
                        Tab(
                            selected = selected == i,
                            onClick = { selected = i },
                            text = { Text(title) }
                        )
                    }
                }
            }
            when (selected) {
                0 -> LidarrContent()
                // 1 -> DownloadsContent()  // qBittorrent (Phase 3)
                // 2 -> IndexersContent()   // Prowlarr
            }
        }
    }
}
