package com.example.health_app.network

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.random.Random

class SensorRepository(
	context: Context,
	private val api: MockHealthApi = MockHealthApiProvider.api
) {
	private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

	fun hasHeartRateSensor(): Boolean {
		return sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null
	}

	suspend fun readHeartRateFromSensor(): Int? {
		val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) ?: return null

		return withTimeoutOrNull(4000L) {
			suspendCancellableCoroutine { continuation ->
				val listener = object : SensorEventListener {
					override fun onSensorChanged(event: SensorEvent?) {
						val value = event?.values?.firstOrNull()?.toInt()
						if (value != null && value > 0 && continuation.isActive) {
							sensorManager.unregisterListener(this)
							continuation.resume(value)
						}
					}

					override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
				}

				sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
				continuation.invokeOnCancellation {
					sensorManager.unregisterListener(listener)
				}
			}
		}
	}

	suspend fun readHeartRateFromApi(): Int = withContext(Dispatchers.IO) {
		runCatching { api.getHeartRate().value }.getOrElse { Random.nextInt(60, 101) }
	}

	suspend fun readSpO2FromApi(): Int = withContext(Dispatchers.IO) {
		runCatching { api.getSpO2().value }.getOrElse { Random.nextInt(94, 100) }
	}

	suspend fun readTemperatureFromApi(): Double = withContext(Dispatchers.IO) {
		runCatching { api.getTemperature().value }.getOrElse { Random.nextDouble(36.0, 37.8) }
	}
}

