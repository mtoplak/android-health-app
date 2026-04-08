package com.example.health_app.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

private const val BASE_URL = "https://mocki.io/v1/"

data class HeartRateResponse(val value: Int)
data class SpO2Response(val value: Int)
data class TemperatureResponse(val value: Double)

interface MockHealthApi {
    @GET("heartrate")
    suspend fun getHeartRate(): HeartRateResponse

    @GET("spo2")
    suspend fun getSpO2(): SpO2Response

    @GET("temperature")
    suspend fun getTemperature(): TemperatureResponse
}

object MockHealthApiProvider {
    val api: MockHealthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MockHealthApi::class.java)
    }
}

