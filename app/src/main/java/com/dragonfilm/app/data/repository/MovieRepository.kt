package com.dragonfilm.app.data.repository

import com.dragonfilm.app.data.api.ApiClient
import com.dragonfilm.app.data.model.AniListNormalized
import com.dragonfilm.app.data.model.Comment
import com.dragonfilm.app.data.model.Episode
import com.dragonfilm.app.data.model.EpisodeServer
import com.dragonfilm.app.data.model.HomeResponse
import com.dragonfilm.app.data.model.Movie
import com.dragonfilm.app.data.model.NetflixItem
import com.dragonfilm.app.data.model.TMDBWeeklyItem
import com.dragonfilm.app.util.SourceNormalizer
import com.dragonfilm.app.util.SourceServer
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

data class MovieDetailResult(
    val movie: Movie,
    val availableServers: List<SourceServer>,
    val episodeServers: List<EpisodeServer>,
    val description: String
)

data class CatalogResult(
    val movies: List<Movie>,
    val totalPages: Int,
    val currentPage: Int
)

class MovieRepository {

    private val api = ApiClient.service
    private val gson = ApiClient.gson

    // In-memory caches for instant 0ms tab switching
    private var cachedHome: HomeResponse? = null
    private var cachedHomeTime: Long = 0L

    private var cachedNetflix: List<NetflixItem>? = null
    private var cachedNetflixTime: Long = 0L

    private val tmdbCache = ConcurrentHashMap<String, Pair<Long, List<TMDBWeeklyItem>>>()
    private var cachedAniListWeekly: Pair<Long, List<AniListNormalized>>? = null
    private var cachedAniListSeason: Pair<Long, Pair<String, List<AniListNormalized>>>? = null

    private val detailCache = ConcurrentHashMap<String, Pair<Long, MovieDetailResult>>()
    private val catalogCache = ConcurrentHashMap<String, Pair<Long, CatalogResult>>()

