package com.example.health_app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meritve")
data class Meritev(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val ime: String,
    val priimek: String,
    val datum: Long,
    val srcniUtrip: Int,
    val spO2: Int,
    val temperatura: Double
)

