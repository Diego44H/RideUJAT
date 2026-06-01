package mx.ujat.dacyti.rideujat.ui.active

import android.annotation.SuppressLint
import android.app.Application
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.ujat.dacyti.rideujat.data.model.TripEstado
import mx.ujat.dacyti.rideujat.data.model.TripWithDetails
import mx.ujat.dacyti.rideujat.data.repository.ActiveTripRepository

class ActiveTripViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ActiveTripRepository()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _uiState = MutableStateFlow(ActiveTripUiState())
    val uiState: StateFlow<ActiveTripUiState> = _uiState.asStateFlow()

    private var currentTripId = ""
    private var pollingJob: Job? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                _uiState.update { it.copy(conductorLat = loc.latitude, conductorLng = loc.longitude) }
                viewModelScope.launch {
                    repository.updateConductorLocation(currentTripId, loc.latitude, loc.longitude)
                }
            }
        }
    }

    fun initialize(tripId: String, isConductor: Boolean) {
        currentTripId = tripId
        viewModelScope.launch {
            _uiState.update { it.copy(isConductor = isConductor, isLoading = true) }
            repository.loadActiveTrip(tripId).fold(
                onSuccess = { details ->
                    _uiState.update { it.copy(isLoading = false, tripDetails = details) }
                    // Set initial location if available
                    details.trip.latConductor?.let { lat ->
                        details.trip.lngConductor?.let { lng ->
                            _uiState.update { it.copy(conductorLat = lat, conductorLng = lng) }
                        }
                    }
                    if (!isConductor) startPassengerPolling(tripId)
                },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startConductorTracking() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(3_000L)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    fun stopConductorTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun startPassengerPolling(tripId: String) {
        pollingJob = viewModelScope.launch {
            while (true) {
                repository.getConductorLocation(tripId).getOrNull()?.let { (lat, lng) ->
                    _uiState.update { it.copy(conductorLat = lat, conductorLng = lng) }
                }
                repository.loadActiveTrip(tripId).onSuccess { details ->
                    _uiState.update { it.copy(tripDetails = details) }
                    if (details.trip.estado == TripEstado.FINALIZADO) {
                        _uiState.update { it.copy(tripFinished = true) }
                        pollingJob?.cancel()
                        return@launch
                    }
                }
                delay(5_000L)
            }
        }
    }

    fun startTrip() {
        viewModelScope.launch {
            repository.startTrip(currentTripId).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(tripDetails = state.tripDetails?.let { d ->
                            d.copy(trip = d.trip.copy(estado = TripEstado.EN_CURSO))
                        })
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun finishTrip() {
        stopConductorTracking()
        viewModelScope.launch {
            repository.finishTrip(currentTripId).fold(
                onSuccess = { _uiState.update { it.copy(tripFinished = true) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }

    override fun onCleared() {
        super.onCleared()
        stopConductorTracking()
        pollingJob?.cancel()
    }
}

data class ActiveTripUiState(
    val tripDetails: TripWithDetails? = null,
    val isConductor: Boolean = false,
    val conductorLat: Double? = null,
    val conductorLng: Double? = null,
    val isLoading: Boolean = false,
    val tripFinished: Boolean = false,
    val error: String? = null
)
