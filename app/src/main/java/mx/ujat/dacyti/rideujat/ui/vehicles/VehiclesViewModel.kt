package mx.ujat.dacyti.rideujat.ui.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.ujat.dacyti.rideujat.core.supabase
import mx.ujat.dacyti.rideujat.data.model.Vehicle
import mx.ujat.dacyti.rideujat.data.repository.VehicleRepository

class VehiclesViewModel : ViewModel() {

    private val repository = VehicleRepository()

    private val _uiState = MutableStateFlow(VehiclesUiState())
    val uiState: StateFlow<VehiclesUiState> = _uiState.asStateFlow()

    init { loadVehicles() }

    fun loadVehicles() {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.loadVehicles(userId).fold(
                onSuccess = { list -> _uiState.update { it.copy(isLoading = false, vehicles = list) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun addVehicle(marca: String, modelo: String, color: String, placas: String) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            repository.addVehicle(userId, marca, modelo, color, placas).fold(
                onSuccess = { loadVehicles() },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun updateVehicle(vehicleId: String, marca: String, modelo: String, color: String, placas: String) {
        viewModelScope.launch {
            repository.updateVehicle(vehicleId, marca, modelo, color, placas).fold(
                onSuccess = { loadVehicles() },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun deleteVehicle(vehicleId: String) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicleId).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(vehicles = state.vehicles.filter { it.id != vehicleId })
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}

data class VehiclesUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
