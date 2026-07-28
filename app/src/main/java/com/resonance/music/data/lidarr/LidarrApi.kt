package com.resonance.music.data.lidarr

import retrofit2.Response
import retrofit2.http.*

interface LidarrApi {

    // --- Artist ---

    @GET("api/v1/artist/lookup")
    suspend fun lookupArtist(@Query("term") term: String): List<LidarrArtist>

    @GET("api/v1/artist")
    suspend fun getArtists(): List<LidarrArtist>

    @POST("api/v1/artist")
    suspend fun addArtist(@Body request: AddArtistRequest): LidarrArtist

    // --- Album ---

    @GET("api/v1/album")
    suspend fun getAlbums(@Query("artistId") artistId: Int): List<LidarrAlbum>

    @PUT("api/v1/album/monitor")
    suspend fun monitorAlbums(@Body albumIds: List<Int>): Response<Unit>

    // --- Command ---

    @POST("api/v1/command")
    suspend fun searchAlbums(@Body command: SearchAlbumCommand): Response<Unit>
}

data class SearchAlbumCommand(
    val name: String = "AlbumSearch",
    val albumIds: List<Int>
)
