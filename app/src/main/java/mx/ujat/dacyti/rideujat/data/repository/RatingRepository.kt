package mx.ujat.dacyti.rideujat.data.repository

import io.github.jan.supabase.postgrest.postgrest
import mx.ujat.dacyti.rideujat.core.supabase
import mx.ujat.dacyti.rideujat.data.model.Profile
import mx.ujat.dacyti.rideujat.data.model.Rating
import mx.ujat.dacyti.rideujat.data.model.RatingAverageUpdate
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

                val passengers = requests.mapNotNull { req ->
                    runCatching {
                        supabase.postgrest["users"].select {
                            filter { eq("id", req.pasajeroId) }
                        }.decodeSingle<Profile>()
                    }.getOrNull()
                }.filter { it.id != currentUserId }

                Result.success(passengers)
            } else {
                val trip = supabase.postgrest["trips"].select {
                    filter { eq("id", tripId) }
                }.decodeSingle<Trip>()
                val conductor = supabase.postgrest["users"].select {
                    filter { eq("id", trip.conductorId) }
                }.decodeSingle<Profile>()
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
            recalcularPromedio(evaluadoId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun recalcularPromedio(userId: String) {
        try {
            val ratings = supabase.postgrest["ratings"].select {
                filter { eq("evaluado_id", userId) }
            }.decodeList<Rating>()
            if (ratings.isEmpty()) return
            val promedio = ratings.map { it.estrellas }.average()
            supabase.postgrest["users"].update(RatingAverageUpdate(promedio)) {
                filter { eq("id", userId) }
            }
        } catch (_: Exception) { /* no crítico */ }
    }

    suspend fun incrementViajesCount(userId: String) {
        try {
            val profile = supabase.postgrest["users"].select {
                filter { eq("id", userId) }
            }.decodeSingle<Profile>()
            supabase.postgrest["users"].update(ViajesCountUpdate(profile.viajesCount + 1)) {
                filter { eq("id", userId) }
            }
        } catch (_: Exception) { /* no crítico */ }
    }
}
