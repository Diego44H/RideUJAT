package mx.ujat.dacyti.rideujat.data.repository

import io.github.jan.supabase.postgrest.postgrest
import mx.ujat.dacyti.rideujat.core.supabase
import mx.ujat.dacyti.rideujat.data.model.Profile
import mx.ujat.dacyti.rideujat.data.model.Rating
import mx.ujat.dacyti.rideujat.data.model.RequestEstado
import mx.ujat.dacyti.rideujat.data.model.Trip
import mx.ujat.dacyti.rideujat.data.model.TripRequest
import mx.ujat.dacyti.rideujat.data.model.ViajesCountUpdate

class RatingRepository {

    suspend fun getUsersToRate(
        tripId: String,
        currentUserId: String,
        isConductor: Boolean
    ): Result<List<Profile>> {
        return try {
            if (isConductor) {
                val requests = supabase.postgrest["trip_requests"].select {
                    filter { eq("trip_id", tripId); eq("estado", RequestEstado.ACEPTADO) }
                }.decodeList<TripRequest>()

                val passengers = requests
                    .filter { it.pasajeroId != currentUserId }
                    .map { req ->
                        runCatching {
                            supabase.postgrest["users"].select {
                                filter { eq("id", req.pasajeroId) }
                            }.decodeList<Profile>().firstOrNull()
                        }.getOrNull() ?: Profile(id = req.pasajeroId, nombre = "Pasajero")
                    }

                Result.success(passengers)
            } else {
                val trip = supabase.postgrest["trips"].select {
                    filter { eq("id", tripId) }
                }.decodeSingle<Trip>()

                val conductor = runCatching {
                    supabase.postgrest["users"].select {
                        filter { eq("id", trip.conductorId) }
                    }.decodeList<Profile>().firstOrNull()
                }.getOrNull() ?: Profile(id = trip.conductorId, nombre = "Conductor")

                Result.success(listOf(conductor))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitRating(
        tripId: String,
        evaluadorId: String,
        evaluadoId: String,
        estrellas: Int,
        comentario: String?
    ): Result<Unit> {
        return try {
            supabase.postgrest["ratings"].insert(
                Rating(
                    tripId = tripId,
                    evaluadorId = evaluadorId,
                    evaluadoId = evaluadoId,
                    estrellas = estrellas,
                    comentario = comentario?.ifBlank { null }
                )
            )
            // El trigger `trigger_recalcular_rating` en Supabase actualiza rating_promedio automáticamente
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun incrementViajesCount(userId: String) {
        try {
            val profile = supabase.postgrest["users"].select {
                filter { eq("id", userId) }
            }.decodeList<Profile>().firstOrNull() ?: return
            supabase.postgrest["users"].update(ViajesCountUpdate(profile.viajesCount + 1)) {
                filter { eq("id", userId) }
            }
        } catch (_: Exception) { /* no crítico */ }
    }
}
