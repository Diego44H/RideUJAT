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
import mx.ujat.dacyti.rideujat.data.model.TripDataUpdate
import mx.ujat.dacyti.rideujat.data.model.Vehicle
import mx.ujat.dacyti.rideujat.data.repository.TripRepository
import mx.ujat.dacyti.rideujat.data.repository.VehicleRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PublishTripViewModel : ViewModel() {

    private val vehicleRepo = VehicleRepository()
    private val tripRepo = TripRepository()

    private val _uiState = MutableStateFlow(PublishTripUiState())
    val uiState: StateFlow<PublishTripUiState> = _uiState.asStateFlow()

    init { loadVehicles() }

    private fun loadVehicles() {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            vehicleRepo.loadVehicles(userId).onSuccess { vehicles ->
                _uiState.update { it.copy(vehicles = vehicles) }
            }
        }
    }

    fun loadTripForEdit(tripId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val hasAccepted = tripRepo.hasAcceptedPassengers(tripId)
            tripRepo.loadTrip(tripId).fold(
                onSuccess = { trip ->
                    val fechaMillis = runCatching {
                        LocalDate.parse(trip.fechaSalida, DateTimeFormatter.ISO_LOCAL_DATE)
                            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                    }.getOrNull()
                    val selectedVehicle = _uiState.value.vehicles.find { it.id == trip.vehiculoId }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            editingTripId = tripId,
                            hasAcceptedPassengers = hasAccepted,
                            selectedVehicle = selectedVehicle,
                            origen = trip.origen,
                            destino = trip.destino,
                            fechaMillis = fechaMillis,
                            hora = trip.horaSalida,
                            asientos = trip.asientosTotal,
                            tarifa = trip.tarifa.toString(),
                            tiempoEstimado = trip.tiempoEstimado ?: ""
                        )
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun selectVehicle(vehicle: Vehicle) { _uiState.update { it.copy(selectedVehicle = vehicle) } }
    fun setOrigen(v: String) { _uiState.update { it.copy(origen = v) } }
    fun setDestino(v: String) { _uiState.update { it.copy(destino = v) } }
    fun setFechaMillis(millis: Long) { _uiState.update { it.copy(fechaMillis = millis) } }
    fun setHora(hour: Int, minute: Int) { _uiState.update { it.copy(hora = "%02d:%02d:00".format(hour, minute)) } }
    fun incrementAsientos() { _uiState.update { it.copy(asientos = (it.asientos + 1).coerceAtMost(7)) } }
    fun decrementAsientos() { _uiState.update { it.copy(asientos = (it.asientos - 1).coerceAtLeast(1)) } }
    fun setTarifa(v: String) { _uiState.update { it.copy(tarifa = v) } }
    fun setTiempoEstimado(v: String) { _uiState.update { it.copy(tiempoEstimado = v) } }

    fun submitTrip() {
        val state = _uiState.value
        val error = when {
            state.selectedVehicle == null -> "Selecciona un vehículo"
            state.origen.isBlank() -> "El origen es requerido"
            state.fechaMillis == null -> "Selecciona la fecha de salida"
            state.hora.isBlank() -> "Selecciona la hora de salida"
            state.tarifa.toDoubleOrNull() == null -> "Ingresa una tarifa válida"
            !isValidFutureDateTime(state.fechaMillis!!, state.hora) -> "La fecha y hora deben estar en el futuro"
            else -> null
        }
        if (error != null) { _uiState.update { it.copy(error = error) }; return }

        val fecha = Instant.ofEpochMilli(state.fechaMillis!!)
            .atZone(ZoneId.of("UTC")).toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            if (state.editingTripId != null) {
                tripRepo.updateTrip(
                    state.editingTripId,
                    TripDataUpdate(
                        origen = state.origen,
                        destino = state.destino,
                        fechaSalida = fecha,
                        horaSalida = state.hora,
                        asientosTotal = state.asientos,
                        asientosDisponibles = state.asientos,
                        tarifa = state.tarifa.toDouble(),
                        tiempoEstimado = state.tiempoEstimado.ifBlank { null }
                    )
                ).fold(
                    onSuccess = { _uiState.update { it.copy(isLoading = false, success = true) } },
                    onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                )
            } else {
                val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                tripRepo.publishTrip(
                    Trip(
                        conductorId = userId,
                        vehiculoId = state.selectedVehicle!!.id!!,
                        origen = state.origen,
                        destino = state.destino,
                        fechaSalida = fecha,
                        horaSalida = state.hora,
                        asientosTotal = state.asientos,
                        asientosDisponibles = state.asientos,
                        tarifa = state.tarifa.toDouble(),
                        tiempoEstimado = state.tiempoEstimado.ifBlank { null }
                    )
                ).fold(
                    onSuccess = { _uiState.update { it.copy(isLoading = false, success = true) } },
                    onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                )
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun resetSuccess() { _uiState.update { it.copy(success = false) } }

    private fun isValidFutureDateTime(dateMillis: Long, hourStr: String): Boolean {
        return try {
            val hour = hourStr.take(2).toInt()
            val minute = hourStr.drop(3).take(2).toInt()
            val selectedDate = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.of("UTC")).toLocalDate()
            val selectedDateTime = LocalDateTime.of(selectedDate, LocalTime.of(hour, minute))
            val now = LocalDateTime.now(ZoneId.of("UTC"))
            selectedDateTime.isAfter(now)
        } catch (e: Exception) {
            false
        }
    }
}

data class PublishTripUiState(
    val editingTripId: String? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicle: Vehicle? = null,
    val hasAcceptedPassengers: Boolean = false,
    val origen: String = "",
    val destino: String = "",
    val fechaMillis: Long? = null,
    val hora: String = "",
    val asientos: Int = 1,
    val tarifa: String = "",
    val tiempoEstimado: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
) {
    val isEditMode: Boolean get() = editingTripId != null
}
