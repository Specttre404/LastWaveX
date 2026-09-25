package com.lastwave.app.data.sponsorblock

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

data class SkipSegment(
    val category: String,
    val startMs: Long,
    val endMs: Long,
)

@Singleton
class SponsorBlockRepository @Inject constructor() {

    private val segmentCache = object : LinkedHashMap<String, List<SkipSegment>>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<SkipSegment>>?): Boolean {
            return size > 100
        }
    }

    suspend fun getSkipSegments(videoId: String): List<SkipSegment> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext emptyList()
        synchronized(segmentCache) {
            segmentCache[videoId]?.let { return@withContext it }
        }

        runCatching {
            val categoriesJson = URLEncoder.encode("[\"music_offtopic\",\"intro\",\"outro\",\"preview\",\"filler\",\"sponsor\"]", "UTF-8")
            val urlString = "https://sponsor.ajay.app/api/skipSegments?videoID=$videoId&categories=$categoriesJson"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3_000
                readTimeout = 3_000
                setRequestProperty("User-Agent", "LastWave/1.0")
            }

            if (connection.responseCode != 200) {
                connection.disconnect()
                synchronized(segmentCache) { segmentCache[videoId] = emptyList() }
                return@withContext emptyList()
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val jsonArray = JSONArray(responseText)
            val segments = mutableListOf<SkipSegment>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val category = obj.optString("category", "")
                val segArray = obj.optJSONArray("segment")
                if (segArray != null && segArray.length() >= 2) {
                    val startSec = segArray.getDouble(0)
                    val endSec = segArray.getDouble(1)
                    val startMs = (startSec * 1000).toLong()
                    val endMs = (endSec * 1000).toLong()
                    if (endMs > startMs) {
                        segments.add(SkipSegment(category, startMs, endMs))
                    }
                }
            }
            synchronized(segmentCache) { segmentCache[videoId] = segments }
            segments
        }.getOrElse { error ->
            Log.w("SponsorBlock", "Failed to fetch skip segments for $videoId: ${error.message}")
            emptyList()
        }
    }
}
