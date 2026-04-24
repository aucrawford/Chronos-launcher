package com.soc.launcher.util

object AppCategoryHelper {
    fun determineCategory(pkg: String): String {
        val pLower = pkg.lowercase()
        return when {
            pLower.contains("browser") || pLower.contains("chrome") || pLower.contains("firefox") || pLower.contains("opera") || pLower.contains("vivaldi") -> "Tools"
            pLower.contains("video") || pLower.contains("music") || pLower.contains("player") || pLower.contains("youtube") || pLower.contains("netflix") || pLower.contains("vlc") -> "Media"
            pLower.contains("social") || pLower.contains("messaging") || pLower.contains("whatsapp") || pLower.contains("insta") || pLower.contains("facebook") || pLower.contains("twitter") || pLower.contains("telegr") || pLower.contains("snapchat") || pLower.contains("tiktok") || pLower.contains("discord") -> "Social"
            pLower.contains("mail") || pLower.contains("gm") || pLower.contains("outlook") -> "Social"
            pLower.contains("office") || pLower.contains("calendar") || pLower.contains("note") || pLower.contains("drive") || pLower.contains("sheet") || pLower.contains("word") || pLower.contains("excel") || pLower.contains("pdf") || pLower.contains("scan") || pLower.contains("docs") -> "Work"
            pLower.contains("game") || pLower.contains("steam") || pLower.contains("play.games") || pLower.contains("arcade") -> "Games"
            pLower.contains("news") || pLower.contains("magazine") || pLower.contains("newspaper") -> "News"
            pLower.contains("clock") || pLower.contains("calc") || pLower.contains("setting") || pLower.contains("camera") || pLower.contains("gallery") || pLower.contains("file") || pLower.contains("assistant") -> "Tools"
            else -> "System"
        }
    }
}
