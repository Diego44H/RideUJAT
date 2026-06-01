package mx.ujat.dacyti.rideujat.ui.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProfileClick: () -> Unit = {},
    onPublishTripClick: () -> Unit = {},
    onMyTripsClick: () -> Unit = {},
    onSearchTripsClick: () -> Unit = {},
    onActiveTripClick: (tripId: String, isConductor: Boolean) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RideUJAT", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Mi perfil")
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
            item { Spacer(Modifier.height(8.dp)) }

            // Viaje activo
            if (uiState.activeTripId != null) {
                item {
                    HomeActionCard(
                        icon = Icons.Filled.LocationOn,
                        title = "Viaje Activo",
                        subtitle = if (uiState.isConductor) "Ver trayecto" else "Ver trayecto",
                        onClick = { onActiveTripClick(uiState.activeTripId!!, uiState.isConductor) },
                        modifier = Modifier.fillMaxWidth(),
                        cardColor = Color(0xFF4CAF50)
                    )
                }
            }

            // Sección conductor
            item {
                Text("Soy conductor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeActionCard(
                        icon = Icons.Filled.Add,
                        title = "Publicar Viaje",
                        subtitle = "Ofrece un lugar",
                        onClick = onPublishTripClick,
                        modifier = Modifier.weight(1f)
                    )
                    HomeActionCard(
                        icon = Icons.Filled.DirectionsCar,
                        title = "Mis Viajes",
                        subtitle = "Gestionar publicaciones",
                        onClick = onMyTripsClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Sección pasajero
            item {
                Text("Soy pasajero", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                HomeActionCard(
                    icon = Icons.Filled.Search,
                    title = "Buscar Viaje",
                    subtitle = "Encuentra un lugar al campus",
                    onClick = onSearchTripsClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardColor: Color? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (cardColor != null) CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.1f)) else CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = cardColor ?: MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
