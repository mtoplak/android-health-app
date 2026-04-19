package com.example.health_app.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp

enum class HealthStatus {
    NORMAL,
    ELEVATED,
    CRITICAL
}

data class HealthPrediction(
    val status: HealthStatus,
    val confidence: Map<HealthStatus, Float>,
    val fromTflite: Boolean
)

class HealthClassifier(private val context: Context) {

    private val interpreter: Interpreter? by lazy {
        runCatching {
            Interpreter(loadModelFile("health_classifier.tflite"))
        }.onFailure {
            Log.w("HealthClassifier", "TFLite model not available, using rule-based fallback", it)
        }.getOrNull()
    }

    fun classify(hr: Int, spo2: Int, temp: Double): HealthPrediction {
        val tflitePrediction = runTflite(hr, spo2, temp)
        return tflitePrediction ?: classifyRuleBased(hr, spo2, temp)
    }

    private fun runTflite(hr: Int, spo2: Int, temp: Double): HealthPrediction? {
        val model = interpreter ?: return null
        return runCatching {
            val input = arrayOf(floatArrayOf(hr.toFloat(), spo2.toFloat(), temp.toFloat()))
            val output = Array(1) { FloatArray(3) }
            model.run(input, output)

            val probs = softmax(output[0])
            val confidenceMap = mapOf(
                HealthStatus.NORMAL to probs[0],
                HealthStatus.ELEVATED to probs[1],
                HealthStatus.CRITICAL to probs[2]
            )

            val status = confidenceMap.maxByOrNull { it.value }?.key ?: HealthStatus.ELEVATED
            HealthPrediction(status = status, confidence = confidenceMap, fromTflite = true)
        }.onFailure {
            Log.e("HealthClassifier", "TFLite inference failed, using rule fallback", it)
        }.getOrNull()
    }

    private fun classifyRuleBased(hr: Int, spo2: Int, temp: Double): HealthPrediction {
        val status = when {
            hr > 120 || spo2 < 90 || temp > 39.0 -> HealthStatus.CRITICAL
            hr > 100 || spo2 < 95 || temp > 37.5 -> HealthStatus.ELEVATED
            else -> HealthStatus.NORMAL
        }

        val confidenceMap = when (status) {
            HealthStatus.NORMAL -> mapOf(
                HealthStatus.NORMAL to 0.90f,
                HealthStatus.ELEVATED to 0.08f,
                HealthStatus.CRITICAL to 0.02f
            )

            HealthStatus.ELEVATED -> mapOf(
                HealthStatus.NORMAL to 0.15f,
                HealthStatus.ELEVATED to 0.75f,
                HealthStatus.CRITICAL to 0.10f
            )

            HealthStatus.CRITICAL -> mapOf(
                HealthStatus.NORMAL to 0.05f,
                HealthStatus.ELEVATED to 0.15f,
                HealthStatus.CRITICAL to 0.80f
            )
        }

        return HealthPrediction(status = status, confidence = confidenceMap, fromTflite = false)
    }

    private fun loadModelFile(fileName: String): MappedByteBuffer {
        context.assets.openFd(fileName).use { fileDescriptor ->
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                val fileChannel = inputStream.channel
                return fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fileDescriptor.startOffset,
                    fileDescriptor.declaredLength
                )
            }
        }
    }

    private fun softmax(values: FloatArray): FloatArray {
        val max = values.maxOrNull() ?: 0f
        val exps = values.map { exp((it - max).toDouble()).toFloat() }
        val sum = exps.sum().takeIf { it > 0f } ?: 1f
        return exps.map { it / sum }.toFloatArray()
    }
}

