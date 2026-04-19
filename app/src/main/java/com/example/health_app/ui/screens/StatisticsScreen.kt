package com.example.health_app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.health_app.R
import com.example.health_app.viewmodel.MeritevViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: MeritevViewModel,
    onNavigateBack: () -> Unit
) {
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val cloudMeritve by viewModel.cloudMeritve.collectAsStateWithLifecycle()
    val aiSummaryState by viewModel.aiSummaryState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistika)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.preklici)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Srčni utrip stats
            StatisticsCard(
                title = stringResource(R.string.srcni_utrip),
                avgValue = statistics.avgSrcniUtrip,
                minValue = statistics.minSrcniUtrip.toString(),
                maxValue = statistics.maxSrcniUtrip.toString(),
                unit = "bpm"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SpO2 stats
            StatisticsCard(
                title = stringResource(R.string.spo2),
                avgValue = statistics.avgSpO2,
                minValue = statistics.minSpO2.toString(),
                maxValue = statistics.maxSpO2.toString(),
                unit = "%"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Temperatura stats
            StatisticsCard(
                title = stringResource(R.string.temperatura),
                avgValue = statistics.avgTemperatura,
                minValue = String.format(Locale.getDefault(), "%.1f", statistics.minTemperatura),
                maxValue = String.format(Locale.getDefault(), "%.1f", statistics.maxTemperatura),
                unit = "°C"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Total measurements
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.skupno_meritev),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = statistics.totalMeritve.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Graphical trend
            if (cloudMeritve.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.trend_meritev),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.srcni_utrip),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        val heartRates = cloudMeritve.asReversed().map { it.srcniUtrip.toFloat() }

                        SimpleLineChart(
                            values = heartRates,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${stringResource(R.string.minimum)}: ${heartRates.minOrNull()?.toInt() ?: 0} bpm",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${stringResource(R.string.maximum)}: ${heartRates.maxOrNull()?.toInt() ?: 0} bpm",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Text(
                            text = stringResource(R.string.skupno_meritev) + ": ${cloudMeritve.size}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${stringResource(R.string.temperatura)}: ${String.format(Locale.getDefault(), "%.1f", cloudMeritve.last().temperatura)} °C",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ai_asistent),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { viewModel.generateAiSummary() },
                        enabled = !aiSummaryState.isLoading
                    ) {
                        if (aiSummaryState.isLoading) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.generiraj_ai_povzetek))
                        }
                    }

                    if (!aiSummaryState.isLoading && aiSummaryState.summary == null && aiSummaryState.error == null) {
                        Text(
                            text = stringResource(R.string.ai_povzetek_namig),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    aiSummaryState.summary?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    aiSummaryState.error?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsCard(
    title: String,
    avgValue: Double,
    minValue: String,
    maxValue: String,
    unit: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatValue(
                    label = stringResource(R.string.povprecje),
                    value = "${String.format(Locale.getDefault(), "%.1f", avgValue)} $unit"
                )
                StatValue(
                    label = stringResource(R.string.minimum),
                    value = "$minValue $unit"
                )
                StatValue(
                    label = stringResource(R.string.maximum),
                    value = "$maxValue $unit"
                )
            }
        }
    }
}

@Composable
private fun StatValue(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SimpleLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas

        val minValue = values.minOrNull() ?: 0f
        val maxValue = values.maxOrNull() ?: 0f
        val range = if (maxValue - minValue == 0f) 1f else maxValue - minValue
        val stepX = if (values.size == 1) 0f else size.width / (values.size - 1)

        val points = values.mapIndexed { index, value ->
            val x = if (values.size == 1) size.width / 2f else index * stepX
            val normalized = (value - minValue) / range
            val y = size.height - (normalized * size.height)
            Offset(x, y)
        }

        // background grid
        val gridColor = Color.Gray.copy(alpha = 0.18f)
        repeat(3) { index ->
            val y = size.height * (index + 1) / 4f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        for (i in 0 until points.lastIndex) {
            drawLine(
                color = Color(0xFF1976D2),
                start = points[i],
                end = points[i + 1],
                strokeWidth = 6f
            )
        }

        points.forEach { point ->
            drawCircle(
                color = Color(0xFF1565C0),
                radius = 7f,
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 2.5f,
                center = point
            )
        }

        // frame
        drawRect(
            color = Color.Gray.copy(alpha = 0.25f),
            style = Stroke(width = 2f)
        )
    }
}

