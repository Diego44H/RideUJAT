package mx.ujat.dacyti.rideujat.ui.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishTripScreen(
    tripId: String? = null,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: PublishTripViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var vehicleExpanded by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.fechaMillis ?: System.currentTimeMillis()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = uiState.hora.take(2).toIntOrNull() ?: 7,
        initialMinute = uiState.hora.drop(3).take(2).toIntOrNull() ?: 0,
        is24Hour = true
    )

    LaunchedEffect(tripId) {
        if (tripId != null) viewModel.loadTripForEdit(tripId)
    }
    LaunchedEffect(uiState.success) {
        if (uiState.success) { viewModel.resetSuccess(); onSuccess() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Editar Viaje" else "Publicar Viaje") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Aviso si no puede editar
            if (uiState.isEditMode && uiState.hasAcceptedPassengers) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = "Este viaje ya tiene pasajeros aceptados y no puede modificarse.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Vehículo
            item {
                Text("Vehículo", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = vehicleExpanded,
                    onExpandedChange = { if (!uiState.isEditMode) vehicleExpanded = !vehicleExpanded }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedVehicle
                            ?.let { "${it.marca} ${it.modelo} — ${it.placas}" }
                            ?: "Selecciona un vehículo",
                        onValueChange = {},
                        readOnly = true,
                        enabled = !uiState.isEditMode,
                        trailingIcon = { if (!uiState.isEditMode) ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = vehicleExpanded, onDismissRequest = { vehicleExpanded = false }) {
                        if (uiState.vehicles.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Sin vehículos — regístralos en tu perfil", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = { vehicleExpanded = false }
                            )
                        }
                        uiState.vehicles.forEach { v ->
                            DropdownMenuItem(
                                text = { Text("${v.marca} ${v.modelo} — ${v.placas}") },
                                onClick = { viewModel.selectVehicle(v); vehicleExpanded = false }
                            )
                        }
                    }
                }
            }

            // Origen
            item {
                OutlinedTextField(
                    value = uiState.origen,
                    onValueChange = { viewModel.setOrigen(it) },
                    label = { Text("Origen") },
                    placeholder = { Text("Ej: Col. México, Villahermosa") },
                    singleLine = true,
                    enabled = !(uiState.isEditMode && uiState.hasAcceptedPassengers),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Destino
            item {
                OutlinedTextField(
                    value = uiState.destino,
                    onValueChange = { viewModel.setDestino(it) },
                    label = { Text("Destino") },
                    singleLine = true,
                    enabled = !(uiState.isEditMode && uiState.hasAcceptedPassengers),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Fecha + Hora
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        enabled = !(uiState.isEditMode && uiState.hasAcceptedPassengers),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            uiState.fechaMillis?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            } ?: "Fecha",
                            maxLines = 1
                        )
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        enabled = !(uiState.isEditMode && uiState.hasAcceptedPassengers),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Schedule, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (uiState.hora.isNotBlank()) uiState.hora.take(5) else "Hora")
                    }
                }
            }

            // Asientos
            item {
                Text("Asientos disponibles", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.decrementAsientos() },
                        enabled = !(uiState.isEditMode && uiState.hasAcceptedPassengers)
                    ) { Icon(Icons.Filled.Remove, null) }
                    Text(
                        "${uiState.asientos}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    IconButton(
                        onClick = { viewModel.incrementAsientos() },
                        enabled = !(uiState.isEditMode && uiState.hasAcceptedPassengers)
                    ) { Icon(Icons.Filled.Add, null) }
                    Text("(máx. 7)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Tarifa
            item {
                OutlinedTextField(
                    value = uiState.tarifa,
                    onValueChange = { viewModel.setTarifa(it) },
                    label = { Text("Tarifa sugerida (MXN)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("$", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 4.dp)) },
                    enabled = !(uiState.isEditMode && uiState.hasAcceptedPassengers),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Tiempo estimado
            item {
                var showTiempoDialog by remember { mutableStateOf(false) }
                val totalMin = uiState.tiempoEstimado.toIntOrNull() ?: 0
                OutlinedButton(
                    onClick = { showTiempoDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val label = if (totalMin > 0) {
                        val h = totalMin / 60; val m = totalMin % 60
                        if (h > 0 && m > 0) "${h}h ${m}min" else if (h > 0) "${h}h" else "${m}min"
                    } else "Tiempo estimado (opcional)"
                    Text(label)
                }
                if (showTiempoDialog) {
                    var selectedHoras by remember { mutableStateOf((totalMin / 60).coerceIn(0, 5)) }
                    var selectedMinutos by remember { mutableStateOf((totalMin % 60).coerceIn(0, 59)) }
                    AlertDialog(
                        onDismissRequest = { showTiempoDialog = false },
                        title = { Text("Tiempo estimado", style = MaterialTheme.typography.titleLarge) },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Horas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.height(4.dp))
                                        TiempoWheelColumn(
                                            count = 6,
                                            selectedIndex = selectedHoras,
                                            onIndexSelected = { selectedHoras = it },
                                            label = { it.toString() }
                                        )
                                    }
                                    Text(":", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 12.dp))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Minutos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.height(4.dp))
                                        TiempoWheelColumn(
                                            count = 60,
                                            selectedIndex = selectedMinutos,
                                            onIndexSelected = { selectedMinutos = it },
                                            label = { "%02d".format(it) }
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val total = selectedHoras * 60 + selectedMinutos
                                if (total > 0) viewModel.setTiempoEstimado(total.toString())
                                showTiempoDialog = false
                            }) { Text("Aceptar") }
                        },
                        dismissButton = { TextButton(onClick = { showTiempoDialog = false }) { Text("Cancelar") } }
                    )
                }
            }

            if (uiState.error != null) {
                item {
                    Text(
                        uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Botón submit
            item {
                Button(
                    onClick = { viewModel.submitTrip() },
                    enabled = !uiState.isLoading && !(uiState.isEditMode && uiState.hasAcceptedPassengers),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(if (uiState.isEditMode) "Actualizar Viaje" else "Publicar Viaje", fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setFechaMillis(it) }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setHora(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") } },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

@Composable
private fun TiempoWheelColumn(
    count: Int,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    label: (Int) -> String
) {
    val itemHeightDp = 48.dp
    val visibleItems = 5
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index -> onIndexSelected(index.coerceIn(0, count - 1)) }
    }

    Box(
        modifier = Modifier
            .width(72.dp)
            .height(itemHeightDp * visibleItems),
        contentAlignment = Alignment.Center
    ) {
        // Líneas que marcan el elemento seleccionado
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(72.dp)
                .height(itemHeightDp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
        )

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = itemHeightDp * 2),
            flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(listState),
            modifier = Modifier.width(72.dp).height(itemHeightDp * visibleItems)
        ) {
            items(count) { index ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(itemHeightDp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label(index),
                        style = if (isSelected) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