    suspend fun getHome(forceRefresh: Boolean = false): HomeResponse = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedHome != null && (now - cachedHomeTime) < 10 * 60 * 1000) {
            return@withContext cachedHome!!
        }
        try {
            val res = api.getHome()
            cachedHome = res
            cachedHomeTime = now
            res
        } catch (e: Exception) {
            cachedHome ?: HomeResponse(rows = emptyList())
        }
    }

    suspend fun getSourceList(
        server: SourceServer = SourceServer.KKPHIM,
        operation: String = "latest",
        slug: String? = null,
        page: Int = 1,
        forceRefresh: Boolean = false
    ): CatalogResult = withContext(Dispatchers.IO) {
        val cacheKey = "${server.rawValue}_${operation}_${slug ?: ""}_$page"
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            val cached = catalogCache[cacheKey]
            if (cached != null && (now - cached.first) < 5 * 60 * 1000) {
                return@withContext cached.second
            }
        }

        try {
            val path = SourceNormalizer.upstreamPath(server, operation, slug, null, page)
            val resp = api.getSourceRaw(server.rawValue, path)
            val jsonStr = resp.string()
            val result = parseCatalog(jsonStr, page)
            catalogCache[cacheKey] = Pair(now, result)
            result
        } catch (_: Exception) {
            CatalogResult(emptyList(), 1, page)
        }
    }

    suspend fun getMovieDetail(
        slug: String,
        preferredServer: SourceServer = SourceServer.KKPHIM
    ): MovieDetailResult? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = detailCache[slug]
        if (cached != null && (now - cached.first) < 5 * 60 * 1000) {
            return@withContext cached.second
        }

        val checkedServers = coroutineScope {
            SourceServer.entries.map { server ->
                async {
                    try {
                        val path = SourceNormalizer.upstreamPath(server, "detail", slug)
                        val resp = api.getSourceRaw(server.rawValue, path)
                        val jsonStr = resp.string()
                        val res = parseMovieDetailResponse(jsonStr)
                        if (res != null) Pair(server, res) else null
                    } catch (_: Exception) {
                        null
                    }
                }
            }.mapNotNull { it.await() }
        }

        if (checkedServers.isEmpty()) return@withContext null

        val available = checkedServers.map { it.first }
        val primaryPair = checkedServers.firstOrNull { it.first == preferredServer }
            ?: checkedServers.first()

        val primaryResult = primaryPair.second
        val allEpisodeServers = checkedServers.flatMap { it.second.episodeServers }

        val finalResult = MovieDetailResult(
            movie = primaryResult.movie,
            availableServers = available,
            episodeServers = if (allEpisodeServers.isNotEmpty()) allEpisodeServers else primaryResult.episodeServers,
            description = primaryResult.description
        )

        detailCache[slug] = Pair(now, finalResult)
        finalResult
    }

    suspend fun getEpisodesForServer(
        server: SourceServer,
        slug: String
    ): Pair<List<EpisodeServer>, String> = withContext(Dispatchers.IO) {
        try {
            val path = SourceNormalizer.upstreamPath(server, "detail", slug)
            val resp = api.getSourceRaw(server.rawValue, path)
            val jsonStr = resp.string()
            val res = parseMovieDetailResponse(jsonStr)
            if (res != null) {
                Pair(res.episodeServers, res.description)
            } else {
                Pair(emptyList(), "")
            }
        } catch (_: Exception) {
            Pair(emptyList(), "")
        }
    }

    suspend fun searchMovies(query: String): List<Movie> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()

        coroutineScope {
            val tasks = listOf(SourceServer.KKPHIM, SourceServer.OPHIM, SourceServer.NGUONC).map { server ->
                async {
                    try {
                        val path = SourceNormalizer.upstreamPath(server, "search", null, q, 1)
                        val resp = api.getSourceRaw(server.rawValue, path)
                        val jsonStr = resp.string()
                        parseCatalog(jsonStr, 1).movies
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }

            val all = tasks.flatMap { it.await() }
            val seen = mutableSetOf<String>()
            val unique = mutableListOf<Movie>()
            for (m in all) {
                if (m.slug.isNotEmpty() && !seen.contains(m.slug)) {
                    seen.add(m.slug)
                    unique.add(m)
                }
            }
            unique
        }
    }

    suspend fun getNetflixTop10(forceRefresh: Boolean = false): List<NetflixItem> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedNetflix != null && (now - cachedNetflixTime) < 15 * 60 * 1000) {
            return@withContext cachedNetflix!!
        }
        try {
            val res = api.getNetflixTop10()
            cachedNetflix = res.items
            cachedNetflixTime = now
            res.items
        } catch (_: Exception) {
            cachedNetflix ?: emptyList()
        }
    }

    suspend fun getTMDBWeekly(country: String, forceRefresh: Boolean = false): List<TMDBWeeklyItem> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = tmdbCache[country]
        if (!forceRefresh && cached != null && (now - cached.first) < 15 * 60 * 1000) {
            return@withContext cached.second
        }
        try {
            val res = api.getTMDBWeekly(country)
            tmdbCache[country] = Pair(now, res.items)
            res.items
        } catch (_: Exception) {
            cached?.second ?: emptyList()
        }
    }

    suspend fun getAniListWeeklyTrending(perPage: Int = 10): List<AniListNormalized> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedAniListWeekly != null && (now - cachedAniListWeekly!!.first) < 30 * 60 * 1000) {
            return@withContext cachedAniListWeekly!!.second
        }

        try {
            val query = """
                query {
                  Page(page: 1, perPage: $perPage) {
                    media(sort: TRENDING_DESC, type: ANIME, isAdult: false) {
                      id
                      title { romaji english native }
                      coverImage { extraLarge large medium color }
                      bannerImage
                      averageScore
                      popularity
                    }
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply { put("query", query) }.toString()
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            val resp = api.getAniList(requestBody)
            val jsonStr = resp.string()

            val list = mutableListOf<AniListNormalized>()
            val jsonObj = JSONObject(jsonStr)
            val mediaArray = jsonObj.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("media")
            if (mediaArray != null) {
                for (i in 0 until mediaArray.length()) {
                    val obj = mediaArray.optJSONObject(i) ?: continue
                    val titleObj = obj.optJSONObject("title")
                    val coverObj = obj.optJSONObject("coverImage")

                    val english = titleObj?.optString("english") ?: ""
                    val romaji = titleObj?.optString("romaji") ?: ""
                    val native = titleObj?.optString("native") ?: ""

                    val cover = coverObj?.optString("extraLarge")?.takeIf { it.isNotEmpty() }
                        ?: coverObj?.optString("large") ?: ""

                    list.add(
                        AniListNormalized(
                            id = obj.optInt("id", 0),
                            titleEN = english,
                            titleRomaji = romaji,
                            titleNative = native,
                            coverUrl = cover,
                            score = obj.optInt("averageScore", 0),
                            popularity = obj.optInt("popularity", 0)
                        )
                    )
                }
            }
            cachedAniListWeekly = Pair(now, list)
            list
        } catch (_: Exception) {
            cachedAniListWeekly?.second ?: emptyList()
        }
    }

    suspend fun getAniListSeasonRanking(perPage: Int = 10): Pair<String, List<AniListNormalized>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedAniListSeason != null && (now - cachedAniListSeason!!.first) < 30 * 60 * 1000) {
            return@withContext cachedAniListSeason!!.second
        }

        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val season = when (month) {
            in 1..3 -> "WINTER"
            in 4..6 -> "SPRING"
            in 7..9 -> "SUMMER"
            else -> "FALL"
        }
        val seasonLabel = when (season) {
            "WINTER" -> "Mùa Đông $year"
            "SPRING" -> "Mùa Xuân $year"
            "SUMMER" -> "Mùa Hạ $year"
            else -> "Mùa Thu $year"
        }

        try {
            val query = """
                query {
                  Page(page: 1, perPage: $perPage) {
                    media(season: $season, seasonYear: $year, sort: POPULARITY_DESC, type: ANIME, isAdult: false) {
                      id
                      title { romaji english native }
                      coverImage { extraLarge large medium color }
                      bannerImage
                      averageScore
                      popularity
                    }
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply { put("query", query) }.toString()
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            val resp = api.getAniList(requestBody)
            val jsonStr = resp.string()

            val list = mutableListOf<AniListNormalized>()
            val jsonObj = JSONObject(jsonStr)
            val mediaArray = jsonObj.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("media")
            if (mediaArray != null) {
                for (i in 0 until mediaArray.length()) {
                    val obj = mediaArray.optJSONObject(i) ?: continue
                    val titleObj = obj.optJSONObject("title")
                    val coverObj = obj.optJSONObject("coverImage")

                    val english = titleObj?.optString("english") ?: ""
                    val romaji = titleObj?.optString("romaji") ?: ""
                    val native = titleObj?.optString("native") ?: ""

                    val cover = coverObj?.optString("extraLarge")?.takeIf { it.isNotEmpty() }
                        ?: coverObj?.optString("large") ?: ""

                    list.add(
                        AniListNormalized(
                            id = obj.optInt("id", 0),
                            titleEN = english,
                            titleRomaji = romaji,
                            titleNative = native,
                            coverUrl = cover,
                            score = obj.optInt("averageScore", 0),
                            popularity = obj.optInt("popularity", 0)
                        )
                    )
                }
            }
            val result = Pair(seasonLabel, list)
            cachedAniListSeason = Pair(now, result)
            result
        } catch (_: Exception) {
            cachedAniListSeason?.second ?: Pair(seasonLabel, emptyList())
        }
    }

    suspend fun getComments(movieKey: String): List<Comment> = withContext(Dispatchers.IO) {
        try {
            api.getComments(movieKey).comments
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun postComment(
        token: String,
        movieKey: String,
        text: String,
        movieName: String
    ): Comment? = withContext(Dispatchers.IO) {
        try {
            val res = api.postComment(
                token = "Bearer $token",
                body = mapOf(
                    "movieKey" to movieKey,
                    "body" to text,
                    "movieName" to movieName
                )
            )
            if (res.ok) res.comment else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun deleteComment(token: String, commentId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val res = api.deleteComment(
                token = "Bearer $token",
                body = mapOf("commentId" to commentId)
            )
            res.ok
        } catch (_: Exception) {
            false
        }
    }

    // MARK: - Private Parsers

    private fun parseCatalog(jsonStr: String, defaultPage: Int): CatalogResult {
        val root = JSONObject(jsonStr)
        val dataObj = root.optJSONObject("data")
        val itemsArray = dataObj?.optJSONArray("items")
            ?: root.optJSONArray("items")
            ?: root.optJSONArray("movies")
            ?: dataObj?.optJSONArray("movies")

        val movies: List<Movie> = if (itemsArray != null) {
            gson.fromJson(itemsArray.toString(), object : TypeToken<List<Movie>>() {}.type) ?: emptyList()
        } else {
            emptyList()
        }

        val pagination = dataObj?.optJSONObject("params")?.optJSONObject("pagination")
        val totalPages = pagination?.optInt("totalPages", 1)
            ?: root.optInt("totalPages", 1)

        return CatalogResult(movies, totalPages, defaultPage)
    }

    private fun parseMovieDetailResponse(jsonStr: String): MovieDetailResult? {
        try {
            val root = JSONObject(jsonStr)
            val movieObj = root.optJSONObject("movie")
                ?: root.optJSONObject("data")?.optJSONObject("item")
                ?: root.optJSONObject("data")?.optJSONObject("movie")
                ?: return null

            val movie: Movie = gson.fromJson(movieObj.toString(), Movie::class.java) ?: return null
            val desc = movieObj.optString("content", "")

            val epServers = mutableListOf<EpisodeServer>()
            val epArray = root.optJSONArray("episodes")
                ?: root.optJSONObject("data")?.optJSONArray("episodes")

            if (epArray != null) {
                for (i in 0 until epArray.length()) {
                    val epObj = epArray.optJSONObject(i) ?: continue
                    val rawName = epObj.optString("server_name", "Server ${i + 1}")
                    val srvName = sanitizeServerName(rawName, i)
                    val itemsArr = epObj.optJSONArray("server_data") ?: epObj.optJSONArray("items")
                    val eps: List<Episode> = if (itemsArr != null) {
                        gson.fromJson(itemsArr.toString(), object : TypeToken<List<Episode>>() {}.type) ?: emptyList()
                    } else {
                        emptyList()
                    }
                    if (eps.isNotEmpty()) {
                        epServers.add(EpisodeServer(serverName = srvName, items = eps))
                    }
                }
            }

            return MovieDetailResult(
                movie = movie,
                availableServers = listOf(SourceServer.KKPHIM),
                episodeServers = epServers,
                description = desc
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun sanitizeServerName(rawName: String, index: Int): String {
        var s = rawName
            .replace(Regex("(?i)kkphim|ophim|nguonc|vsmov|phimmoi|dongphim|hayphim|kkp|vsm"), "")
            .replace(Regex("^[#_\\-\\s]+"), "")
            .trim()

        if (s.isEmpty()) {
            return "Server ${index + 1}"
        }
        return s
    }
}
