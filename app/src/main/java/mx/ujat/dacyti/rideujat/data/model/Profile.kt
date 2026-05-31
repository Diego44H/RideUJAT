package mx.ujat.dacyti.rideujat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileFotoUpdate(@SerialName("foto_url") val fotoUrl: String)

@Serializable
data class RatingAverageUpdate(@SerialName("rating_promedio") val ratingPromedio: Double)

@Serializable
data class ViajesCountUpdate(@SerialName("viajes_count") val viajesCount: Int)

@Serializable
data class Profile(
    val id: String? = null,
    val nombre: String = "",
    val matricula: String = "",
    val correo: String = "",
    @SerialName("foto_url")
    val fotoUrl: String? = null,
    @SerialName("rating_promedio")
    val ratingPromedio: Double = 5.0,
    @SerialName("viajes_count")
    val viajesCount: Int = 0
)
