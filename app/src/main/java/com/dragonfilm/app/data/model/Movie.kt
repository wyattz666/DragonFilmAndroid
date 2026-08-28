package com.dragonfilm.app.data.model

import com.google.gson.annotations.SerializedName

data class Movie(
    @SerializedName("slug")
    val slug: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("origin_name")
    val originName: String = "",

    @SerializedName("thumb_url")
    val thumbUrl: String = "",

    @SerializedName("poster_url")
    val posterUrl: String = "",

    @SerializedName("year")
    val yearRaw: Any? = null,

    @SerializedName("type")
    val type: String = "single",

    @SerializedName("episode_current")
    val episodeCurrent: String = "",

    @SerializedName("quality")
    val quality: String? = null,

    @SerializedName("lang")
    val lang: String? = null,

    @SerializedName("category")
    val category: List<Genre> = emptyList(),

    @SerializedName("country")
    val country: List<Genre> = emptyList(),

    @SerializedName("actor")
    val actor: List<PersonRef>? = null,

    @SerializedName("director")
    val director: List<PersonRef>? = null,

    @SerializedName("tmdb")
    val tmdb: TMDBInfo? = null,

    @SerializedName("imdb")
    val imdb: TMDBInfo? = null,

    @SerializedName("_server")
    val server: String? = null,

    @SerializedName("_sources")
    val sources: List<String>? = null,

    @SerializedName("_serverSlugs")
    val serverSlugs: Map<String, String>? = null,

    @SerializedName("_source_thumb_url")
    val sourceThumbUrl: String? = null,

    @SerializedName("_source_poster_url")
    val sourcePosterUrl: String? = null
) {
    val yearString: String
        get() = when (yearRaw) {
            is Number -> yearRaw.toInt().toString()
            is String -> yearRaw
            else -> ""
        }

    val bestBanner: String
        get() {
            if (!sourceThumbUrl.isNullOrEmpty()) return sourceThumbUrl
            if (thumbUrl.isNotEmpty()) return thumbUrl
            if (!tmdb?.backdropUrl.isNullOrEmpty()) return tmdb?.backdropUrl ?: ""
            return bestPoster
        }

    val bestPoster: String
        get() {
            if (!sourcePosterUrl.isNullOrEmpty()) return sourcePosterUrl
            if (posterUrl.isNotEmpty()) return posterUrl
            if (!tmdb?.posterUrl.isNullOrEmpty()) return tmdb?.posterUrl ?: ""
            return thumbUrl
        }

    val bestThumb: String
        get() = bestBanner

    val isSeries: Boolean
        get() = type == "series" || type == "hoathinh" || type == "tvshows"

    val commentKey: String
        get() {
            val source = if (originName.isNotEmpty()) originName else name
            val normalised = source.lowercase()
                .replace(Regex("[^a-zA-Z0-9]+"), "-")
                .trim('-')
            return "$normalised:$yearString"
        }
}

data class Genre(
    @SerializedName("name")
    val name: String = "",

    @SerializedName("slug")
    val slug: String = ""
)

data class TMDBInfo(
    @SerializedName("id")
    val id: Any? = null,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("season")
    val season: Any? = null,

    @SerializedName("vote_average", alternate = ["voteAverage", "rate"])
    val voteAverage: Any? = null,

    @SerializedName("vote_count", alternate = ["voteCount", "votes"])
    val voteCount: Any? = null,

    @SerializedName("poster_url", alternate = ["poster_path"])
    val posterUrl: String? = null,

    @SerializedName("backdrop_url", alternate = ["backdrop_path"])
    val backdropUrl: String? = null,

    @SerializedName("thumb_url")
    val thumbUrl: String? = null
) {
    val scoreString: String
        get() {
            val d = when (voteAverage) {
                is Number -> voteAverage.toDouble()
                is String -> voteAverage.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            return if (d > 0.0) String.format("%.1f", d) else "N/A"
        }
}

data class Episode(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("slug")
    val slug: String = "",

    @SerializedName("filename")
    val filename: String? = null,

    @SerializedName("link_m3u8")
    val linkM3U8: String? = null,

    @SerializedName("link_embed")
    val linkEmbed: String? = null
)

data class EpisodeServer(
    @SerializedName("server_name")
    val serverName: String = "",

    @SerializedName("items")
    val items: List<Episode> = emptyList()
)

data class PersonRef(
    val name: String = "",
    val character: String = "",
    val profileUrl: String = ""
)

data class HomeRow(
    @SerializedName("key")
    val key: String = "",

    @SerializedName("title")
    val title: String = "",

    @SerializedName("items")
    val items: List<Movie> = emptyList()
)

data class HomeResponse(
    @SerializedName("rows")
    val rows: List<HomeRow> = emptyList()
)

enum class CatalogFilterKind {
    TYPE, GENRE, COUNTRY, LATEST
}

data class CatalogFilter(
    val kind: CatalogFilterKind = CatalogFilterKind.LATEST,
    val slug: String = "",
    val label: String = ""
) {
    val isEmpty: Boolean
        get() = slug.isEmpty()

    val operation: String
        get() = when (kind) {
            CatalogFilterKind.GENRE -> "genre"
            CatalogFilterKind.COUNTRY -> "country"
            CatalogFilterKind.TYPE -> "type"
            CatalogFilterKind.LATEST -> "latest"
        }

    val displayTitle: String
        get() = if (label.isNotEmpty()) label else "Phim Mới Cập Nhật"
}

object CatalogOption {
    val types = listOf(
        Genre("Tất cả loại phim", ""),
        Genre("Phim Bộ", "phim-bo"),
        Genre("Phim Lẻ", "phim-le"),
        Genre("Hoạt Hình", "hoat-hinh"),
        Genre("TV Shows", "tv-shows")
    )

    val genres = listOf(
        Genre("Tất cả thể loại", ""),
        Genre("Hành Động", "hanh-dong"),
        Genre("Cổ Trang", "co-trang"),
        Genre("Chiến Tranh", "chien-tranh"),
        Genre("Viễn Tưởng", "vien-tuong"),
        Genre("Kinh Dị", "kinh-di"),
        Genre("Tài Liệu", "tai-lieu"),
        Genre("Bí Ẩn", "bi-an"),
        Genre("Tình Cảm", "tinh-cam"),
        Genre("Tâm Lý", "tam-ly"),
        Genre("Hài Hước", "hai-huoc"),
        Genre("Phiêu Lưu", "phieu-luu"),
        Genre("Âm Nhạc", "am-nhac"),
        Genre("Gia Đình", "gia-dinh"),
        Genre("Học Đường", "hoc-duong"),
        Genre("Hình Sự", "hinh-su"),
        Genre("Võ Thuật", "vo-thuat"),
        Genre("Khoa Học", "khoa-hoc"),
        Genre("Thần Thoại", "than-thoai"),
        Genre("Chính Kịch", "chinh-kich")
    )

    val countries = listOf(
        Genre("Tất cả quốc gia", ""),
        Genre("Việt Nam", "viet-nam"),
        Genre("Trung Quốc", "trung-quoc"),
        Genre("Hàn Quốc", "han-quoc"),
        Genre("Nhật Bản", "nhat-ban"),
        Genre("Âu Mỹ", "au-my"),
        Genre("Thái Lan", "thai-lan"),
        Genre("Ấn Độ", "an-do"),
        Genre("Đài Loan", "dai-loan"),
        Genre("Hồng Kông", "hong-kong")
    )
}
