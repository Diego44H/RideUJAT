package mx.ujat.dacyti.rideujat.ui.search

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import mx.ujat.dacyti.rideujat.data.model.TripWithDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTripsScreen(
    onBack: () -> Unit,
    onTripClick: (tripId: String) -> Unit,
    viewModel: SearchTripsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar Viaje") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChange(it) },
                placeholder = { Text("Origen, destino o conductor…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Scaffold
            }

            if (uiState.filteredTrips.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.DirectionsCar, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(12.dp))
                        Text("Sin viajes disponibles", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                return@Scaffold
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.filteredTrips, key = { it.trip.id ?: it.trip.fechaSalida }) { details ->
                    TripSearchCard(details = details, onClick = { details.trip.id?.let { onTripClick(it) } })
                }
            }
        }
    }
}

@Composable
private fun TripSearchCard(details: TripWithDetails, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Conductor row
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConductorAvatar(
                    nombre = details.conductor?.nombre ?: "?",
                    fotoUrl = details.conductor?.fotoUrl
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(details.conductor?.nombre ?: "Conductor", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, Modifier.size(14.dp), tint = Color(0xFFFFD700))
                        Spacer(Modifier.width(2.dp))
                        Text("%.1f".format(details.conductor?.ratingPromedio ?: 5.0), style = MaterialTheme.typography.bodySmall)
                    }
                }
                AvailabilityBadge(asientos = details.trip.asientosDisponibles)
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))

            // Ruta
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("De: ${details.trip.origen}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    Text("A: ${details.trip.destino}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }

            Spacer(Modifier.height(6.dp))

            // Fecha + vehículo + precio
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${details.trip.fechaSalida}  ${details.trip.horaSalida.take(5)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$${details.trip.tarifa}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            details.vehicle?.let { v ->
                Text(
                    "${v.marca} ${v.modelo} · ${v.color} · ${v.placas}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun ConductorAvatar(nombre: String, fotoUrl: String?, size: Int = 44) {
    if (!fotoUrl.isNullOrBlank()) {
        AsyncImage(
            model = fotoUrl,
            contentDescription = "Foto de $nombre",
            modifier = Modifier.size(size.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier.size(size.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                nombre.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun AvailabilityBadge(asientos: Int) {
    val (bg, fg, label) = when {
        asientos >= 2 -> Triple(Color(0xFF4CAF50).copy(alpha = 0.15f), Color(0xFF2E7D32), "Disponible")
        asientos == 1 -> Triple(Color(0xFFFF9800).copy(alpha = 0.15f), Color(0xFFE65100), "Último lugar")
        else -> Triple(Color(0xFFF44336).copy(alpha = 0.15f), Color(0xFFC62828), "Lleno")
    }
    Surface(color = bg, shape = MaterialTheme.shapes.small) {
        Text(label, color = fg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}
