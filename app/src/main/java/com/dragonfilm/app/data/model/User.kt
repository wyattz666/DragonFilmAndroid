package com.dragonfilm.app.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("username")
    val username: String = "",

    @SerializedName("email")
    val email: String = "",

    @SerializedName("phone")
    val phone: String = "",

    @SerializedName("avatar_url", alternate = ["avatarUrl", "avatar", "picture", "photo_url", "photoURL", "image_url"])
    val avatarUrl: String = "",

    @SerializedName("role")
    val role: String = "user",

    @SerializedName("is_admin", alternate = ["isAdmin", "admin"])
    val isAdmin: Boolean = false,

    @SerializedName("created_at", alternate = ["createdAt"])
    val createdAt: String = ""
)

data class AuthResponse(
    @SerializedName("ok")
    val ok: Boolean = false,

    @SerializedName("token")
    val token: String? = null,

    @SerializedName("user")
    val user: User? = null,

    @SerializedName("error")
    val error: String? = null,

    @SerializedName("code")
    val code: String? = null
)

data class Comment(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("movie_key")
    val movieKey: String = "",

    @SerializedName("body")
    val body: String = "",

    @SerializedName("created_at")
    val createdAt: String = "",

    @SerializedName("user")
    val user: CommentUser = CommentUser()
)

data class CommentUser(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("username")
    val username: String = "",

    @SerializedName("avatar_url", alternate = ["avatarUrl", "avatar", "picture", "photo_url", "photoURL", "image_url"])
    val avatarUrl: String? = null,

    @SerializedName("role")
    val role: String = "user",

    @SerializedName("is_admin", alternate = ["isAdmin", "admin"])
    val isAdmin: Boolean = false
)

data class HistoryItem(
    @SerializedName("slug", alternate = ["movie_slug"])
    val slug: String = "",

    @SerializedName("name", alternate = ["movie_name"])
    val name: String = "",

    @SerializedName("poster_url", alternate = ["posterURL", "thumb_url", "thumbURL"])
    val posterUrl: String = "",

    @SerializedName("year", alternate = ["movie_year"])
    val year: String = "",

    @SerializedName("_server", alternate = ["server", "movie_server"])
    val server: String = "kkphim",

    @SerializedName("source_name", alternate = ["sourceName"])
    val sourceName: String = "",

    @SerializedName("episode_name", alternate = ["episodeName"])
    val episodeName: String = "",

    @SerializedName("episode_slug", alternate = ["episodeSlug"])
    val episodeSlug: String = "",

    @SerializedName("episode_server_name", alternate = ["episodeServerName"])
    val episodeServerName: String = "",

    @SerializedName("episode_server_idx", alternate = ["episodeServerIdx"])
    val episodeServerIdx: Int = 0,

    @SerializedName("episode_index0", alternate = ["episodeIndex0"])
    val episodeIndex0: Int = 0,

    @SerializedName("episode_number", alternate = ["episodeNumber"])
    val episodeNumber: Int = 1,

    @SerializedName("watched_seconds", alternate = ["watchedSeconds", "seconds"])
    val watchedSeconds: Double = 0.0,

    @SerializedName("duration_seconds", alternate = ["durationSeconds"])
    val durationSeconds: Double = 0.0,

    @SerializedName("progress_percent", alternate = ["progressPercent"])
    val progressPercent: Double = 0.0,

    @SerializedName("resume_key", alternate = ["resumeKey"])
    val resumeKey: String = "",

    @SerializedName("watchedAt", alternate = ["watched_at"])
    val watchedAt: Double = System.currentTimeMillis().toDouble()
) {
    val id: String
        get() = "$slug-$episodeSlug"

    val normalizedWatchedAt: Long
        get() = if (watchedAt < 10_000_000_000.0) (watchedAt * 1000).toLong() else watchedAt.toLong()
}

data class SavedActor(
    @SerializedName("name", alternate = ["actor_name"])
    val name: String = "",

    @SerializedName("character", alternate = ["character_name"])
    val character: String = "",

    @SerializedName("profile_url", alternate = ["profileURL", "avatar", "image_url"])
    val profileUrl: String = "",

    @SerializedName("addedAt", alternate = ["added_at"])
    val addedAt: Double = System.currentTimeMillis().toDouble()
) {
    val id: String
        get() = name

    val normalizedAddedAt: Long
        get() = if (addedAt < 10_000_000_000.0) (addedAt * 1000).toLong() else addedAt.toLong()
}
