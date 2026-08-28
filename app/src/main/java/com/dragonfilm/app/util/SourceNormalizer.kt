package com.dragonfilm.app.util

import java.net.URLEncoder

enum class SourceServer(val rawValue: String, val displayName: String, val shortName: String) {
    KKPHIM("kkphim", "Server 1", "SV 1"),
    OPHIM("ophim", "Server 2", "SV 2"),
    NGUONC("nguonc", "Server 3", "SV 3"),
    VSMOV("vsmov", "Server 4", "SV 4");

    companion object {
        fun from(value: String?): SourceServer {
            return entries.firstOrNull { it.rawValue.equals(value, ignoreCase = true) } ?: KKPHIM
        }
    }
}

object SourceNormalizer {

    fun upstreamPath(
        server: SourceServer,
        operation: String,
        slug: String? = null,
        keyword: String? = null,
        page: Int = 1
    ): String {
        val kw = try {
            URLEncoder.encode(keyword ?: "", "UTF-8")
        } catch (_: Exception) {
            keyword ?: ""
        }

        return when (server) {
            SourceServer.KKPHIM, SourceServer.OPHIM -> when (operation) {
                "latest" -> "/danh-sach/phim-moi-cap-nhat?page=$page"
                "search" -> "/v1/api/tim-kiem?keyword=$kw&page=$page"
                "detail" -> "/phim/${slug ?: ""}"
                "genre"  -> "/v1/api/the-loai/${slug ?: ""}?page=$page"
                "country"-> "/v1/api/quoc-gia/${slug ?: ""}?page=$page"
                "type"   -> "/v1/api/danh-sach/${slug ?: ""}?page=$page"
                else     -> ""
            }
            SourceServer.NGUONC -> when (operation) {
                "latest" -> "/api/films/phim-moi-cap-nhat?page=$page"
                "search" -> "/api/films/search?keyword=$kw&page=$page"
                "detail" -> "/api/film/${slug ?: ""}"
                "genre"  -> "/api/films/danh-sach/${slug ?: ""}?page=$page"
                "country"-> "/api/films/quoc-gia/${slug ?: ""}?page=$page"
                "type"   -> "/api/films/danh-sach/${slug ?: ""}?page=$page"
                else     -> ""
            }
            SourceServer.VSMOV -> when (operation) {
                "latest" -> "/api/danh-sach/phim-moi-cap-nhat?page=$page"
                "search" -> "/api/tim-kiem?keyword=$kw&page=$page"
                "detail" -> "/api/phim/${slug ?: ""}"
                "genre"  -> "/api/the-loai/${slug ?: ""}?page=$page"
                "country"-> "/api/quoc-gia/${slug ?: ""}?page=$page"
                "type"   -> "/api/the-loai/${slug ?: ""}?page=$page"
                else     -> ""
            }
        }
    }
}
