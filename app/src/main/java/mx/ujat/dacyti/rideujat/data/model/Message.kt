package mx.ujat.dacyti.rideujat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String? = null,
    @SerialName("trip_id")
    val tripId: String = "",
    @SerialName("sender_id")
    val senderId: String = "",
    val texto: String = "",
    @SerialName("created_at")
    val createdAt: String? = null
)
