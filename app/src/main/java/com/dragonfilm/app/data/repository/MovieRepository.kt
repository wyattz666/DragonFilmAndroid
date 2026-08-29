package com.dragonfilm.app.data.repository

import com.dragonfilm.app.data.api.ApiClient
import com.dragonfilm.app.data.model.AniListNormalized
import com.dragonfilm.app.data.model.Comment
import com.dragonfilm.app.data.model.Episode
import com.dragonfilm.app.data.model.EpisodeServer
import com.dragonfilm.app.data.model.Genre
import com.dragonfilm.app.data.model.HomeResponse
import com.dragonfilm.app.data.model.Movie
import com.dragonfilm.app.data.model.NetflixItem
import com.dragonfilm.app.data.model.PersonRef
import com.dragonfilm.app.data.model.TMDBInfo
import com.dragonfilm.app.data.model.TMDBWeeklyItem
import com.dragonfilm.app.util.SourceNormalizer
import com.dragonfilm.app.util.SourceServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
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
            val result = parseCatalog(jsonStr, page, server)
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
        val trimmedSlug = slug.trim()
        if (trimmedSlug.isEmpty()) return@withContext null

        val now = System.currentTimeMillis()
        val cached = detailCache[trimmedSlug]
        if (cached != null && (now - cached.first) < 5 * 60 * 1000) {
            return@withContext cached.second
        }

        // 1. Direct Lookup across available servers
        val activeServers = listOf(SourceServer.KKPHIM, SourceServer.VSMOV, SourceServer.NGUONC)
        val checkedServers = coroutineScope {
            activeServers.map { server ->
                async {
                    try {
                        val path = SourceNormalizer.upstreamPath(server, "detail", trimmedSlug)
                        val resp = api.getSourceRaw(server.rawValue, path)
                        val jsonStr = resp.string()
                        val res = parseMovieDetailResponse(jsonStr, server)
                        if (res != null) Pair(server, res) else null
                    } catch (_: Exception) {
                        null
                    }
                }
            }.mapNotNull { it.await() }
        }

        if (checkedServers.isNotEmpty()) {
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

            detailCache[trimmedSlug] = Pair(now, finalResult)
            return@withContext finalResult
        }

        // 2. Search Fallback: If not found directly, search by keyword/title
        val searchQuery = trimmedSlug.replace("-", " ")
        val searchResults = searchMovies(searchQuery)
        if (searchResults.isNotEmpty()) {
            val matchedSlug = searchResults.first().slug
            if (matchedSlug.isNotEmpty() && matchedSlug != trimmedSlug) {
                val matchedDetail = getMovieDetail(matchedSlug, preferredServer)
                if (matchedDetail != null) {
                    detailCache[trimmedSlug] = Pair(now, matchedDetail)
                    return@withContext matchedDetail
                }
            }
        }

        null
    }

    suspend fun getEpisodesForServer(
        server: SourceServer,
        slug: String
    ): Pair<List<EpisodeServer>, String> = withContext(Dispatchers.IO) {
        try {
            val path = SourceNormalizer.upstreamPath(server, "detail", slug)
            val resp = api.getSourceRaw(server.rawValue, path)
            val jsonStr = resp.string()
            val res = parseMovieDetailResponse(jsonStr, server)
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

        val searchServers = listOf(SourceServer.KKPHIM, SourceServer.VSMOV, SourceServer.NGUONC)
        coroutineScope {
            val tasks = searchServers.map { server ->
                async {
                    try {
                        val path = SourceNormalizer.upstreamPath(server, "search", null, q, 1)
                        val resp = api.getSourceRaw(server.rawValue, path)
                        val jsonStr = resp.string()
                        parseCatalog(jsonStr, 1, server).movies
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

    suspend fun getAniListWeeklyTrending(perPage: Int = 10): List<AniListNormalized> = getAniListTrendingWeekly(perPage)

    suspend fun getAniListTrendingWeekly(perPage: Int = 10): List<AniListNormalized> = withContext(Dispatchers.IO) {
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

    private fun parseCatalog(jsonStr: String, defaultPage: Int, server: SourceServer): CatalogResult {
        if (!jsonStr.startsWith("{")) return CatalogResult(emptyList(), 1, defaultPage)
        try {
            val root = JSONObject(jsonStr)
            val dataObj = root.optJSONObject("data")
            val itemsArray = dataObj?.optJSONArray("items")
                ?: root.optJSONArray("items")
                ?: root.optJSONArray("movies")
                ?: dataObj?.optJSONArray("movies")

            val movies = mutableListOf<Movie>()
            if (itemsArray != null) {
                for (i in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.optJSONObject(i) ?: continue
                    movies.add(parseMovieObject(itemObj, server))
                }
            }

            val pagination = dataObj?.optJSONObject("params")?.optJSONObject("pagination")
                ?: root.optJSONObject("paginate")
            val totalPages = pagination?.optInt("totalPages", 1)
                ?: pagination?.optInt("total_page", 1)
                ?: root.optInt("totalPages", 1)

            return CatalogResult(movies, totalPages, defaultPage)
        } catch (_: Exception) {
            return CatalogResult(emptyList(), 1, defaultPage)
        }
    }

    private fun parseMovieDetailResponse(jsonStr: String, server: SourceServer): MovieDetailResult? {
        if (!jsonStr.startsWith("{")) return null
        try {
            val root = JSONObject(jsonStr)
            val movieObj = root.optJSONObject("movie")
                ?: root.optJSONObject("data")?.optJSONObject("item")
                ?: root.optJSONObject("data")?.optJSONObject("movie")
                ?: root.optJSONObject("item")
                ?: return null

            val movie = parseMovieObject(movieObj, server)
            val desc = movieObj.optString("content", "")
                .ifEmpty { movieObj.optString("description", "") }

            val epServers = mutableListOf<EpisodeServer>()
            val epArray = root.optJSONArray("episodes")
                ?: root.optJSONObject("data")?.optJSONArray("episodes")
                ?: root.optJSONObject("data")?.optJSONObject("item")?.optJSONArray("episodes")
                ?: movieObj.optJSONArray("episodes")

            if (epArray != null) {
                for (i in 0 until epArray.length()) {
                    val epObj = epArray.optJSONObject(i) ?: continue
                    val rawName = epObj.optString("server_name", "Server ${i + 1}")
                    val srvName = sanitizeServerName(rawName, i)
                    val itemsArr = epObj.optJSONArray("server_data")
                        ?: epObj.optJSONArray("items")

                    val eps = mutableListOf<Episode>()
                    if (itemsArr != null) {
                        for (j in 0 until itemsArr.length()) {
                            val itemObj = itemsArr.optJSONObject(j) ?: continue
                            val name = itemObj.optString("name", "Tập ${j + 1}")
                            val epSlug = itemObj.optString("slug", "tap-${j + 1}")
                            val filename = itemObj.optString("filename", "").takeIf { it.isNotEmpty() }
                            val rawM3u8 = itemObj.optString("link_m3u8", "").ifEmpty { itemObj.optString("m3u8", "") }
                            val rawEmbed = itemObj.optString("link_embed", "").ifEmpty { itemObj.optString("embed", "") }

                            var m3u8 = rawM3u8.takeIf { it.isNotEmpty() }
                            if (m3u8 == null && rawEmbed.contains("streamvsmov.com/video/")) {
                                m3u8 = rawEmbed.replace("/video/", "/stream/") + "/master.m3u8"
                            }

                            eps.add(
                                Episode(
                                    id = epSlug.ifEmpty { "ep-$j" },
                                    name = name,
                                    slug = epSlug,
                                    filename = filename,
                                    linkM3U8 = m3u8,
                                    linkEmbed = rawEmbed.takeIf { it.isNotEmpty() }
                                )
                            )
                        }
                    }

                    if (eps.isNotEmpty()) {
                        epServers.add(EpisodeServer(serverName = srvName, items = eps))
                    }
                }
            }

            return MovieDetailResult(
                movie = movie,
                availableServers = listOf(server),
                episodeServers = epServers,
                description = desc
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseMovieObject(obj: JSONObject, server: SourceServer?): Movie {
        val slug = obj.optString("slug", "")
        val name = obj.optString("name", "").ifEmpty { obj.optString("title", slug) }
        val originName = obj.optString("origin_name", "").ifEmpty { obj.optString("original_name", "") }
        var thumb = obj.optString("thumb_url", "").ifEmpty { obj.optString("thumb", "") }
        var poster = obj.optString("poster_url", "").ifEmpty { obj.optString("poster", "") }
        val yearRaw = obj.opt("year")
        val type = obj.optString("type", "single")
        val episodeCurrent = obj.optString("episode_current", "").ifEmpty { obj.optString("current_episode", "") }
        val quality = obj.optString("quality", "").takeIf { it.isNotEmpty() }
        val lang = obj.optString("lang", "").ifEmpty { obj.optString("language", "") }.takeIf { it.isNotEmpty() }

        // Categories / Genres
        val categories = mutableListOf<Genre>()
        val catVal = obj.opt("category") ?: obj.opt("genres") ?: obj.opt("the_loai")
        if (catVal is JSONArray) {
            for (i in 0 until catVal.length()) {
                val item = catVal.opt(i)
                if (item is JSONObject) {
                    categories.add(Genre(name = item.optString("name", ""), slug = item.optString("slug", "")))
                } else if (item is String && item.isNotBlank()) {
                    categories.add(Genre(name = item.trim(), slug = item.trim().lowercase()))
                }
            }
        } else if (catVal is String && catVal.isNotBlank()) {
            catVal.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                categories.add(Genre(name = it, slug = it.lowercase()))
            }
        }

        // Countries
        val countries = mutableListOf<Genre>()
        val countryVal = obj.opt("country") ?: obj.opt("quoc_gia")
        if (countryVal is JSONArray) {
            for (i in 0 until countryVal.length()) {
                val item = countryVal.opt(i)
                if (item is JSONObject) {
                    countries.add(Genre(name = item.optString("name", ""), slug = item.optString("slug", "")))
                } else if (item is String && item.isNotBlank()) {
                    countries.add(Genre(name = item.trim(), slug = item.trim().lowercase()))
                }
            }
        } else if (countryVal is String && countryVal.isNotBlank()) {
            countryVal.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                countries.add(Genre(name = it, slug = it.lowercase()))
            }
        }

        // Actors
        val actors = mutableListOf<PersonRef>()
        val actorVal = obj.opt("actor") ?: obj.opt("actors") ?: obj.opt("dien_vien")
        if (actorVal is JSONArray) {
            for (i in 0 until actorVal.length()) {
                val item = actorVal.opt(i)
                if (item is JSONObject) {
                    actors.add(PersonRef(
                        name = item.optString("name", ""),
                        character = item.optString("character", ""),
                        profileUrl = item.optString("profile_url", "").ifEmpty { item.optString("profile_path", "") }
                    ))
                } else if (item is String && item.isNotBlank()) {
                    actors.add(PersonRef(name = item.trim()))
                }
            }
        } else if (actorVal is String && actorVal.isNotBlank()) {
            actorVal.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                actors.add(PersonRef(name = it))
            }
        }

        // Directors
        val directors = mutableListOf<PersonRef>()
        val directorVal = obj.opt("director") ?: obj.opt("directors") ?: obj.opt("dao_dien")
        if (directorVal is JSONArray) {
            for (i in 0 until directorVal.length()) {
                val item = directorVal.opt(i)
                if (item is JSONObject) {
                    directors.add(PersonRef(
                        name = item.optString("name", ""),
                        character = item.optString("character", ""),
                        profileUrl = item.optString("profile_url", "").ifEmpty { item.optString("profile_path", "") }
                    ))
                } else if (item is String && item.isNotBlank()) {
                    directors.add(PersonRef(name = item.trim()))
                }
            }
        } else if (directorVal is String && directorVal.isNotBlank()) {
            directorVal.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                directors.add(PersonRef(name = it))
            }
        }

        // TMDB
        val tmdbObj = obj.optJSONObject("tmdb")
        val tmdb = if (tmdbObj != null) {
            TMDBInfo(
                id = tmdbObj.opt("id"),
                type = tmdbObj.optString("type").takeIf { it.isNotEmpty() },
                season = tmdbObj.opt("season"),
                voteAverage = tmdbObj.opt("vote_average") ?: tmdbObj.opt("voteAverage"),
                voteCount = tmdbObj.opt("vote_count") ?: tmdbObj.opt("voteCount"),
                posterUrl = tmdbObj.optString("poster_url").ifEmpty { tmdbObj.optString("poster_path") }.takeIf { it.isNotEmpty() },
                backdropUrl = tmdbObj.optString("backdrop_url").ifEmpty { tmdbObj.optString("backdrop_path") }.takeIf { it.isNotEmpty() },
                thumbUrl = tmdbObj.optString("thumb_url").takeIf { it.isNotEmpty() }
            )
        } else null

        // IMDB
        val imdbObj = obj.optJSONObject("imdb")
        val imdb = if (imdbObj != null) {
            TMDBInfo(
                id = imdbObj.opt("id"),
                type = imdbObj.optString("type").takeIf { it.isNotEmpty() },
                season = null,
                voteAverage = imdbObj.opt("vote_average") ?: imdbObj.opt("voteAverage") ?: imdbObj.opt("rate"),
                voteCount = imdbObj.opt("vote_count") ?: imdbObj.opt("voteCount") ?: imdbObj.opt("votes"),
                posterUrl = null,
                backdropUrl = null,
                thumbUrl = null
            )
        } else null

        if (server != null) {
            if (poster.isNotEmpty() && !poster.startsWith("http")) {
                poster = resolveImageUrl(poster, server)
            }
            if (thumb.isNotEmpty() && !thumb.startsWith("http")) {
                thumb = resolveImageUrl(thumb, server)
            }
        }

        return Movie(
            slug = slug,
            name = name,
            originName = originName,
            thumbUrl = thumb,
            posterUrl = poster,
            yearRaw = yearRaw,
            type = type,
            episodeCurrent = episodeCurrent,
            quality = quality,
            lang = lang,
            category = categories,
            country = countries,
            actor = actors.takeIf { it.isNotEmpty() },
            director = directors.takeIf { it.isNotEmpty() },
            tmdb = tmdb,
            imdb = imdb,
            server = server?.rawValue
        )
    }

    private fun resolveImageUrl(path: String, server: SourceServer): String {
        if (path.startsWith("http")) return path
        val clean = if (path.startsWith("/")) path else "/$path"
        return when (server) {
            SourceServer.KKPHIM -> "https://phimimg.com$clean"
            SourceServer.OPHIM -> "https://img.ophim.live/uploads/movies$clean"
            SourceServer.NGUONC -> "https://phim.nguonc.com$clean"
            SourceServer.VSMOV -> "https://vsmov.com$clean"
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
