package mx.ujat.dacyti.rideujat.ui.active

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlagCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import mx.ujat.dacyti.rideujat.data.model.TripEstado

// Coordenadas por defecto: Campus DACyTI UJAT, Villahermosa
private val DEFAULT_LOCATION = LatLng(17.9890, -92.9478)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTripScreen(
    tripId: String,
    isConductor: Boolean,
    onBack: () -> Unit,
    onTripFinished: () -> Unit,
    onChatClick: (tripId: String) -> Unit = {},
    viewModel: ActiveTripViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showSosAlert by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, 14f)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) viewModel.startConductorTracking()
    }

    LaunchedEffect(tripId) {
        viewModel.initialize(tripId, isConductor)
        if (isConductor) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) viewModel.startConductorTracking()
            else locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    val lat = uiState.conductorLat
    val lng = uiState.conductorLng
    LaunchedEffect(lat, lng) {
        if (lat != null && lng != null) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLng(LatLng(lat, lng)))
        }
    }

    LaunchedEffect(uiState.tripFinished) {
        if (uiState.tripFinished) onTripFinished()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    DisposableEffect(Unit) {
        onDispose { if (isConductor) viewModel.stopConductorTracking() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isConductor) "Trayecto Activo" else "Mi Viaje") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar") }
                },
                actions = {
                    // Chat
                    IconButton(onClick = { onChatClick(tripId) }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    // SOS — siempre visible durante el trayecto
                    SosLongPressButton(
                        onSosActivated = { showSosAlert = true },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Mapa full screen
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = false)
            ) {
                if (lat != null && lng != null) {
                    Marker(
                        state = MarkerState(position = LatLng(lat, lng)),
                        title = "Conductor",
                        snippet = uiState.tripDetails?.conductor?.nombre ?: ""
                    )
                }
            }

            // Panel inferior con info + botones
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val trip = uiState.tripDetails?.trip
                    val vehicle = uiState.tripDetails?.vehicle

                    // Ruta
                    Text(
                        "${trip?.origen ?: "—"}  →  ${trip?.destino ?: "—"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        "${trip?.fechaSalida ?: ""}  ·  ${trip?.horaSalida?.take(5) ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(6.dp))

                    vehicle?.let { v ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DirectionsCar, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text("${v.marca} ${v.modelo}  ·  ${v.color}  ·  ${v.placas}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    if (!isConductor) {
                        // Info del conductor para el pasajero
                        Text(
                            "Conductor: ${uiState.tripDetails?.conductor?.nombre ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    // Botones del conductor
                    if (isConductor) {
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            when (trip?.estado) {
                                TripEstado.PUBLICADO -> {
                                    Button(
                                        onClick = { viewModel.startTrip() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Iniciar Viaje", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                TripEstado.EN_CURSO -> {
                                    Button(
                                        onClick = { showFinishDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Filled.FlagCircle, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Finalizar Viaje", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmación para finalizar viaje
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finalizar viaje") },
            text = { Text("¿Confirmas que el viaje ha concluido? Se habilitarán las calificaciones.") },
            confirmButton = {
                TextButton(onClick = { viewModel.finishTrip(); showFinishDialog = false }) {
                    Text("Sí, finalizar")
                }
            },
            dismissButton = { TextButton(onClick = { showFinishDialog = false }) { Text("Cancelar") } }
        )
    }

    // Confirmación para SOS
    if (showSosAlert) {
        AlertDialog(
            onDismissRequest = { showSosAlert = false },
            title = { Text("🚨 ALERTA SOS") },
            text = { Text("Se enviarán mensajes WhatsApp a tus contactos de emergencia con tu ubicación actual. ¿Continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    sendSosToContacts(
                        context = context,
                        contacts = emptyList(),
                        userName = uiState.tripDetails?.conductor?.nombre ?: "Usuario",
                        lat = uiState.conductorLat,
                        lng = uiState.conductorLng
                    )
                    showSosAlert = false
                }) {
                    Text("Sí, enviar SOS", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showSosAlert = false }) { Text("Cancelar") } }
        )
    }
}
