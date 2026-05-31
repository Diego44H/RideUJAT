package mx.ujat.dacyti.rideujat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Rating(
    val id: String? = null,
    @SerialName("trip_id")
    val tripId: String = "",
    @SerialName("evaluador_id")
    val evaluadorId: String = "",
    @SerialName("evaluado_id")
    val evaluadoId: String = "",
    val estrellas: Int = 5,
    val comentario: String? = null
)
