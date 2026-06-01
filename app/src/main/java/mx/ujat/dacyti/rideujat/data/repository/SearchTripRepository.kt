package mx.ujat.dacyti.rideujat.data.repository

import io.github.jan.supabase.postgrest.postgrest
import mx.ujat.dacyti.rideujat.core.supabase
import mx.ujat.dacyti.rideujat.data.model.Profile
import mx.ujat.dacyti.rideujat.data.model.Trip
import mx.ujat.dacyti.rideujat.data.model.TripEstado
import mx.ujat.dacyti.rideujat.data.model.TripRequest
import mx.ujat.dacyti.rideujat.data.model.TripWithDetails
import mx.ujat.dacyti.rideujat.data.model.Vehicle

class SearchTripRepository {

    suspend fun searchTrips(query: String, excludeUserId: String? = null): Result<List<TripWithDetails>> {
        return try {
            val trips = supabase.postgrest["trips"].select {
                filter {
                    eq("estado", TripEstado.PUBLICADO)
                    gt("asientos_disponibles", 0)
                }
            }.decodeList<Trip>()

            if (trips.isEmpty()) return Result.success(emptyList())

            val conductors = trips.map { it.conductorId }.distinct().mapNotNull { id ->
                runCatching {
                    supabase.postgrest["users"].select {
                        filter { eq("id", id) }
                    }.decodeSingle<Profile>()
                }.getOrNull()
            }.associateBy { it.id }

            val vehicles = trips.map { it.vehiculoId }.distinct().mapNotNull { id ->
                runCatching {
                    supabase.postgrest["vehicles"].select {
                        filter { eq("id", id) }
                    }.decodeSingle<Vehicle>()
                }.getOrNull()
            }.associateBy { it.id }

            val result = trips.map { trip ->
                TripWithDetails(trip, conductors[trip.conductorId], vehicles[trip.vehiculoId])
            }.filter { d ->
                excludeUserId == null || d.trip.conductorId != excludeUserId
            }.let { list ->
                if (query.isBlank()) list
                else list.filter { d ->
                    d.trip.origen.contains(query, ignoreCase = true) ||
                    d.trip.destino.contains(query, ignoreCase = true) ||
                    d.conductor?.nombre?.contains(query, ignoreCase = true) == true
                }
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadTripWithDetails(tripId: String): Result<TripWithDetails> {
        return try {
            val trip = supabase.postgrest["trips"].select {
                filter { eq("id", tripId) }
            }.decodeSingle<Trip>()
            val conductor = runCatching {
                supabase.postgrest["users"].select { filter { eq("id", trip.conductorId) } }.decodeSingle<Profile>()
            }.getOrNull()
            val vehicle = runCatching {
                supabase.postgrest["vehicles"].select { filter { eq("id", trip.vehiculoId) } }.decodeSingle<Vehicle>()
            }.getOrNull()
            Result.success(TripWithDetails(trip, conductor, vehicle))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestTrip(tripId: String, pasajeroId: String): Result<Unit> {
        return try {
            supabase.postgrest["trip_requests"].insert(
                mapOf(
                    "trip_id" to tripId,
                    "pasajero_id" to pasajeroId,
                    "estado" to "pendiente"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = if (e.message?.contains("unique", ignoreCase = true) == true)
                "Ya tienes una solicitud para este viaje" else e.message ?: "Error al solicitar"
            Result.failure(Exception(msg))
        }
    }

    suspend fun getMyRequest(tripId: String, pasajeroId: String): Result<TripRequest?> {
        return try {
            val list = supabase.postgrest["trip_requests"].select {
                filter { eq("trip_id", tripId); eq("pasajero_id", pasajeroId) }
            }.decodeList<TripRequest>()
            Result.success(list.firstOrNull())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelMyRequest(requestId: String): Result<Unit> {
        return try {
            supabase.postgrest["trip_requests"].update(
                TripRequest(estado = "cancelado_pasajero")
            ) { filter { eq("id", requestId) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
