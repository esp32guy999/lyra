package com.resonance.music.data.prowlarr

import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

// Raw indexer hit from Prowlarr's search (unparsed — quality inferred from title).
data class ProwlarrResult(
    val title: String? = null,
    val size: Long? = null,
    val seeders: Int? = null,
    val indexer: String? = null,
    val indexerId: Int? = null,
    val downloadUrl: String? = null,
    val guid: String? = null,
    val protocol: String? = null           // "torrent" | "usenet"
)

interface ProwlarrApi {
    // categories 3000 = Audio/Music
    @GET("api/v1/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("categories") categories: Int = 3000,
        @Query("type") type: String = "search"
    ): List<ProwlarrResult>
}

@Singleton
class ProwlarrRepository @Inject constructor(private val api: ProwlarrApi) {
    suspend fun search(query: String): List<ProwlarrResult> = api.search(query)
}
