package com.resonance.music.data.qbit

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/** A torrent added paused, with its files, ready for track selection. */
data class PendingTorrent(val hash: String, val files: List<QbitFile>)

@Singleton
class QbitRepository @Inject constructor(
    private val api: QbitApi,
    private val config: QbitConfig
) {
    private var authed = false

    private suspend fun ensureAuth() {
        if (authed) return
        val r = api.login(config.user, config.pass)
        if (!r.isSuccessful) throw IllegalStateException("qBit login failed (${r.code()})")
        authed = true
    }

    /**
     * Add a torrent paused and return its hash + file list for selection. Since add-by-url
     * doesn't return the hash, snapshot the hash set before/after and diff to find the newcomer.
     */
    suspend fun addForSelection(downloadUrl: String): PendingTorrent {
        ensureAuth()
        val before = api.info().mapNotNull { it.hash }.toSet()
        val add = api.add(urls = downloadUrl, savepath = config.savePath)
        if (!add.isSuccessful) throw IllegalStateException("qBit add failed (${add.code()})")

        var hash: String? = null
        repeat(15) {                                   // poll up to ~7.5s for the metadata
            delay(500)
            hash = api.info().mapNotNull { it.hash }.firstOrNull { it !in before }
            if (hash != null) return@repeat
        }
        val h = hash ?: throw IllegalStateException("Couldn't locate the added torrent")

        var files: List<QbitFile> = emptyList()
        repeat(20) {                                   // files appear once metadata resolves
            files = api.files(h)
            if (files.isNotEmpty()) return@repeat
            delay(500)
        }
        return PendingTorrent(h, files)
    }

    /**
     * Apply a track selection (indices to keep) and start the torrent. Files not in [keep]
     * are set to priority 0 (skip); kept files to 1 (normal).
     */
    suspend fun startWithSelection(hash: String, total: Int, keep: Set<Int>) {
        ensureAuth()
        val skip = (0 until total).filter { it !in keep }
        if (skip.isNotEmpty()) api.filePrio(hash, skip.joinToString("|"), 0)
        if (keep.isNotEmpty()) api.filePrio(hash, keep.joinToString("|"), 1)
        api.start(hash)
    }
}
