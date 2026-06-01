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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
                OutlinedButton(
                    onClick = { showTiempoDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (uiState.tiempoEstimado.isNotBlank()) "${uiState.tiempoEstimado} min" else "Tiempo estimado (opcional)")
                }
                if (showTiempoDialog) {
                    var selectedMinutos by remember { mutableStateOf(uiState.tiempoEstimado.toIntOrNull()?.minus(1)?.coerceIn(0, 59) ?: 0) }
                    AlertDialog(
                        onDismissRequest = { showTiempoDialog = false },
                        title = { Text("Tiempo estimado") },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "${selectedMinutos + 1} minutos",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(8.dp))
                                MinutosWheelPicker(
                                    selectedIndex = selectedMinutos,
                                    onIndexSelected = { selectedMinutos = it }
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.setTiempoEstimado((selectedMinutos + 1).toString())
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
private fun MinutosWheelPicker(
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit
) {
    val itemCount = 60
    val visibleItems = 5
    val itemHeightDp = 48.dp
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                onIndexSelected(index.coerceIn(0, itemCount - 1))
            }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeightDp * visibleItems),
        contentAlignment = Alignment.Center
    ) {
        // Líneas de selección
        HorizontalDivider(
            modifier = Modifier.align(Alignment.Center).padding(bottom = itemHeightDp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            thickness = 1.dp
        )
        HorizontalDivider(
            modifier = Modifier.align(Alignment.Center).padding(top = itemHeightDp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            thickness = 1.dp
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = 0.99f }
                .drawWithContent {
                    drawContent()
                    // Degradado superior e inferior para efecto de rueda
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.White,
                            0.35f to Color.Transparent,
                            0.65f to Color.Transparent,
                            1f to Color.White
                        ),
                        blendMode = BlendMode.DstIn
                    )
                },
            contentPadding = PaddingValues(vertical = itemHeightDp * 2),
            flingBehavior = androidx.compose.foundation.lazy.rememberLazyListState().let {
                androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(listState)
            }
        ) {
            items(itemCount) { index ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = if (isSelected) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
