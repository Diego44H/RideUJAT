package mx.ujat.dacyti.rideujat.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.ujat.dacyti.rideujat.core.supabase
import mx.ujat.dacyti.rideujat.data.model.Trip
import mx.ujat.dacyti.rideujat.data.model.TripEstado
import mx.ujat.dacyti.rideujat.data.model.TripRequest
import mx.ujat.dacyti.rideujat.data.model.RequestEstado

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { startPolling() }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                loadActiveTrip()
                delay(5000) // Verificar cada 5 segundos
            }
        }
    }

    fun loadActiveTrip() {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            try {
                // Check for active trips as conductor
                val conductorTrip = runCatching {
                    supabase.postgrest["trips"].select {
                        filter {
                            eq("conductor_id", userId)
                            eq("estado", TripEstado.EN_CURSO)
                        }
                    }.decodeSingle<Trip>()
                }.getOrNull()

                if (conductorTrip != null) {
                    _uiState.update { it.copy(activeTripId = conductorTrip.id, isConductor = true) }
                    return@launch
                }

                // Check for active trips as passenger - verify trip is also EN_CURSO
                val passengerRequest = runCatching {
                    supabase.postgrest["trip_requests"].select {
                        filter {
                            eq("pasajero_id", userId)
                            eq("estado", RequestEstado.ACEPTADO)
                        }
                    }.decodeSingle<TripRequest>()
                }.getOrNull()

                if (passengerRequest != null) {
                    // Verify the trip is still EN_CURSO
                    val trip = runCatching {
                        supabase.postgrest["trips"].select {
                            filter { eq("id", passengerRequest.tripId) }
                        }.decodeSingle<Trip>()
                    }.getOrNull()

                    if (trip != null && trip.estado == TripEstado.EN_CURSO) {
                        _uiState.update { it.copy(activeTripId = passengerRequest.tripId, isConductor = false) }
                    } else {
                        _uiState.update { it.copy(activeTripId = null, isConductor = false) }
                    }
                } else {
                    // No active trip found
                    _uiState.update { it.copy(activeTripId = null, isConductor = false) }
                }
            } catch (e: Exception) {
                // No active trip found
                _uiState.update { it.copy(activeTripId = null, isConductor = false) }
            }
        }
    }
}

data class HomeUiState(
    val activeTripId: String? = null,
    val isConductor: Boolean = false
)
