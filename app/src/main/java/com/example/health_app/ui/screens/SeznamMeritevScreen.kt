package com.example.health_app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.health_app.R
import com.example.health_app.data.Meritev
import com.example.health_app.viewmodel.MeritevViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeznamMeritevScreen(
    viewModel: MeritevViewModel,
    onNavigateBack: () -> Unit,
    onSync: () -> Unit,
    onLogout: () -> Unit,
    loggedInEmail: String?,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateToStatistics: () -> Unit = {}
) {
    val vseMeritve by viewModel.vseMeritve.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var meritevToDelete by remember { mutableStateOf<Meritev?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Filter measurements by search query
    val filteredMeritve = if (searchQuery.isBlank()) {
        vseMeritve
    } else {
        vseMeritve.filter {
            "${it.ime} ${it.priimek}".contains(searchQuery, ignoreCase = true)
        }
    }

    val operationResult by viewModel.operationResult.collectAsStateWithLifecycle()
    val lastSyncCount by viewModel.lastSyncCount.collectAsStateWithLifecycle()
    val msgIzbrisana = stringResource(R.string.meritev_izbrisana)
    val msgSync = stringResource(R.string.sinhronizirano, lastSyncCount ?: 0)

    LaunchedEffect(operationResult) {
        when (operationResult) {
            MeritevViewModel.OperationResult.DELETED -> {
                snackbarHostState.showSnackbar(msgIzbrisana)
                viewModel.clearOperationResult()
            }
            MeritevViewModel.OperationResult.SYNCED -> {
                snackbarHostState.showSnackbar(msgSync)
                viewModel.clearOperationResult()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.seznam_meritev)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.preklici)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToStatistics) {
                        Text(stringResource(R.string.statistika))
                    }
                    TextButton(onClick = onSync) {
                        Text(stringResource(R.string.sinhroniziraj))
                    }
                    TextButton(onClick = onLogout) {
                        Text(stringResource(R.string.odjava))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            if (!loggedInEmail.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.prijavljen_kot, loggedInEmail),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            androidx.compose.material3.OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(stringResource(R.string.iskanje)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            if (filteredMeritve.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.ni_meritev),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                ) {
                    items(
                        items = filteredMeritve,
                        key = { it.id }
                    ) { meritev ->
                        MeritevSwipeItem(
                            meritev = meritev,
                            onClick = { onNavigateToDetail(meritev.id) },
                            onEdit = { onNavigateToEdit(meritev.id) },
                            onDelete = { meritevToDelete = meritev }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    meritevToDelete?.let { meritev ->
        AlertDialog(
            onDismissRequest = { meritevToDelete = null },
            title = { Text(stringResource(R.string.potrdi_brisanje)) },
            text = { Text(stringResource(R.string.potrdi_brisanje_sporocilo)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.izbrisi(meritev)
                    meritevToDelete = null
                }) {
                    Text(stringResource(R.string.izbrisi))
                }
            },
            dismissButton = {
                TextButton(onClick = { meritevToDelete = null }) {
                    Text(stringResource(R.string.preklici))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeritevSwipeItem(
    meritev: Meritev,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false // don't dismiss yet, let dialog confirm
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFFFCDD2)
                    else -> Color.Transparent
                },
                label = "swipe_bg_color"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.izbrisi),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        MeritevCard(meritev = meritev, onClick = onClick, onEdit = onEdit)
    }
}

@Composable
private fun MeritevCard(
    meritev: Meritev,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${meritev.ime} ${meritev.priimek}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(meritev.datum)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.bpm_value, meritev.srcniUtrip),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.spo2_value, meritev.spO2),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.uredi)
                    )
                }
            }
        }
    }
}

