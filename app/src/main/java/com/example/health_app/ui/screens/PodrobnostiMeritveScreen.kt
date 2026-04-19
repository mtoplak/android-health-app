package com.example.health_app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.health_app.R
import com.example.health_app.ml.HealthStatus
import com.example.health_app.viewmodel.MeritevViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodrobnostiMeritveScreen(
    viewModel: MeritevViewModel,
    meritevId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit
) {
    val meritev by viewModel.getMeritevById(meritevId)
        .collectAsStateWithLifecycle(initialValue = null)
    val healthPrediction by viewModel.healthPrediction.collectAsStateWithLifecycle()
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    LaunchedEffect(meritev?.id) {
        meritev?.let { viewModel.classifyMeasurement(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.podrobnosti_meritve)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.preklici)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(meritevId) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.uredi)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        meritev?.let { m ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                // Name and date card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "${m.ime} ${m.priimek}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dateFormat.format(Date(m.datum)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Health data card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.zdravstveni_podatki),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        DetailRow(
                            label = stringResource(R.string.srcni_utrip_label),
                            value = stringResource(R.string.bpm_value, m.srcniUtrip)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(
                            label = stringResource(R.string.spo2_label),
                            value = stringResource(R.string.spo2_detail_value, m.spO2)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(
                            label = stringResource(R.string.temperatura_label),
                            value = stringResource(
                                R.string.temp_value,
                                String.format(Locale.getDefault(), "%.1f", m.temperatura)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Health status card (TensorFlow Lite + fallback heuristika)
                val prediction = healthPrediction
                val status = prediction?.status ?: HealthStatus.ELEVATED
                val statusText = stringResource(viewModel.getHealthStatusLabel(status))
                val confidence = prediction?.confidence.orEmpty()
                val normalPct = ((confidence[HealthStatus.NORMAL] ?: 0f) * 100).toInt()
                val elevatedPct = ((confidence[HealthStatus.ELEVATED] ?: 0f) * 100).toInt()
                val criticalPct = ((confidence[HealthStatus.CRITICAL] ?: 0f) * 100).toInt()

                val statusContainerColor = when (status) {
                    HealthStatus.NORMAL -> MaterialTheme.colorScheme.secondaryContainer
                    HealthStatus.ELEVATED -> Color(0xFFFFE0B2)
                    HealthStatus.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                }

                val statusTextColor = when (status) {
                    HealthStatus.NORMAL -> MaterialTheme.colorScheme.primary
                    HealthStatus.ELEVATED -> Color(0xFFEF6C00)
                    HealthStatus.CRITICAL -> MaterialTheme.colorScheme.error
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = statusContainerColor
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.zdravstveno_stanje),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = statusTextColor
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = stringResource(
                                R.string.confidence_line,
                                normalPct,
                                elevatedPct,
                                criticalPct
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (prediction?.fromTflite == true) {
                                stringResource(R.string.tflite_model_used)
                            } else {
                                stringResource(R.string.rule_based_fallback)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.meritev_ni_najdena),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

