package com.resonance.music.data.lidarr

data class LidarrArtist(
    val id: Int? = null,
    val artistName: String? = null,
    val foreignArtistId: String? = null,
    val monitored: Boolean? = null,
    val overview: String? = null
)

data class LidarrAlbum(
    val id: Int? = null,
    val title: String? = null,
    val monitored: Boolean? = null,
    val artistId: Int? = null,
    val foreignAlbumId: String? = null
)

data class LidarrRootFolder(
    val id: Int? = null,
    val path: String? = null
)

// Quality + metadata profiles share this shape.
data class LidarrProfile(
    val id: Int? = null,
    val name: String? = null
)

data class AddArtistRequest(
    val artistName: String,
    val foreignArtistId: String,
    val qualityProfileId: Int,
    val metadataProfileId: Int,
    val rootFolderPath: String,
    val monitored: Boolean = true,
    val addOptions: AddArtistOptions = AddArtistOptions()
)

data class AddArtistOptions(
    val monitor: String = "all",
    val searchForMissingAlbums: Boolean = true
)
