package com.soc.launcher.util

import com.soc.launcher.data.model.NewsArticle
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import java.io.InputStream
import android.util.Log

object NewsParser {
    fun parseRss(inputStream: InputStream): List<NewsArticle> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(inputStream)
        val items = doc.getElementsByTagName("item")
        val articles = mutableListOf<NewsArticle>()
        for (i in 0 until minOf(items.length, 10)) {
            val item = items.item(i) as Element
            val fullTitle = item.getElementsByTagName("title").item(0).textContent
            val link = item.getElementsByTagName("link").item(0).textContent
            
            val title = fullTitle.substringBeforeLast(" - ")
            val source = fullTitle.substringAfterLast(" - ")
            
            var imageUrl: String? = null
            try {
                val description = item.getElementsByTagName("description").item(0)?.textContent
                if (description != null) {
                    val imgMatch = "src=\"([^\"]+)\"".toRegex().find(description)
                    imageUrl = imgMatch?.groupValues?.get(1)
                    if (imageUrl?.startsWith("//") == true) imageUrl = "https:$imageUrl"
                }
            } catch (e: Exception) {
                // In a real app we might use a logger, but for unit tests we can't use android.util.Log
            }
            
            articles.add(NewsArticle(title, link, source, imageUrl))
        }
        return articles
    }
}
