package com.soc.launcher.util

import com.soc.launcher.data.model.NewsArticle
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class NewsParserTest {

    @Test
    fun testParseRss() {
        val rssXml = """
            <rss version="2.0">
                <channel>
                    <item>
                        <title>Sample News Title - Source Name</title>
                        <link>https://example.com/news1</link>
                        <description><![CDATA[<img src="https://example.com/image.jpg">Some description.]]></description>
                    </item>
                    <item>
                        <title>Another News Article - Another Source</title>
                        <link>https://example.com/news2</link>
                        <description>No image here.</description>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(rssXml.toByteArray())
        val articles = NewsParser.parseRss(inputStream)

        assertEquals(2, articles.size)
        
        assertEquals("Sample News Title", articles[0].title)
        assertEquals("Source Name", articles[0].source)
        assertEquals("https://example.com/news1", articles[0].url)
        assertEquals("https://example.com/image.jpg", articles[0].urlToImage)

        assertEquals("Another News Article", articles[1].title)
        assertEquals("Another Source", articles[1].source)
        assertEquals("https://example.com/news2", articles[1].url)
        assertEquals(null, articles[1].urlToImage)
    }
}
