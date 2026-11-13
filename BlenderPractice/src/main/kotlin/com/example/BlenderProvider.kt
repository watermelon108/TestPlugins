package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup

class BlenderProvider : MainAPI() {
    override var mainUrl = "https://download.blender.org/demo/movies/"
    override var name = "Blender Open Movies"
    override val supportedTypes = setOf(TvType.Movie)
    override var lang = "en"
    override val hasMainPage = false

    // Folder list: har folder ek movie hai (jaise BBB/)
    private suspend fun listFolders(): List<Pair<String, String>> {
        val doc = Jsoup.parse(app.get(mainUrl).text)
        return doc.select("a[href]")
            .map { it.attr("href") }
            .filter { it.endsWith("/") && !it.startsWith("/") && !it.startsWith("..") && !it.startsWith("?") }
            .map { href -> href.trimEnd('/') to (mainUrl + href) }
    }

    // Folder ke andar se pehli video file ka link (mp4, mkv ya webm)
    private suspend fun findMp4(folderUrl: String): String? {
        val doc = Jsoup.parse(app.get(folderUrl).text)
        val file = doc.select("a[href]")
            .map { it.attr("href") }
            .firstOrNull { it.endsWith(".mp4") || it.endsWith(".mkv") || it.endsWith(".webm") }
            ?: return null
        return if (file.startsWith("http")) file else folderUrl + file
    }

    // 1) Search: movie ka naam dhoondo
    override suspend fun search(query: String): List<SearchResponse> {
        return listFolders()
            .filter { (title, _) -> title.contains(query, ignoreCase = true) }
            .map { (title, url) -> newMovieSearchResponse(title, url, TvType.Movie) }
    }

    // 2) Load: movie ki details, aur video link ko dataUrl me rakho
    override suspend fun load(url: String): LoadResponse? {
        val title = url.trimEnd('/').substringAfterLast('/')
        val mp4 = findMp4(url) ?: return null
        return newMovieLoadResponse(title, url, TvType.Movie, mp4)
    }

    // 3) Play: video link app ko do
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        callback(newExtractorLink(name, name, data, ExtractorLinkType.VIDEO))
        return true
    }
}
