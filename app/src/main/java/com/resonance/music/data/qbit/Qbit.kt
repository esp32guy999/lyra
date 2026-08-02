package com.resonance.music.data.qbit

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** One torrent in qBit's list (only the fields we need to locate a fresh add). */
data class QbitTorrent(
    val hash: String? = null,
    val name: String? = null,
    val added_on: Long? = null
)

/** One file inside a torrent. The 0-based list position IS the filePrio id. */
data class QbitFile(
    val name: String? = null,
    val size: Long? = null,
    val progress: Double? = null,
    val priority: Int? = null,      // 0 = skip, 1 = normal, 6/7 = high
    val index: Int? = null          // present on newer qBit; fall back to list position
) {
    /** Just the leaf filename for display. */
    val leaf: String get() = (name ?: "?").substringAfterLast('/')
}

interface QbitApi {
    @FormUrlEncoded
    @POST("api/v2/auth/login")
    suspend fun login(
        @Field("username") user: String,
        @Field("password") pass: String
    ): Response<Unit>

    @GET("api/v2/torrents/info")
    suspend fun info(): List<QbitTorrent>

    // urls = newline/pipe separated. paused (4.x) + stopped (5.x) → add without starting.
    @FormUrlEncoded
    @POST("api/v2/torrents/add")
    suspend fun add(
        @Field("urls") urls: String,
        @Field("category") category: String = "lyra",
        @Field("paused") paused: String = "true",
        @Field("stopped") stopped: String = "true"
    ): Response<Unit>

    @GET("api/v2/torrents/files")
    suspend fun files(@Query("hash") hash: String): List<QbitFile>

    // id = pipe-separated file indices; priority 0 = skip, 1 = normal.
    @FormUrlEncoded
    @POST("api/v2/torrents/filePrio")
    suspend fun filePrio(
        @Field("hash") hash: String,
        @Field("id") id: String,
        @Field("priority") priority: Int
    ): Response<Unit>

    // qBit 5.x renamed resume → start.
    @FormUrlEncoded
    @POST("api/v2/torrents/start")
    suspend fun start(@Field("hashes") hashes: String): Response<Unit>
}
