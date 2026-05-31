package mx.ujat.dacyti.rideujat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TripRequest(
    val id: String? = null,
    @SerialName("trip_id")
    val tripId: String = "",
    @SerialName("pasajero_id")
    val pasajeroId: String = "",
    val estado: String = "pendiente"
)

@Serializable
data class TripRequestStatusUpdate(val estado: String)

object RequestEstado {
    const val PENDIENTE = "pendiente"
    const val ACEPTADO = "aceptado"
    const val RECHAZADO = "rechazado"
    const val CANCELADO_PASAJERO = "cancelado_pasajero"
    const val CANCELADO_CONDUCTOR = "cancelado_conductor"
}
