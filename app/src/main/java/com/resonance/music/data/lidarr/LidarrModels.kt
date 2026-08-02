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
    val foreignAlbumId: String? = null,
    val images: List<LidarrImage>? = null
) {
    /** Cover art URL for the discography row (prefer the absolute remoteUrl). */
    val coverUrl: String?
        get() = (images?.firstOrNull { it.coverType == "cover" } ?: images?.firstOrNull())
            ?.let { it.remoteUrl ?: it.url }
}

data class LidarrImage(
    val coverType: String? = null,
    val url: String? = null,
    val remoteUrl: String? = null
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

// Interactive-search release (Lidarr already parses quality/seeders from the indexer).
data class LidarrRelease(
    val guid: String? = null,
    val title: String? = null,
    val size: Long? = null,
    val seeders: Int? = null,
    val indexer: String? = null,
    val indexerId: Int? = null,
    val protocol: String? = null,          // "torrent" | "usenet"
    val quality: LidarrQualityWrap? = null
) {
    val qualityLabel: String get() = quality?.quality?.name ?: "Unknown"
}
data class LidarrQualityWrap(val quality: LidarrQualityName? = null)
data class LidarrQualityName(val name: String? = null)

data class GrabReleaseRequest(val guid: String, val indexerId: Int)
