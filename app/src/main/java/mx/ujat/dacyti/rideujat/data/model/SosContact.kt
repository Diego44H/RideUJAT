package mx.ujat.dacyti.rideujat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SosContact(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String = "",
    val nombre: String = "",
    val telefono: String = ""
)
