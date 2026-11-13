package com.example

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newAnimeLoadResponse
import com.lagradost.cloudstream3.newAnimeSearchResponse

// Jikan API se aane wala data ka structure
data class JikanSearch(val data: List<JikanAnime>?)
data class JikanOne(val data: JikanAnime?)
data class JikanAnime(
    val mal_id: Int?,
    val title: String?,
    val synopsis: String?,
    val images: JikanImages?
)
data class JikanImages(val jpg: JikanJpg?)
data class JikanJpg(val image_url: String?)

class ExampleProvider : MainAPI() {
    override var mainUrl = "https://api.jikan.moe/v4"
    override var name = "Jikan Practice"
    override val supportedTypes = setOf(TvType.Anime)
    override var lang = "en"
    override val hasMainPage = false

    // 1) Search: user jo likhega, uske results laata hai
    override suspend fun search(query: String): List<SearchResponse> {
        val res = app.get("$mainUrl/anime?q=$query&limit=10").parsed<JikanSearch>()
        return res.data.orEmpty().map { anime ->
            newAnimeSearchResponse(
                anime.title ?: "Unknown",
                "$mainUrl/anime/${anime.mal_id}",
                TvType.Anime
            ) {
                this.posterUrl = anime.images?.jpg?.image_url
            }
        }
    }

    // 2) Load: ek anime ki details (poster, description)
    override suspend fun load(url: String): LoadResponse? {
        val anime = app.get(url).parsed<JikanOne>().data ?: return null
        return newAnimeLoadResponse(
            anime.title ?: "Unknown",
            url,
            TvType.Anime
        ) {
            this.posterUrl = anime.images?.jpg?.image_url
            this.plot = anime.synopsis
        }
    }
}
