package mx.ujat.dacyti.rideujat.ui.search

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.ujat.dacyti.rideujat.data.model.RequestEstado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: String,
    onBack: () -> Unit,
    onActiveTrip: (tripId: String) -> Unit = {},
    onChatClick: (tripId: String) -> Unit = {},
    viewModel: TripDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(tripId) { viewModel.loadTrip(tripId) }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    LaunchedEffect(uiState.notification) {
        uiState.notification?.let { snackbarHostState.showSnackbar(it); viewModel.clearNotification() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Viaje") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (uiState.isLoading || uiState.tripDetails == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        val details = uiState.tripDetails!!
        val trip = details.trip
        val conductor = details.conductor
        val vehicle = details.vehicle

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Conductor
            item {
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        ConductorAvatar(nombre = conductor?.nombre ?: "?", fotoUrl = conductor?.fotoUrl, size = 56)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(conductor?.nombre ?: "Conductor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(5) { i ->
                                    Icon(Icons.Filled.Star, null, Modifier.size(16.dp),
                                        tint = if (i < (conductor?.ratingPromedio?.toInt() ?: 5)) Color(0xFFFFD700) else MaterialTheme.colorScheme.outlineVariant)
                                }
                                Spacer(Modifier.width(4.dp))
                                Text("%.1f".format(conductor?.ratingPromedio ?: 5.0), style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${conductor?.viajesCount ?: 0} viajes realizados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Viaje
            item {
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Ruta", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        LabelValue("Origen", trip.origen)
                        LabelValue("Destino", trip.destino)
                        LabelValue("Fecha", trip.fechaSalida)
                        LabelValue("Hora", trip.horaSalida.take(5))
                        trip.tiempoEstimado?.let { LabelValue("Tiempo estimado", it) }
                    }
                }
            }

            // Vehículo
            vehicle?.let { v ->
                item {
                    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DirectionsCar, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("${v.marca} ${v.modelo}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("${v.color} · Placas: ${v.placas}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Precio y asientos
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(0.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Tarifa", style = MaterialTheme.typography.labelMedium)
                            Text("$${trip.tarifa}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Asientos disponibles", style = MaterialTheme.typography.labelMedium)
                            AvailabilityBadge(trip.asientosDisponibles)
                        }
                    }
                }
            }

            // Botón / Estado de solicitud
            item {
                val req = uiState.myRequest
                when (req?.estado) {
                    null, RequestEstado.CANCELADO_PASAJERO -> {
                        if (uiState.isOwnTrip) {
                            Text("Este es tu viaje", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Button(
                                onClick = { viewModel.requestTrip(tripId) },
                                enabled = !uiState.isRequesting && trip.asientosDisponibles > 0,
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                if (uiState.isRequesting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                else Text("Solicitar Viaje", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    RequestEstado.PENDIENTE -> {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.HourglassEmpty, null, tint = Color(0xFFE65100))
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Solicitud enviada", fontWeight = FontWeight.SemiBold, color = Color(0xFFE65100))
                                    Text("Esperando respuesta del conductor", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(onClick = { viewModel.cancelRequest(tripId) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancelar solicitud")
                        }
                    }
                    RequestEstado.ACEPTADO -> {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("¡Solicitud aceptada!", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    Text("Ya tienes un lugar en este viaje", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onActiveTrip(tripId) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                            ) { Text("Ver Mapa", fontWeight = FontWeight.SemiBold) }
                            Button(
                                onClick = { onChatClick(tripId) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Ir al Chat", fontWeight = FontWeight.SemiBold) }
                        }
                    }
                    RequestEstado.RECHAZADO -> {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text("Solicitud no aceptada por el conductor.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Spacer(Modifier.height(4.dp))
                        Button(onClick = { viewModel.requestTrip(tripId) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Volver a solicitar")
                        }
                    }
                    else -> {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text("Estado de tu solicitud: ${req.estado}", modifier = Modifier.padding(16.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
