package com.soc.launcher.util

import android.content.pm.ApplicationInfo
import android.os.Build

object AppCategoryHelper {
    fun determineCategory(pkg: String, name: String): String {
        val pLower = pkg.lowercase()
        val nLower = name.lowercase()

        // 1. AI detection (High priority)
        if (isAiApp(pLower, nLower)) {
            return "AI"
        }

        // 2. Keyword-based matching
        return when {
            pLower.contains("browser") || pLower.contains("chrome") || pLower.contains("firefox") || pLower.contains("opera") || pLower.contains("vivaldi") -> "Tools"
            pLower.contains("video") || pLower.contains("music") || pLower.contains("player") || pLower.contains("youtube") || pLower.contains("netflix") || pLower.contains("vlc") -> "Media"
            pLower.contains("social") || pLower.contains("messaging") || pLower.contains("whatsapp") || pLower.contains("insta") || pLower.contains("facebook") || pLower.contains("twitter") || pLower.contains("telegr") || pLower.contains("snapchat") || pLower.contains("tiktok") || pLower.contains("discord") -> "Social"
            pLower.contains("mail") || pLower.contains("gm") || pLower.contains("outlook") -> "Social"
            pLower.contains("office") || pLower.contains("calendar") || pLower.contains("note") || pLower.contains("drive") || pLower.contains("sheet") || pLower.contains("word") || pLower.contains("excel") || pLower.contains("pdf") || pLower.contains("scan") || pLower.contains("docs") -> "Work"
            pLower.contains("game") || pLower.contains("steam") || pLower.contains("play.games") || pLower.contains("arcade") -> "Games"
            pLower.contains("news") || pLower.contains("magazine") || pLower.contains("newspaper") -> "News"
            pLower.contains("clock") || pLower.contains("calc") || pLower.contains("setting") || pLower.contains("camera") || pLower.contains("gallery") || pLower.contains("file") -> "Tools"
            else -> "System"
        }
    }

    private fun isAiApp(pkg: String, name: String): Boolean {
        val aiKeywords = listOf(
            "perplexity", "character", "lumo", "rosy", "gemini", "chatgpt", 
            "claude", "copilot", "assistant", "poe", "inflection", "talk", "pplx", "cai",
            "chatbot", "gpt", "bard", "bing", "intelligence", "neural"
        )
        
        return aiKeywords.any { pkg.contains(it) || name.contains(it) } ||
                name.split(Regex("[^a-z]")).contains("ai") ||
                pkg.split(".").contains("ai")
    }
}
