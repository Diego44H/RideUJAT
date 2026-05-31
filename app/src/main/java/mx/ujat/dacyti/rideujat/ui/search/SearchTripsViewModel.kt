package mx.ujat.dacyti.rideujat.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.ujat.dacyti.rideujat.data.model.TripWithDetails
import mx.ujat.dacyti.rideujat.data.repository.SearchTripRepository

class SearchTripsViewModel : ViewModel() {

    private val repository = SearchTripRepository()

    private val _uiState = MutableStateFlow(SearchTripsUiState())
    val uiState: StateFlow<SearchTripsUiState> = _uiState.asStateFlow()

    init { loadTrips() }

    fun loadTrips() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.searchTrips("").fold(
                onSuccess = { trips ->
                    _uiState.update { it.copy(isLoading = false, allTrips = trips, filteredTrips = trips) }
                },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun onQueryChange(query: String) {
        val filtered = if (query.isBlank()) _uiState.value.allTrips
        else _uiState.value.allTrips.filter { d ->
            d.trip.origen.contains(query, ignoreCase = true) ||
            d.trip.destino.contains(query, ignoreCase = true) ||
            d.conductor?.nombre?.contains(query, ignoreCase = true) == true
        }
        _uiState.update { it.copy(query = query, filteredTrips = filtered) }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}

data class SearchTripsUiState(
    val allTrips: List<TripWithDetails> = emptyList(),
    val filteredTrips: List<TripWithDetails> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
