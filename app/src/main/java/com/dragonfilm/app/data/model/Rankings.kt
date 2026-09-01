package com.dragonfilm.app.data.model

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NetflixItem(
    @SerializedName("rank")
    val rank: Int = 0,

    @SerializedName("title")
    val title: String = "",

    @SerializedName("type")
    val type: String = "",

    @SerializedName("poster")
    val poster: String? = null,

    @SerializedName("poster_url")
    val posterUrl: String? = null,

    @SerializedName("logo_url")
    val logoUrl: String? = null,

    @SerializedName("tmdb")
    val tmdb: NetflixTMDB? = null
)

data class NetflixTMDB(
    @SerializedName("id")
    val id: Any? = null,

    @SerializedName("vote_average")
    val voteAverage: Double? = null,

    @SerializedName("poster_url")
    val posterUrl: String? = null,

    @SerializedName("backdrop_url")
    val backdropUrl: String? = null,

    @SerializedName("thumb_url")
    val thumbUrl: String? = null
)

data class NetflixResponse(
    @SerializedName("source")
    val source: String? = null,

    @SerializedName("items")
    val items: List<NetflixItem> = emptyList(),

    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class TMDBWeeklyItem(
    @SerializedName("rank")
    val rank: Int = 0,

    @SerializedName("tmdb_id")
    val tmdbId: Any? = null,

    @SerializedName("title")
    val title: String = "",

    @SerializedName("original_title")
    val originalTitle: String? = null,

    @SerializedName("overview")
    val overview: String? = null,

    @SerializedName("poster_url")
    val posterUrl: String? = null,

    @SerializedName("backdrop_url")
    val backdropUrl: String? = null,

    @SerializedName("vote_average")
    val voteAverage: Double? = null,

    @SerializedName("popularity")
    val popularity: Double? = null,

    @SerializedName("slug")
    val slug: String? = null
)

data class TMDBWeeklyResponse(
    @SerializedName("ok")
    val ok: Boolean = false,

    @SerializedName("country")
    val country: String = "",

    @SerializedName("label")
    val label: String? = null,

    @SerializedName("items")
    val items: List<TMDBWeeklyItem> = emptyList()
)

data class AniListNormalized(
    val id: Int = 0,
    val titleEN: String = "",
    val titleRomaji: String = "",
    val titleNative: String = "",
    val coverUrl: String = "",
    val score: Int = 0,
    val popularity: Int = 0,
    val year: Int = 0,
    val format: String = "",
    val genres: List<String> = emptyList()
) {
    val title: String
        get() = if (titleEN.isNotEmpty()) titleEN else titleRomaji

    val altTitle: String
        get() = if (titleEN.isNotEmpty() && titleRomaji.isNotEmpty()) titleRomaji else titleNative
}

data class ScheduleDayItem(
    val date: Date
) {
    val id: Long = date.time

    val isToday: Boolean
        get() {
            val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            return fmt.format(date) == fmt.format(Date())
        }

    val weekdayShort: String
        get() {
            val fmt = SimpleDateFormat("EEE", Locale("vi", "VN"))
            return fmt.format(date)
        }

    val dayNumber: String
        get() {
            val fmt = SimpleDateFormat("d", Locale.getDefault())
            return fmt.format(date)
        }
}
