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
                // Try to find image in enclosure or media:content tags first (better quality)
                val enclosures = item.getElementsByTagName("enclosure")
                if (enclosures.length > 0) {
                    val enclosure = enclosures.item(0) as Element
                    imageUrl = enclosure.getAttribute("url")
                }
                
                if (imageUrl == null) {
                    val mediaContents = item.getElementsByTagName("media:content")
                    if (mediaContents.length > 0) {
                        val media = mediaContents.item(0) as Element
                        imageUrl = media.getAttribute("url")
                    }
                }

                if (imageUrl == null) {
                    val description = item.getElementsByTagName("description").item(0)?.textContent
                    if (description != null) {
                        // Look for standard img tags
                        val imgMatch = "<img[^>]+src=\"([^\"]+)\"".toRegex(RegexOption.IGNORE_CASE).find(description)
                        imageUrl = imgMatch?.groupValues?.get(1)
                        
                        // Handle Google News specific encoding if necessary
                        if (imageUrl == null) {
                            val urlMatch = "url=([^&\"\\s]+)".toRegex().find(description)
                            imageUrl = urlMatch?.groupValues?.get(1)
                        }
                    }
                }
                
                if (imageUrl?.startsWith("//") == true) imageUrl = "https:$imageUrl"
            } catch (e: Exception) {
                Log.e("NewsParser", "Error parsing image: ${e.message}")
            }
            
            articles.add(NewsArticle(title, link, source, imageUrl))
        }
        return articles
    }
}
