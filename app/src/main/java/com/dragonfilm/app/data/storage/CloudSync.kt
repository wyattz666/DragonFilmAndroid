package com.dragonfilm.app.data.storage

import android.util.Log
import com.dragonfilm.app.data.api.ApiClient
import com.dragonfilm.app.data.model.HistoryItem
import com.dragonfilm.app.data.model.Movie
import com.dragonfilm.app.data.model.SavedActor
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            // 1. Refresh latest user profile (avatar, username, email, avatar_frame)
            try {
                authManager.refreshProfile()
                val refreshedUser = authManager.currentUser.value
                if (refreshedUser != null && refreshedUser.avatarFrame.isNotEmpty()) {
                    localStore.setAvatarFrame(refreshedUser.avatarFrame)
                }
            } catch (e: Exception) {
                Log.w("CloudSync", "Profile refresh warning: ${e.message}")
            }

            // 2. Pull latest user data from cloud /api/user-data
            val getResp = ApiClient.service.getUserData("Bearer $token")
            val getJsonStr = getResp.string()
            val getObj = JSONObject(getJsonStr)

            var remoteData: JSONObject? = null
            if (getObj.has("data")) {
                val dataVal = getObj.opt("data")
                if (dataVal is JSONObject) {
                    remoteData = dataVal
                } else if (dataVal is String && dataVal.startsWith("{")) {
                    remoteData = try { JSONObject(dataVal) } catch (_: Exception) { null }
                }
            } else if (getObj.has("history") || getObj.has("movieLibrary")) {
                remoteData = getObj
            }

            if (remoteData != null) {
                mergeRemote(remoteData)
            }

            localStore.refreshState()

            // 3. Build comprehensive local snapshot conforming to Web & iOS Schema v4
            val snapshot = buildLocalSnapshot()

            // 4. Push updated state back to /api/user-data
            val payload = mapOf("data" to snapshot)
            ApiClient.service.postUserData("Bearer $token", payload)

            true
        } catch (e: Exception) {
            Log.e("CloudSync", "Sync error: ${e.message}", e)
            false
        } finally {
            isSyncing = false
        }
    }

    private fun mergeRemote(remote: JSONObject) {
        val gson = ApiClient.gson

        // Merge Watch History
        val remoteHistoryRaw = remote.optJSONArray("history")
        if (remoteHistoryRaw != null && remoteHistoryRaw.length() > 0) {
            val remoteHistory: List<HistoryItem>? = try {
                gson.fromJson(
                    remoteHistoryRaw.toString(),
                    object : TypeToken<List<HistoryItem>>() {}.type
                )
            } catch (_: Exception) { null }

            if (!remoteHistory.isNullOrEmpty()) {
                val localHistory = localStore.getHistory()
                val historyMap = mutableMapOf<String, HistoryItem>()

                for (item in localHistory) {
                    if (item.slug.isNotEmpty()) {
                        historyMap[item.slug] = item
                    }
                }

                for (item in remoteHistory) {
                    if (item.slug.isNotEmpty()) {
                        val existing = historyMap[item.slug]
                        if (existing == null || item.normalizedWatchedAt > existing.normalizedWatchedAt) {
                            historyMap[item.slug] = item
                        }
                    }
                }

                val sorted = historyMap.values
                    .sortedByDescending { it.normalizedWatchedAt }
                    .take(50)

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
            val remoteList: List<Movie>? = try {
                gson.fromJson(
                    watchLaterArray.toString(),
                    object : TypeToken<List<Movie>>() {}.type
                )
            } catch (_: Exception) { null }

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
            val remoteList: List<Movie>? = try {
                gson.fromJson(
                    likedArray.toString(),
                    object : TypeToken<List<Movie>>() {}.type
                )
            } catch (_: Exception) { null }

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
            val remoteList: List<SavedActor>? = try {
                gson.fromJson(
                    actorArray.toString(),
                    object : TypeToken<List<SavedActor>>() {}.type
                )
            } catch (_: Exception) { null }

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

        // Merge Avatar Frame
        val remoteFrame = remote.optString("avatar_frame", "")
        if (remoteFrame.isNotEmpty()) {
            localStore.setAvatarFrame(remoteFrame)
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
            "avatar_frame" to localStore.getAvatarFrame(),
            "history" to localStore.getHistory().map { item ->
                mapOf(
                    "slug" to item.slug,
                    "name" to item.name,
                    "year" to item.year,
                    "_server" to item.server,
                    "poster_url" to item.posterUrl,
                    "resume_key" to item.resumeKey.ifEmpty { "${item.server}_${item.slug}_${item.episodeSlug}" },
                    "source_name" to item.sourceName,
                    "episode_name" to item.episodeName,
                    "episode_slug" to item.episodeSlug,
                    "episode_server_name" to item.episodeServerName,
                    "episode_server_idx" to item.episodeServerIdx,
                    "episode_index0" to item.episodeIndex0,
                    "episode_number" to item.episodeNumber,
                    "watched_seconds" to item.watchedSeconds,
                    "duration_seconds" to item.durationSeconds,
                    "progress_percent" to item.progressPercent,
                    "watchedAt" to item.normalizedWatchedAt
                )
            },
            "resumeTimes" to localStore.getResumeTimes(),
            "movieLibrary" to mapOf(
                "watchLater" to localStore.getWatchLater(),
                "liked" to localStore.getLikedMovies()
            ),
            "actorLibrary" to localStore.getFavoriteActors().map { actor ->
                mapOf(
                    "name" to actor.name,
                    "character" to actor.character,
                    "profile_url" to actor.profileUrl,
                    "addedAt" to actor.normalizedAddedAt
                )
            }
        )
    }
}
