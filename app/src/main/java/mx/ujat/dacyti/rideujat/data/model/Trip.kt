package mx.ujat.dacyti.rideujat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Trip(
    val id: String? = null,
    @SerialName("conductor_id")
    val conductorId: String = "",
    @SerialName("vehiculo_id")
    val vehiculoId: String = "",
    val origen: String = "",
    val destino: String = "Campus DACyTI-UJAT",
    @SerialName("fecha_salida")
    val fechaSalida: String = "",
    @SerialName("hora_salida")
    val horaSalida: String = "",
    @SerialName("asientos_total")
    val asientosTotal: Int = 1,
    @SerialName("asientos_disponibles")
    val asientosDisponibles: Int = 1,
    val tarifa: Double = 0.0,
    @SerialName("tiempo_estimado")
    val tiempoEstimado: String? = null,
    val estado: String = "publicado",
    @SerialName("lat_conductor")
    val latConductor: Double? = null,
    @SerialName("lng_conductor")
    val lngConductor: Double? = null
)

@Serializable
data class TripStatusUpdate(val estado: String)

@Serializable
data class TripSeatsUpdate(@SerialName("asientos_disponibles") val asientosDisponibles: Int)

@Serializable
data class LocationUpdate(
    @SerialName("lat_conductor") val latConductor: Double,
    @SerialName("lng_conductor") val lngConductor: Double
)

@Serializable
data class TripDataUpdate(
    val origen: String,
    val destino: String,
    @SerialName("fecha_salida") val fechaSalida: String,
    @SerialName("hora_salida") val horaSalida: String,
    @SerialName("asientos_total") val asientosTotal: Int,
    @SerialName("asientos_disponibles") val asientosDisponibles: Int,
    val tarifa: Double,
    @SerialName("tiempo_estimado") val tiempoEstimado: String?
)

object TripEstado {
    const val PUBLICADO = "publicado"
    const val EN_CURSO = "en_curso"
    const val FINALIZADO = "finalizado"
    const val CANCELADO = "cancelado"
}
