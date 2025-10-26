package it.teateonair.app

import org.jsoup.Jsoup
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object CacheManager {
    private val cache = ConcurrentHashMap<String, String>()
    private val executor = java.util.concurrent.Executors.newFixedThreadPool(2)

    fun prefetchAll() {
        prefetchSchedule()
        prefetchPrograms()
    }

    private fun prefetchSchedule() {
        executor.execute {
            try {
                val connection = java.net.URL("https://radioteateonair.it/palinsesto").openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val html = connection.getInputStream().bufferedReader().use { it.readText() }
                cache["schedule_raw"] = html
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun prefetchPrograms() {
        executor.execute {
            try {
                val connection = java.net.URL("https://www.radioteateonair.it/programmi/").openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val html = connection.getInputStream().bufferedReader().use { it.readText() }
                cache["programs_raw"] = html
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getScheduleHtml(): String? {
        val rawHtml = cache["schedule_raw"] ?: return null

        return try {
            val doc = Jsoup.parse(rawHtml, "https://radioteateonair.it/palinsesto")

            val dayNames = listOf("Domenica", "Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato")
            val todayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
            val todayName = dayNames[todayIndex]

            val filteredDivs = doc.select("div.qt-part-show-schedule-day-item").toList().filter { element ->
                element.select("span.qt-day").any { daySpan ->
                    daySpan.text().equals(todayName, ignoreCase = true)
                }
            }

            filteredDivs.forEach { div ->
                div.select(".qt-header-bg").forEach { bgDiv ->
                    val bgUrl = bgDiv.attr("data-bgimage")
                    if (bgUrl.isNotEmpty()) {
                        bgDiv.attr(
                            "style",
                            "background-image:url('$bgUrl'); background-size:cover; background-position:center; background-repeat:no-repeat;"
                        )
                    }
                }
            }

            val finalContent = if (filteredDivs.isNotEmpty()) {
                filteredDivs.joinToString("\n") { it.outerHtml() }
            } else {
                "<div style='display:flex;align-items:center;justify-content:center;min-height:300px;text-align:center;padding:32px;'><div><div style='font-size:48px;margin-bottom:16px;'>📻</div><h2 style='color:#00FF88;margin:0 0 8px 0;font-size:24px;'>E' tutto per oggi</h2><p style='color:#666;margin:0;'>Torna domani per il nuovo palinsesto</p></div></div>"
            }

            val cssLinks = doc.select("link[rel=stylesheet]").joinToString("\n") {
                """<link rel="stylesheet" href="${it.absUrl("href")}">"""
            }

            """
                <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        $cssLinks
                        <style>
                            body { 
                                margin: 0; 
                                padding: 16px; 
                                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; 
                                background: #fff; 
                                color: #000; 
                                line-height: 1.4;
                                font-size: 14px;
                            }
                            img, iframe { 
                                max-width: 100% !important; 
                                height: auto !important; 
                                display: block; 
                                margin: 8px auto; 
                            }
                            .qt-header-bg {
                                min-height: 120px;
                                background-size: cover !important;
                                background-position: center !important;
                                background-repeat: no-repeat !important;
                                border-radius: 8px;
                                margin-bottom: 16px;
                            }
                            .qt-part-show-schedule-day-item {
                                margin-bottom: 20px;
                                padding: 12px;
                                border-radius: 8px;
                                box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                            }
                            @media (max-width: 600px) {
                                body { padding: 12px; font-size: 13px; }
                                .qt-header-bg { min-height: 100px; }
                            }
                            @media (min-width: 768px) {
                                body { padding: 24px; font-size: 16px; }
                                .qt-header-bg { min-height: 160px; }
                            }
                        </style>
                    </head>
                    <body>
                        $finalContent
                    </body>
                </html>
            """.trimIndent()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getProgramsHtml(): String? {
        val rawHtml = cache["programs_raw"] ?: return null

        return try {
            val doc = Jsoup.parse(rawHtml, "https://www.radioteateonair.it/programmi/")

            doc.select("*").forEach { element ->
                if (element.ownText().equals("PROGRAMMI", ignoreCase = true)) {
                    element.remove()
                }
            }

            doc.select("*").forEach { element ->
                val text = element.ownText().lowercase()
                if (text.contains("podcast attivi") || text.contains("podcast archiviati")) {
                    element.attr("style", "${element.attr("style")}; color: black !important; font-variant: small-caps !important; font-weight: bold !important;")
                }
            }

            val programs = doc.select("article, .program-item, .post, .entry, .content-item, .program, .show").ifEmpty {
                doc.select("div[class*='program'], div[class*='show'], div[class*='post']").ifEmpty {
                    doc.select("div:has(h1), div:has(h2), div:has(h3)")
                }
            }

            val finalContent = if (programs.isNotEmpty()) {
                val processedPrograms = programs.map { program ->
                    program.select("nav, .nav, .navigation, .sidebar, .widget, script, style").remove()

                    program.select("img").forEach { img ->
                        val src = img.attr("src")
                        if (src.isNotEmpty()) {
                            if (src.startsWith("/")) {
                                img.attr("src", "https://www.radioteateonair.it$src")
                            }
                            img.attr("style", "max-width: 100%; height: auto; border-radius: 8px; margin: 8px 0;")
                        }
                    }

                    program.attr("style", "margin-bottom: 24px; padding: 16px; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); background: white;")

                    program.outerHtml()
                }.joinToString("\n")

                processedPrograms
            } else {
                val mainContent = doc.select("main, .main, .content, .site-content, #content, .entry-content").first()

                mainContent?.let { content ->
                    content.select("nav, .nav, .navigation, .sidebar, .widget, script, style, header, footer").remove()

                    content.select("img").forEach { img ->
                        val src = img.attr("src")
                        if (src.startsWith("/")) {
                            img.attr("src", "https://www.radioteateonair.it$src")
                        }
                    }

                    content.html()
                } ?: "<div style='text-align: center; padding: 40px;'><p>Nessun programma trovato</p></div>"
            }

            val cssLinks = doc.select("link[rel=stylesheet]").take(2).joinToString("\n") {
                """<link rel="stylesheet" href="${it.absUrl("href")}">"""
            }

            """
                <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        $cssLinks
                        <style>
                            body { 
                                margin: 0; 
                                padding: 16px; 
                                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; 
                                background: #f8f9fa; 
                                color: #333; 
                                line-height: 1.5;
                                font-size: 14px;
                            }
                            img { 
                                max-width: 100% !important; 
                                height: auto !important; 
                                display: block; 
                                margin: 8px auto; 
                                border-radius: 8px;
                            }
                            h1, h2, h3, h4, h5, h6 {
                                color: #00FF88;
                                margin-top: 20px;
                                margin-bottom: 12px;
                            }
                            p {
                                margin-bottom: 16px;
                                color: #555;
                            }
                            a {
                                color: #00FF88;
                                text-decoration: none;
                            }
                            a:hover {
                                text-decoration: underline;
                            }
                            .program-item, article, .post {
                                background: white;
                                margin-bottom: 20px;
                                padding: 16px;
                                border-radius: 12px;
                                box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                            }
                            *[style*="small-caps"] {
                                color: black !important;
                                font-variant: small-caps !important;
                                font-weight: bold !important;
                            }
                            @media (max-width: 600px) {
                                body { padding: 12px; font-size: 13px; }
                            }
                            @media (min-width: 768px) {
                                body { padding: 24px; font-size: 16px; }
                            }
                        </style>
                    </head>
                    <body>
                        $finalContent
                    </body>
                </html>
            """.trimIndent()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun refresh() {
        cache.clear()
        prefetchAll()
    }
}
