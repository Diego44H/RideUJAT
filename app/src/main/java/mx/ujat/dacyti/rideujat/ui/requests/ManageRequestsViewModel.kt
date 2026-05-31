package mx.ujat.dacyti.rideujat.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.ujat.dacyti.rideujat.data.model.RequestWithPassenger
import mx.ujat.dacyti.rideujat.data.repository.ManageRequestsRepository

class ManageRequestsViewModel : ViewModel() {

    private val repository = ManageRequestsRepository()

    private val _uiState = MutableStateFlow(ManageRequestsUiState())
    val uiState: StateFlow<ManageRequestsUiState> = _uiState.asStateFlow()

    private var currentTripId: String = ""

    fun loadRequests(tripId: String) {
        currentTripId = tripId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.loadRequests(tripId).fold(
                onSuccess = { list -> _uiState.update { it.copy(isLoading = false, requests = list) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun acceptRequest(requestId: String) {
        viewModelScope.launch {
            repository.acceptRequest(requestId, currentTripId).fold(
                onSuccess = { loadRequests(currentTripId) },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            repository.rejectRequest(requestId).fold(
                onSuccess = { loadRequests(currentTripId) },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}

data class ManageRequestsUiState(
    val requests: List<RequestWithPassenger> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
