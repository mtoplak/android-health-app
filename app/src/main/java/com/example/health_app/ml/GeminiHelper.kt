package com.example.health_app.ml

import com.example.health_app.BuildConfig
import com.example.health_app.data.Meritev
import com.google.ai.client.generativeai.GenerativeModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GeminiHelper {

    private val apiKey: String = BuildConfig.GEMINI_API_KEY
    private val modelNames = listOf("gemini-flash-latest", "gemini-2.0-flash")

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

            generateWithSdk(prompt).orEmpty().ifBlank {
                generateLocalSummary(meritve, includeFallbackNote = true)
            }
        }.getOrElse {
            generateLocalSummary(meritve, includeFallbackNote = true)
        }
    }

    private suspend fun generateWithSdk(prompt: String): String? {
        for (modelName in modelNames) {
            val text = runCatching {
                val model = GenerativeModel(modelName = modelName, apiKey = apiKey)
                model.generateContent(prompt).text?.trim()
            }.getOrNull()

            if (!text.isNullOrBlank()) {
                return text
            }
        }
        return null
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



