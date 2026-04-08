package com.example.health_app.data

data class Statistics(
    val avgSrcniUtrip: Double = 0.0,
    val minSrcniUtrip: Int = 0,
    val maxSrcniUtrip: Int = 0,

    val avgSpO2: Double = 0.0,
    val minSpO2: Int = 0,
    val maxSpO2: Int = 0,

    val avgTemperatura: Double = 0.0,
    val minTemperatura: Double = 0.0,
    val maxTemperatura: Double = 0.0,

    val totalMeritve: Int = 0
) {
    companion object {
        fun fromMeritve(meritve: List<Meritev>): Statistics {
            if (meritve.isEmpty()) return Statistics()

            return Statistics(
                avgSrcniUtrip = meritve.map { it.srcniUtrip }.average(),
                minSrcniUtrip = meritve.minOf { it.srcniUtrip },
                maxSrcniUtrip = meritve.maxOf { it.srcniUtrip },

                avgSpO2 = meritve.map { it.spO2 }.average(),
                minSpO2 = meritve.minOf { it.spO2 },
                maxSpO2 = meritve.maxOf { it.spO2 },

                avgTemperatura = meritve.map { it.temperatura }.average(),
                minTemperatura = meritve.minOf { it.temperatura },
                maxTemperatura = meritve.maxOf { it.temperatura },

                totalMeritve = meritve.size
            )
        }
    }
}
