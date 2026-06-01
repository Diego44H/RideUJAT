package mx.ujat.dacyti.rideujat.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.ujat.dacyti.rideujat.core.supabase
import mx.ujat.dacyti.rideujat.data.model.TripRequest
import mx.ujat.dacyti.rideujat.data.model.TripWithDetails
import mx.ujat.dacyti.rideujat.data.repository.SearchTripRepository

class TripDetailViewModel : ViewModel() {

    private val repository = SearchTripRepository()

    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    fun loadTrip(tripId: String) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.loadTripWithDetails(tripId).fold(
                onSuccess = { details ->
                    val isOwnTrip = details.trip.conductorId == userId
                    _uiState.update { it.copy(tripDetails = details, isOwnTrip = isOwnTrip) }
                },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
            repository.getMyRequest(tripId, userId).fold(
                onSuccess = { req -> _uiState.update { it.copy(isLoading = false, myRequest = req) } },
                onFailure = { _uiState.update { it.copy(isLoading = false) } }
            )
        }
    }

    fun clearNotification() { _uiState.update { it.copy(notification = null) } }

    fun requestTrip(tripId: String) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRequesting = true, error = null) }
            repository.requestTrip(tripId, userId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isRequesting = false) }
                    loadTrip(tripId)
                },
                onFailure = { e -> _uiState.update { it.copy(isRequesting = false, error = e.message) } }
            )
        }
    }

    fun cancelRequest(tripId: String) {
        val requestId = _uiState.value.myRequest?.id ?: return
        viewModelScope.launch {
            repository.cancelMyRequest(requestId).fold(
                onSuccess = { loadTrip(tripId) },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}

data class TripDetailUiState(
    val tripDetails: TripWithDetails? = null,
    val myRequest: TripRequest? = null,
    val isLoading: Boolean = false,
    val isRequesting: Boolean = false,
    val isOwnTrip: Boolean = false,
    val error: String? = null,
    val notification: String? = null
)
