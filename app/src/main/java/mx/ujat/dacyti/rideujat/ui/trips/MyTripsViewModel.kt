package mx.ujat.dacyti.rideujat.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.ujat.dacyti.rideujat.core.supabase
import mx.ujat.dacyti.rideujat.data.model.Trip
import mx.ujat.dacyti.rideujat.data.repository.TripRepository

class MyTripsViewModel : ViewModel() {

    private val repository = TripRepository()

    private val _uiState = MutableStateFlow(MyTripsUiState())
    val uiState: StateFlow<MyTripsUiState> = _uiState.asStateFlow()

    init { loadTrips() }

    fun loadTrips() {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.loadMyTrips(userId).fold(
                onSuccess = { trips -> _uiState.update { it.copy(isLoading = false, trips = trips) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun clearNotification() { _uiState.update { it.copy(notification = null) } }

    fun cancelTrip(tripId: String) {
        viewModelScope.launch {
            repository.cancelTrip(tripId).fold(
                onSuccess = { loadTrips() },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}

data class MyTripsUiState(
    val trips: List<Trip> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingRequests: Int = 0,
    val notification: String? = null
)
