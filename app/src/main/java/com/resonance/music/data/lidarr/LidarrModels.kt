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

data class AddArtistRequest(
    val artistName: String,
    val foreignArtistId: String,
    val monitored: Boolean = true,
    val rootFolderPath: String
)
