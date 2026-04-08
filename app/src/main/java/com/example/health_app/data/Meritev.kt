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
    val temperatura: Double,
    val userId: String = "",
    val firestoreId: String = ""
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "ime" to ime,
        "priimek" to priimek,
        "datum" to datum,
        "srcniUtrip" to srcniUtrip,
        "spO2" to spO2,
        "temperatura" to temperatura,
        "userId" to userId
    )

    companion object {
        fun fromFirestore(documentId: String, data: Map<String, Any?>): Meritev? {
            return try {
                Meritev(
                    ime = data["ime"] as? String ?: return null,
                    priimek = data["priimek"] as? String ?: return null,
                    datum = data["datum"] as? Long ?: return null,
                    srcniUtrip = (data["srcniUtrip"] as? Number)?.toInt() ?: return null,
                    spO2 = (data["spO2"] as? Number)?.toInt() ?: return null,
                    temperatura = (data["temperatura"] as? Number)?.toDouble() ?: return null,
                    userId = data["userId"] as? String ?: "",
                    firestoreId = documentId
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

