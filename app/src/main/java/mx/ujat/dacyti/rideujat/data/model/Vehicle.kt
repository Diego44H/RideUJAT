package mx.ujat.dacyti.rideujat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VehicleUpdate(
    val marca: String,
    val modelo: String,
    val color: String,
    val placas: String
)
@Serializable
data class VehicleInsert(
    val user_id: String,
    val marca: String,
    val modelo: String,
    val color: String,
    val placas: String
)

@Serializable
data class Vehicle(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String = "",
    val marca: String = "",
    val modelo: String = "",
    val color: String = "",
    val placas: String = "",
    val activo: Boolean = true
)
