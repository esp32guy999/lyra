package com.resonance.music.data.lidarr

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LidarrRepository @Inject constructor(
    private val api: LidarrApi,
    private val config: LidarrConfig
) {
    suspend fun lookupArtist(term: String): List<LidarrArtist> =
        api.lookupArtist(term)

    /**
     * Add an artist to Lidarr (monitored + search for missing albums). Resolves the
     * root folder + quality/metadata profiles from the server (uses the first of each,
     * matching the common single-library setup).
     */
    suspend fun addArtist(artist: LidarrArtist): LidarrArtist {
        val rootFolder = api.getRootFolders().firstOrNull()?.path
            ?: throw IllegalStateException("No root folder configured in Lidarr")
        val qualityId = api.getQualityProfiles().firstOrNull()?.id
            ?: throw IllegalStateException("No quality profile in Lidarr")
        val metadataId = api.getMetadataProfiles().firstOrNull()?.id
            ?: throw IllegalStateException("No metadata profile in Lidarr")
        val request = AddArtistRequest(
            artistName = artist.artistName.orEmpty(),
            foreignArtistId = artist.foreignArtistId.orEmpty(),
            qualityProfileId = qualityId,
            metadataProfileId = metadataId,
            rootFolderPath = rootFolder
        )
        return api.addArtist(request)
    }

    suspend fun getAlbums(artistId: Int): List<LidarrAlbum> =
        api.getAlbums(artistId)

    suspend fun monitorAlbums(ids: List<Int>) {
        api.monitorAlbums(ids)
    }

    suspend fun searchAlbums(ids: List<Int>) {
        api.searchAlbums(SearchAlbumCommand(albumIds = ids))
    }

    /** Interactive search: parsed releases for one album (needs the album in Lidarr). */
    suspend fun searchAlbum(albumId: Int): List<LidarrRelease> = api.getReleases(albumId)

    /** Grab a specific release → Lidarr downloads + imports it. */
    suspend fun grab(release: LidarrRelease) {
        api.grabRelease(GrabReleaseRequest(release.guid.orEmpty(), release.indexerId ?: 0))
    }

    /**
     * Delete the owned track files for an album off disk (the REPLACE path). Returns how many
     * files were removed. Lidarr is the delete authority since it manages the music library.
     */
    suspend fun deleteAlbumFiles(albumId: Int): Int {
        val ids = api.getTrackFiles(albumId).map { it.id }
        if (ids.isEmpty()) return 0
        api.deleteTrackFiles(DeleteTrackFilesRequest(ids))
        return ids.size
    }
}
