package mx.ujat.dacyti.rideujat.data.repository

import io.github.jan.supabase.postgrest.postgrest
import mx.ujat.dacyti.rideujat.core.supabase
import mx.ujat.dacyti.rideujat.data.model.Profile
import mx.ujat.dacyti.rideujat.data.model.RequestEstado
import mx.ujat.dacyti.rideujat.data.model.RequestWithPassenger
import mx.ujat.dacyti.rideujat.data.model.Trip
import mx.ujat.dacyti.rideujat.data.model.TripRequest
import mx.ujat.dacyti.rideujat.data.model.TripRequestStatusUpdate
import mx.ujat.dacyti.rideujat.data.model.TripSeatsUpdate

class ManageRequestsRepository {

    suspend fun loadRequests(tripId: String): Result<List<RequestWithPassenger>> {
        return try {
            val requests = supabase.postgrest["trip_requests"].select {
                filter { eq("trip_id", tripId) }
            }.decodeList<TripRequest>()

            val pasajeros = requests.map { it.pasajeroId }.distinct().mapNotNull { id ->
                runCatching {
                    supabase.postgrest["users"].select { filter { eq("id", id) } }.decodeSingle<Profile>()
                }.getOrNull()
            }.associateBy { it.id }

            Result.success(requests.map { req ->
                RequestWithPassenger(req, pasajeros[req.pasajeroId])
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptRequest(requestId: String, tripId: String): Result<Unit> {
        return try {
            supabase.postgrest["trip_requests"].update(
                TripRequestStatusUpdate(RequestEstado.ACEPTADO)
            ) { filter { eq("id", requestId) } }

            val trip = supabase.postgrest["trips"].select {
                filter { eq("id", tripId) }
            }.decodeSingle<Trip>()

            supabase.postgrest["trips"].update(
                TripSeatsUpdate(maxOf(0, trip.asientosDisponibles - 1))
            ) { filter { eq("id", tripId) } }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectRequest(requestId: String): Result<Unit> {
        return try {
            supabase.postgrest["trip_requests"].update(
                TripRequestStatusUpdate(RequestEstado.RECHAZADO)
            ) { filter { eq("id", requestId) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
