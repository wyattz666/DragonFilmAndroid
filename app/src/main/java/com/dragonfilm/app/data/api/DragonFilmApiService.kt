package com.dragonfilm.app.data.api

import com.dragonfilm.app.data.model.AuthResponse
import com.dragonfilm.app.data.model.Comment
import com.dragonfilm.app.data.model.HomeResponse
import com.dragonfilm.app.data.model.NetflixResponse
import com.dragonfilm.app.data.model.TMDBWeeklyResponse
import com.google.gson.annotations.SerializedName
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

data class CommentListResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("comments") val comments: List<Comment> = emptyList()
)

data class CommentCreateResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("comment") val comment: Comment? = null
)

data class OKResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("error") val error: String? = null
)

interface DragonFilmApiService {

    @GET("/api/home")
    suspend fun getHome(): HomeResponse

    @GET("/api/source")
    suspend fun getSourceRaw(
        @Query("server") server: String,
        @Query("path") path: String
    ): ResponseBody

    @GET("/api/netflix-top10-vn")
    suspend fun getNetflixTop10(): NetflixResponse

    @GET("/api/tmdb-weekly")
    suspend fun getTMDBWeekly(
        @Query("country") country: String
    ): TMDBWeeklyResponse

    @GET("/api/comments")
    suspend fun getComments(
        @Query("movieKey") movieKey: String
    ): CommentListResponse

    @POST("/api/comments")
    suspend fun postComment(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): CommentCreateResponse

    @HTTP(method = "DELETE", path = "/api/comments", hasBody = true)
    suspend fun deleteComment(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): OKResponse

    @POST("/api/auth/login")
    suspend fun login(
        @Body body: Map<String, String>
    ): AuthResponse

    @POST("/api/auth/register")
    suspend fun register(
        @Body body: Map<String, String>
    ): AuthResponse

    @POST("/api/auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): AuthResponse

    @GET("/api/user/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): AuthResponse

    @GET("/api/user-data")
    suspend fun getUserData(
        @Header("Authorization") token: String
    ): ResponseBody

    @POST("/api/user-data")
    suspend fun postUserData(
        @Header("Authorization") token: String,
        @Body body: Map<String, Any>
    ): ResponseBody

    @POST("https://graphql.anilist.co")
    suspend fun getAniList(
        @Body body: RequestBody
    ): ResponseBody
}
