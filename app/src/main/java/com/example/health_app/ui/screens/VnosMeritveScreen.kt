package com.example.health_app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
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
fun VnosMeritveScreen(
    viewModel: MeritevViewModel,
    meritevId: Int? = null,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToList: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Form state
    var ime by rememberSaveable { mutableStateOf("") }
    var priimek by rememberSaveable { mutableStateOf("") }
    var datum by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var srcniUtripText by rememberSaveable { mutableStateOf("") }
    var spO2Text by rememberSaveable { mutableStateOf("") }
    var temperaturaText by rememberSaveable { mutableStateOf("") }

    // Error state
    var imeError by rememberSaveable { mutableStateOf(false) }
    var priimekError by rememberSaveable { mutableStateOf(false) }
    var srcniUtripError by rememberSaveable { mutableStateOf<String?>(null) }
    var spO2Error by rememberSaveable { mutableStateOf<String?>(null) }
    var temperaturaError by rememberSaveable { mutableStateOf<String?>(null) }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var isLoaded by rememberSaveable { mutableStateOf(false) }
    var newlyInsertedId by rememberSaveable { mutableStateOf<Int?>(null) }

    fun resetForm() {
        ime = ""
        priimek = ""
        datum = System.currentTimeMillis()
        srcniUtripText = ""
        spO2Text = ""
        temperaturaText = ""

        imeError = false
        priimekError = false
        srcniUtripError = null
        spO2Error = null
        temperaturaError = null
    }

    // Load existing measurement for editing
    if (meritevId != null && !isLoaded) {
        val meritev by viewModel.getMeritevById(meritevId)
            .collectAsStateWithLifecycle(initialValue = null)
        meritev?.let {
            ime = it.ime
            priimek = it.priimek
            datum = it.datum
            srcniUtripText = it.srcniUtrip.toString()
            spO2Text = it.spO2.toString()
            temperaturaText = it.temperatura.toString()
            isLoaded = true
        }
    }

    // Observe operation result for snackbar
    val operationResult by viewModel.operationResult.collectAsStateWithLifecycle()
    val msgShranjena = stringResource(R.string.meritev_shranjena)
    val msgPosodobljena = stringResource(R.string.meritev_posodobljena)
    val msgNapaka = stringResource(R.string.napaka_shranjevanja)
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(statusMessage) {
        statusMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(operationResult) {
        when (operationResult) {
            MeritevViewModel.OperationResult.INSERTED -> {
                snackbarHostState.showSnackbar(msgShranjena)
                viewModel.clearOperationResult()
                resetForm()
                newlyInsertedId?.let(onNavigateToDetail) ?: onNavigateToList()
                newlyInsertedId = null
            }
            MeritevViewModel.OperationResult.UPDATED -> {
                snackbarHostState.showSnackbar(msgPosodobljena)
                viewModel.clearOperationResult()
                if (meritevId != null) {
                    onNavigateToDetail(meritevId)
                } else {
                    onNavigateToList()
                }
            }
            MeritevViewModel.OperationResult.ERROR -> {
                snackbarHostState.showSnackbar(msgNapaka)
                viewModel.clearOperationResult()
            }
            else -> {}
        }
    }

    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    // Validation strings
    val poljeObvezno = stringResource(R.string.polje_obvezno)
    val srcniUtripNapaka = stringResource(R.string.srcni_utrip_napaka)
    val spO2Napaka = stringResource(R.string.spo2_napaka)
    val temperaturaNapaka = stringResource(R.string.temperatura_napaka)
    val neveljavenVnos = stringResource(R.string.neveljaven_vnos)

    val title = if (meritevId != null) stringResource(R.string.uredi_meritev)
                else stringResource(R.string.nova_meritev)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Ime
            OutlinedTextField(
                value = ime,
                onValueChange = {
                    ime = it
                    imeError = false
                },
                label = { Text(stringResource(R.string.ime)) },
                isError = imeError,
                supportingText = if (imeError) ({ Text(poljeObvezno) }) else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Priimek
            OutlinedTextField(
                value = priimek,
                onValueChange = {
                    priimek = it
                    priimekError = false
                },
                label = { Text(stringResource(R.string.priimek)) },
                isError = priimekError,
                supportingText = if (priimekError) ({ Text(poljeObvezno) }) else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Datum
            OutlinedTextField(
                value = dateFormat.format(Date(datum)),
                onValueChange = {},
                label = { Text(stringResource(R.string.datum_meritve)) },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = stringResource(R.string.datum_meritve)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Srčni utrip
            OutlinedTextField(
                value = srcniUtripText,
                onValueChange = {
                    srcniUtripText = it
                    srcniUtripError = null
                },
                label = { Text(stringResource(R.string.srcni_utrip)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = srcniUtripError != null,
                supportingText = srcniUtripError?.let { error -> { Text(error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TextButton(
                onClick = {
                    viewModel.preberiSrcniUtrip { value -> srcniUtripText = value.toString() }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.preberi_srcni_utrip))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SpO2
            OutlinedTextField(
                value = spO2Text,
                onValueChange = {
                    spO2Text = it
                    spO2Error = null
                },
                label = { Text(stringResource(R.string.spo2)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = spO2Error != null,
                supportingText = spO2Error?.let { error -> { Text(error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TextButton(
                onClick = {
                    viewModel.preberiSpO2 { value -> spO2Text = value.toString() }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.preberi_spo2))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Temperatura
            OutlinedTextField(
                value = temperaturaText,
                onValueChange = {
                    temperaturaText = it
                    temperaturaError = null
                },
                label = { Text(stringResource(R.string.temperatura)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = temperaturaError != null,
                supportingText = temperaturaError?.let { error -> { Text(error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TextButton(
                onClick = {
                    viewModel.preberiTemperaturo { value ->
                        temperaturaText = String.format(Locale.US, "%.1f", value)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.preberi_temperaturo))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    // Validate
                    var isValid = true

                    if (ime.isBlank()) {
                        imeError = true
                        isValid = false
                    }
                    if (priimek.isBlank()) {
                        priimekError = true
                        isValid = false
                    }

                    val srcniUtrip = srcniUtripText.toIntOrNull()
                    if (srcniUtrip == null) {
                        srcniUtripError = neveljavenVnos
                        isValid = false
                    } else if (srcniUtrip < 30 || srcniUtrip > 250) {
                        srcniUtripError = srcniUtripNapaka
                        isValid = false
                    }

                    val spO2 = spO2Text.toIntOrNull()
                    if (spO2 == null) {
                        spO2Error = neveljavenVnos
                        isValid = false
                    } else if (spO2 < 0 || spO2 > 100) {
                        spO2Error = spO2Napaka
                        isValid = false
                    }

                    val temperatura = temperaturaText.replace(",", ".").toDoubleOrNull()
                    if (temperatura == null) {
                        temperaturaError = neveljavenVnos
                        isValid = false
                    } else if (temperatura < 34.0 || temperatura > 42.0) {
                        temperaturaError = temperaturaNapaka
                        isValid = false
                    }

                    if (isValid) {
                        val meritev = Meritev(
                            id = meritevId ?: 0,
                            ime = ime.trim(),
                            priimek = priimek.trim(),
                            datum = datum,
                            srcniUtrip = srcniUtrip!!,
                            spO2 = spO2!!,
                            temperatura = temperatura!!
                        )
                        if (meritevId != null) {
                            viewModel.posodobi(meritev)
                        } else {
                            viewModel.vstavi(meritev) { id ->
                                newlyInsertedId = id
                                resetForm()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.shrani_meritev))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // List button
            OutlinedButton(
                onClick = onNavigateToList,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_seznam_meritev))
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = datum)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        datum = it
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.potrdi))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.preklici))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

