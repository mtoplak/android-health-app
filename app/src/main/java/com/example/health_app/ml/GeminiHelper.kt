package com.example.health_app.ml

import com.example.health_app.BuildConfig
import com.example.health_app.data.Meritev
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GeminiHelper {

    private val apiKey: String = BuildConfig.GEMINI_API_KEY
    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent"

    suspend fun generateSummary(meritve: List<Meritev>): String {
        if (apiKey.isBlank()) {
            return generateLocalSummary(meritve, includeFallbackNote = true)
        }
        if (meritve.isEmpty()) {
            return "Ni meritev za povzetek."
        }

        return runCatching {
            val input = meritve
                .sortedByDescending { it.datum }
                .take(10)
                .joinToString("\n") { m ->
                    "- datum=${formatDate(m.datum)}, srcniUtrip=${m.srcniUtrip}, spo2=${m.spO2}, temperatura=${"%.1f".format(m.temperatura)}"
                }

            val prompt = """
                Si zdravstveni asistent. Na podlagi spodnjih meritev napiši kratek povzetek v slovenščini (2-4 stavke).
                Ne postavljaj diagnoze. Opiši trend in opozori na možna odstopanja.

                Meritve:
                $input
            """.trimIndent()

            val response = callGeminiRest(prompt)
            extractGeminiText(response)?.trim().orEmpty().ifBlank {
                generateLocalSummary(meritve, includeFallbackNote = true)
            }
        }.getOrElse {
            generateLocalSummary(meritve, includeFallbackNote = true)
        }
    }

    private suspend fun callGeminiRest(prompt: String): String = withContext(Dispatchers.IO) {
        val url = URL(endpoint)
        val body = JSONObject()
            .put(
                "contents",
                org.json.JSONArray().put(
                    JSONObject().put(
                        "parts",
                        org.json.JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
            .toString()

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-goog-api-key", apiKey)
        }

        try {
            connection.outputStream.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
            }

            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            BufferedReader(InputStreamReader(stream)).use { reader ->
                reader.readText()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun extractGeminiText(rawResponse: String): String? {
        if (rawResponse.isBlank()) return null
        return runCatching {
            val root = JSONObject(rawResponse)
            val candidates = root.optJSONArray("candidates") ?: return null
            val firstCandidate = candidates.optJSONObject(0) ?: return null
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            val firstPart = parts.optJSONObject(0) ?: return null
            firstPart.optString("text").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun generateLocalSummary(
        meritve: List<Meritev>,
        includeFallbackNote: Boolean
    ): String {
        if (meritve.isEmpty()) return "Ni meritev za povzetek."

        val sorted = meritve.sortedByDescending { it.datum }.take(10)
        val avgHr = sorted.map { it.srcniUtrip }.average()
        val avgSpo2 = sorted.map { it.spO2 }.average()
        val avgTemp = sorted.map { it.temperatura }.average()
        val latest = sorted.first()

        val healthNote = when {
            avgHr >= 100 || avgSpo2 < 95 || avgTemp >= 37.5 ->
                "Nekatere meritve odstopajo od običajnega razpona in jih je smiselno spremljati."
            else ->
                "Meritve so večinoma v normalnem razponu."
        }

        val fallbackNote = if (includeFallbackNote) {
            " Gemini ni bil na voljo, zato je prikazan lokalni povzetek."
        } else {
            ""
        }

        return buildString {
            append("Na podlagi zadnjih ")
            append(sorted.size)
            append(" meritev je povprečni srčni utrip ")
            append("%.0f".format(avgHr))
            append(" bpm, povprečni SpO₂ ")
            append("%.0f".format(avgSpo2))
            append(" %, temperatura pa ")
            append("%.1f".format(avgTemp))
            append(" °C. ")
            append(healthNote)
            append(" Zadnja meritev je bila zabeležena ")
            append(formatDate(latest.datum))
            append(".")
            append(fallbackNote)
        }
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}



