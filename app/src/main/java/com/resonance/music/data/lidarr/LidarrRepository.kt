package com.resonance.music.data.lidarr

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LidarrRepository @Inject constructor(
    private val api: LidarrApi,
    private val config: LidarrConfig
) {
    suspend fun lookupArtist(term: String): List<LidarrArtist> {
        return api.lookupArtist(term)
    }

    suspend fun addArtist(request: AddArtistRequest): LidarrArtist {
        return api.addArtist(request)
    }

    suspend fun getAlbums(artistId: Int): List<LidarrAlbum> {
        return api.getAlbums(artistId)
    }

    suspend fun monitorAlbums(ids: List<Int>) {
        api.monitorAlbums(ids)
    }

    suspend fun searchAlbums(ids: List<Int>) {
        val command = SearchAlbumCommand(albumIds = ids)
        api.searchAlbums(command)
    }
}
