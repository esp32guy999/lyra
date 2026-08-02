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

    // --- Config needed to add an artist ---

    @GET("api/v1/rootfolder")
    suspend fun getRootFolders(): List<LidarrRootFolder>

    @GET("api/v1/qualityprofile")
    suspend fun getQualityProfiles(): List<LidarrProfile>

    @GET("api/v1/metadataprofile")
    suspend fun getMetadataProfiles(): List<LidarrProfile>

    // --- Interactive search (parsed releases for one album) + grab ---

    @GET("api/v1/release")
    suspend fun getReleases(@Query("albumId") albumId: Int): List<LidarrRelease>

    @POST("api/v1/release")
    suspend fun grabRelease(@Body request: GrabReleaseRequest): Response<Unit>

    // --- Album ---

    @GET("api/v1/album")
    suspend fun getAlbums(@Query("artistId") artistId: Int): List<LidarrAlbum>

    @PUT("api/v1/album/monitor")
    suspend fun monitorAlbums(@Body albumIds: List<Int>): Response<Unit>

    // --- Track files (for REPLACE: delete the owned copy off disk before re-grabbing) ---

    @GET("api/v1/trackfile")
    suspend fun getTrackFiles(@Query("albumId") albumId: Int): List<LidarrTrackFile>

    @HTTP(method = "DELETE", path = "api/v1/trackfile/bulk", hasBody = true)
    suspend fun deleteTrackFiles(@Body request: DeleteTrackFilesRequest): Response<Unit>

    // --- Command ---

    @POST("api/v1/command")
    suspend fun searchAlbums(@Body command: SearchAlbumCommand): Response<Unit>
}

data class SearchAlbumCommand(
    val name: String = "AlbumSearch",
    val albumIds: List<Int>
)

data class LidarrTrackFile(
    val id: Int,
    val albumId: Int? = null,
    val path: String? = null
)

data class DeleteTrackFilesRequest(val trackFileIds: List<Int>)
