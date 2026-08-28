package com.dragonfilm.app.data.storage

import android.content.Context
import com.dragonfilm.app.data.api.ApiClient
import com.dragonfilm.app.data.model.HistoryItem
import com.dragonfilm.app.data.model.Movie
import com.dragonfilm.app.data.model.PersonRef
import com.dragonfilm.app.data.model.SavedActor
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.lang.reflect.Type

class LocalStore(private val context: Context) {

    private val gson = ApiClient.gson
    private val dir = context.filesDir

    private val _historyFlow = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyFlow: StateFlow<List<HistoryItem>> = _historyFlow.asStateFlow()

    private val _likedFlow = MutableStateFlow<List<Movie>>(emptyList())
    val likedFlow: StateFlow<List<Movie>> = _likedFlow.asStateFlow()

    private val _watchLaterFlow = MutableStateFlow<List<Movie>>(emptyList())
    val watchLaterFlow: StateFlow<List<Movie>> = _watchLaterFlow.asStateFlow()

    private val _actorsFlow = MutableStateFlow<List<SavedActor>>(emptyList())
    val actorsFlow: StateFlow<List<SavedActor>> = _actorsFlow.asStateFlow()

    init {
        refreshState()
    }

    fun refreshState() {
        _historyFlow.value = getHistory()
        _likedFlow.value = getLikedMovies()
        _watchLaterFlow.value = getWatchLater()
        _actorsFlow.value = getFavoriteActors()
    }

    // MARK: - History

    fun getHistory(): List<HistoryItem> {
        val list: List<HistoryItem> = load("history.json", object : TypeToken<List<HistoryItem>>() {}.type) ?: emptyList()
        val seen = mutableSetOf<String>()
        val unique = mutableListOf<HistoryItem>()
        for (item in list) {
            if (item.slug.isNotEmpty() && !seen.contains(item.slug)) {
                seen.add(item.slug)
                unique.add(item)
            }
        }
        return unique
    }

    fun saveHistory(list: List<HistoryItem>) {
        save("history.json", list)
        _historyFlow.value = list
    }

    fun addToHistory(item: HistoryItem) {
        if (item.slug.isEmpty()) return
        val list = getHistory().toMutableList()
        list.removeAll { it.slug == item.slug }
        list.add(0, item)
        val trimmed = if (list.size > 50) list.take(50) else list
        save("history.json", trimmed)
        _historyFlow.value = trimmed
    }

    fun clearHistory() {
        save("history.json", emptyList<HistoryItem>())
        _historyFlow.value = emptyList()
    }

    // MARK: - Watch Later

    fun getWatchLater(): List<Movie> {
        return load("watchLater.json", object : TypeToken<List<Movie>>() {}.type) ?: emptyList()
    }

    fun saveWatchLater(list: List<Movie>) {
        save("watchLater.json", list)
        _watchLaterFlow.value = list
    }

    fun isWatchLater(slug: String): Boolean {
        return getWatchLater().any { it.slug == slug }
    }

    fun toggleWatchLater(movie: Movie) {
        val list = getWatchLater().toMutableList()
        val idx = list.indexOfFirst { it.slug == movie.slug }
        if (idx >= 0) {
            list.removeAt(idx)
        } else {
            list.add(0, movie)
        }
        val trimmed = if (list.size > 200) list.take(200) else list
        save("watchLater.json", trimmed)
        _watchLaterFlow.value = trimmed
    }

    fun clearWatchLater() {
        save("watchLater.json", emptyList<Movie>())
        _watchLaterFlow.value = emptyList()
    }

    // MARK: - Liked Movies

    fun getLikedMovies(): List<Movie> {
        return load("liked.json", object : TypeToken<List<Movie>>() {}.type) ?: emptyList()
    }

    fun saveLiked(list: List<Movie>) {
        save("liked.json", list)
        _likedFlow.value = list
    }

    fun isLiked(slug: String): Boolean {
        return getLikedMovies().any { it.slug == slug }
    }

    fun toggleLiked(movie: Movie) {
        val list = getLikedMovies().toMutableList()
        val idx = list.indexOfFirst { it.slug == movie.slug }
        if (idx >= 0) {
            list.removeAt(idx)
        } else {
            list.add(0, movie)
        }
        val trimmed = if (list.size > 200) list.take(200) else list
        save("liked.json", trimmed)
        _likedFlow.value = trimmed
    }

    fun clearLiked() {
        save("liked.json", emptyList<Movie>())
        _likedFlow.value = emptyList()
    }

    // MARK: - Favorite Actors

    fun getFavoriteActors(): List<SavedActor> {
        return load("actors.json", object : TypeToken<List<SavedActor>>() {}.type) ?: emptyList()
    }

    fun saveActors(list: List<SavedActor>) {
        save("actors.json", list)
        _actorsFlow.value = list
    }

    fun isFavoriteActor(name: String): Boolean {
        return getFavoriteActors().any { it.name == name }
    }

    fun toggleFavoriteActor(person: PersonRef) {
        if (person.name.isEmpty()) return
        val list = getFavoriteActors().toMutableList()
        val idx = list.indexOfFirst { it.name == person.name }
        if (idx >= 0) {
            list.removeAt(idx)
        } else {
            list.add(
                0,
                SavedActor(
                    name = person.name,
                    character = person.character,
                    profileUrl = person.profileUrl
                )
            )
        }
        val trimmed = if (list.size > 200) list.take(200) else list
        save("actors.json", trimmed)
        _actorsFlow.value = trimmed
    }

    fun clearActors() {
        save("actors.json", emptyList<SavedActor>())
        _actorsFlow.value = emptyList()
    }

    // MARK: - Resume Times

    fun getResumeTimes(): Map<String, Double> {
        return load("resumeTimes.json", object : TypeToken<Map<String, Double>>() {}.type) ?: emptyMap()
    }

    fun getResumeTime(slug: String): Double {
        return getResumeTimes()[slug] ?: 0.0
    }

    fun setResumeTime(slug: String, seconds: Double) {
        if (slug.isEmpty()) return
        val map = getResumeTimes().toMutableMap()
        map[slug] = seconds
        save("resumeTimes.json", map)
    }

    fun setResumeTimes(times: Map<String, Double>) {
        save("resumeTimes.json", times)
    }

    // MARK: - Search History

    fun getRecentSearches(): List<String> {
        return load("recentSearches.json", object : TypeToken<List<String>>() {}.type) ?: emptyList()
    }

    fun addSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        val list = getRecentSearches().toMutableList()
        list.removeAll { it.equals(q, ignoreCase = true) }
        list.add(0, q)
        save("recentSearches.json", list.take(20))
    }

    fun clearRecentSearches() {
        save("recentSearches.json", emptyList<String>())
    }

    // MARK: - Private JSON Helpers

    private fun <T> save(filename: String, data: T) {
        try {
            val file = File(dir, filename)
            val json = gson.toJson(data)
            file.writeText(json)
        } catch (_: Exception) {}
    }

    private fun <T> load(filename: String, type: Type): T? {
        return try {
            val file = File(dir, filename)
            if (!file.exists()) return null
            val json = file.readText()
            gson.fromJson<T>(json, type)
        } catch (_: Exception) {
            null
        }
    }
}
