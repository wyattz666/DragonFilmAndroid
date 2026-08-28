package com.dragonfilm.app.data.storage

import com.dragonfilm.app.data.api.ApiClient
import com.dragonfilm.app.data.model.HistoryItem
import com.dragonfilm.app.data.model.Movie
import com.dragonfilm.app.data.model.SavedActor
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CloudSync(
    private val localStore: LocalStore,
    private val authManager: AuthManager
) {
    private var isSyncing = false

    suspend fun sync(): Boolean = withContext(Dispatchers.IO) {
        val token = authManager.token.value ?: return@withContext false
        if (isSyncing) return@withContext false
        isSyncing = true
        try {
            // 1. Pull latest from cloud /api/user-data
            val getResp = ApiClient.service.getUserData("Bearer $token")
            val getJsonStr = getResp.string()
            val getObj = JSONObject(getJsonStr)
            if (getObj.optBoolean("ok", true) && getObj.has("data")) {
                val remoteData = getObj.optJSONObject("data")
                if (remoteData != null) {
                    mergeRemote(remoteData)
                }
            }

            localStore.refreshState()

            // 2. Build local snapshot
            val snapshot = buildLocalSnapshot()

            // 3. Push to /api/user-data
            val payload = mapOf("data" to snapshot)
            ApiClient.service.postUserData("Bearer $token", payload)

            true
        } catch (_: Exception) {
            false
        } finally {
            isSyncing = false
        }
    }

    private fun mergeRemote(remote: JSONObject) {
        val gson = ApiClient.gson

        // Merge History
        val remoteHistoryRaw = remote.optJSONArray("history")
        if (remoteHistoryRaw != null && remoteHistoryRaw.length() > 0) {
            val remoteHistory: List<HistoryItem>? = gson.fromJson(
                remoteHistoryRaw.toString(),
                object : TypeToken<List<HistoryItem>>() {}.type
            )
            if (!remoteHistory.isNullOrEmpty()) {
                val localHistory = localStore.getHistory()
                val historyMap = mutableMapOf<String, HistoryItem>()
                for (item in localHistory) {
                    if (item.slug.isNotEmpty()) historyMap[item.slug] = item
                }
                for (item in remoteHistory) {
                    if (item.slug.isNotEmpty()) {
                        val existing = historyMap[item.slug]
                        if (existing == null || item.watchedAt > existing.watchedAt) {
                            historyMap[item.slug] = item
                        }
                    }
                }
                val sorted = historyMap.values.sortedByDescending { it.watchedAt }.take(50)
                localStore.saveHistory(sorted)
            }
        }

        // Merge Resume Times
        val remoteResumeTimes = remote.optJSONObject("resumeTimes")
        if (remoteResumeTimes != null) {
            val localTimes = localStore.getResumeTimes().toMutableMap()
            val keys = remoteResumeTimes.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = remoteResumeTimes.optDouble(k, 0.0)
                if (v > (localTimes[k] ?: 0.0)) {
                    localTimes[k] = v
                }
            }
            localStore.setResumeTimes(localTimes)
        }

        // Merge Movie Library (Watch Later & Liked)
        var watchLaterArray = remote.optJSONArray("watchLater")
        var likedArray = remote.optJSONArray("liked")
        val movieLib = remote.optJSONObject("movieLibrary")
        if (movieLib != null) {
            if (watchLaterArray == null) watchLaterArray = movieLib.optJSONArray("watchLater")
            if (likedArray == null) likedArray = movieLib.optJSONArray("liked")
        }

        if (watchLaterArray != null && watchLaterArray.length() > 0) {
            val remoteList: List<Movie>? = gson.fromJson(
                watchLaterArray.toString(),
                object : TypeToken<List<Movie>>() {}.type
            )
            if (!remoteList.isNullOrEmpty()) {
                val map = localStore.getWatchLater().associateBy { it.slug }.toMutableMap()
                for (m in remoteList) {
                    if (m.slug.isNotEmpty() && !map.containsKey(m.slug)) {
                        map[m.slug] = m
                    }
                }
                localStore.saveWatchLater(map.values.toList().take(200))
            }
        }

        if (likedArray != null && likedArray.length() > 0) {
            val remoteList: List<Movie>? = gson.fromJson(
                likedArray.toString(),
                object : TypeToken<List<Movie>>() {}.type
            )
            if (!remoteList.isNullOrEmpty()) {
                val map = localStore.getLikedMovies().associateBy { it.slug }.toMutableMap()
                for (m in remoteList) {
                    if (m.slug.isNotEmpty() && !map.containsKey(m.slug)) {
                        map[m.slug] = m
                    }
                }
                localStore.saveLiked(map.values.toList().take(200))
            }
        }

        // Merge Actor Library
        val actorArray = remote.optJSONArray("actorLibrary") ?: remote.optJSONArray("actors")
        if (actorArray != null && actorArray.length() > 0) {
            val remoteList: List<SavedActor>? = gson.fromJson(
                actorArray.toString(),
                object : TypeToken<List<SavedActor>>() {}.type
            )
            if (!remoteList.isNullOrEmpty()) {
                val map = localStore.getFavoriteActors().associateBy { it.name }.toMutableMap()
                for (a in remoteList) {
                    if (a.name.isNotEmpty() && !map.containsKey(a.name)) {
                        map[a.name] = a
                    }
                }
                localStore.saveActors(map.values.toList().take(200))
            }
        }
    }

    private fun buildLocalSnapshot(): Map<String, Any> {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        return mapOf(
            "app" to "dragonfilm",
            "type" to "cloud-data",
            "version" to 4,
            "savedAt" to sdf.format(Date()),
            "history" to localStore.getHistory(),
            "resumeTimes" to localStore.getResumeTimes(),
            "movieLibrary" to mapOf(
                "watchLater" to localStore.getWatchLater(),
                "liked" to localStore.getLikedMovies()
            ),
            "actorLibrary" to localStore.getFavoriteActors()
        )
    }
}
