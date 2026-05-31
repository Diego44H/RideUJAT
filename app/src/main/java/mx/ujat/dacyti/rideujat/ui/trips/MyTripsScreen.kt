package mx.ujat.dacyti.rideujat.ui.trips

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.ujat.dacyti.rideujat.data.model.Trip
import mx.ujat.dacyti.rideujat.data.model.TripEstado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTripsScreen(
    onBack: () -> Unit,
    onPublishTrip: () -> Unit,
    onEditTrip: (tripId: String) -> Unit,
    onManageRequests: (tripId: String) -> Unit = {},
    onActiveTrip: (tripId: String) -> Unit = {},
    viewModel: MyTripsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var tripToCancel by remember { mutableStateOf<Trip?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Viajes") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onPublishTrip) {
                Icon(Icons.Filled.Add, "Publicar viaje")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        if (uiState.trips.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.DirectionsCar, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("Aún no has publicado viajes", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Toca + para publicar uno", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.trips, key = { it.id ?: it.fechaSalida }) { trip ->
                TripCard(
                    trip = trip,
                    onEdit = { trip.id?.let { onEditTrip(it) } },
                    onCancel = { tripToCancel = trip },
                    onManageRequests = { trip.id?.let { onManageRequests(it) } },
                    onActiveTrip = { trip.id?.let { onActiveTrip(it) } }
                )
            }
        }
    }

    tripToCancel?.let { trip ->
        AlertDialog(
            onDismissRequest = { tripToCancel = null },
            title = { Text("Cancelar viaje") },
            text = { Text("¿Seguro que deseas cancelar este viaje? Se notificará a los pasajeros.") },
            confirmButton = {
                TextButton(onClick = {
                    trip.id?.let { viewModel.cancelTrip(it) }
                    tripToCancel = null
                }) { Text("Sí, cancelar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { tripToCancel = null }) { Text("Volver") } }
        )
    }
}

@Composable
private fun TripCard(trip: Trip, onEdit: () -> Unit, onCancel: () -> Unit, onManageRequests: () -> Unit = {}, onActiveTrip: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Ruta
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(trip.origen, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(trip.destino, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            // Info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("${trip.fechaSalida}  ${trip.horaSalida.take(5)}", style = MaterialTheme.typography.bodySmall)
                    Text("${trip.asientosDisponibles}/${trip.asientosTotal} lugares", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$${trip.tarifa}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    TripStatusChip(estado = trip.estado)
                }
            }

            // Acciones solo para viajes publicados
            if (trip.estado == TripEstado.PUBLICADO || trip.estado == TripEstado.EN_CURSO) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onActiveTrip) {
                        Text("Ir al trayecto", color = MaterialTheme.colorScheme.primary)
                    }
                    if (trip.estado == TripEstado.PUBLICADO) {
                        TextButton(onClick = onManageRequests) { Text("Solicitudes") }
                        TextButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Editar")
                        }
                        TextButton(onClick = onCancel) {
                            Text("Cancelar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripStatusChip(estado: String) {
    val (bg, fg, label) = when (estado) {
        TripEstado.PUBLICADO  -> Triple(Color(0xFF4CAF50).copy(alpha = 0.15f), Color(0xFF2E7D32), "Publicado")
        TripEstado.EN_CURSO   -> Triple(Color(0xFF2196F3).copy(alpha = 0.15f), Color(0xFF1565C0), "En curso")
        TripEstado.FINALIZADO -> Triple(Color(0xFF9E9E9E).copy(alpha = 0.15f), Color(0xFF616161), "Finalizado")
        TripEstado.CANCELADO  -> Triple(Color(0xFFF44336).copy(alpha = 0.15f), Color(0xFFC62828), "Cancelado")
        else                  -> Triple(Color(0xFF9E9E9E).copy(alpha = 0.15f), Color(0xFF616161), estado)
    }
    Surface(color = bg, shape = MaterialTheme.shapes.small) {
        Text(
            label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
