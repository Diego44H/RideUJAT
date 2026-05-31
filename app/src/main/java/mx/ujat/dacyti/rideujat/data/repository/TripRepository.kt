package mx.ujat.dacyti.rideujat.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import mx.ujat.dacyti.rideujat.core.supabase
import mx.ujat.dacyti.rideujat.data.model.RequestEstado
import mx.ujat.dacyti.rideujat.data.model.Trip
import mx.ujat.dacyti.rideujat.data.model.TripDataUpdate
import mx.ujat.dacyti.rideujat.data.model.TripEstado
import mx.ujat.dacyti.rideujat.data.model.TripRequest
import mx.ujat.dacyti.rideujat.data.model.TripRequestStatusUpdate
import mx.ujat.dacyti.rideujat.data.model.TripStatusUpdate

class TripRepository {

    suspend fun publishTrip(trip: Trip): Result<Unit> {
        return try {
            supabase.from("trips").insert(value = trip)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(exception = e)
        }
    }

    suspend fun loadTrip(tripId: String): Result<Trip> {
        return try {
            val trip = supabase.postgrest["trips"].select {
                filter { eq("id", tripId) }
            }.decodeSingle<Trip>()
            Result.success(trip)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadMyTrips(conductorId: String): Result<List<Trip>> {
        return try {
            val trips = supabase.postgrest["trips"].select {
                filter { eq("conductor_id", conductorId) }
            }.decodeList<Trip>()
            Result.success(trips.sortedByDescending { "${it.fechaSalida}${it.horaSalida}" })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTrip(tripId: String, update: TripDataUpdate): Result<Unit> {
        return try {
            supabase.postgrest["trips"].update(update) {
                filter { eq("id", tripId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelTrip(tripId: String): Result<Unit> {
        return try {
            supabase.postgrest["trips"].update(TripStatusUpdate(TripEstado.CANCELADO)) {
                filter { eq("id", tripId) }
            }
            supabase.postgrest["trip_requests"].update(
                TripRequestStatusUpdate(RequestEstado.CANCELADO_CONDUCTOR)
            ) { filter { eq("trip_id", tripId); eq("estado", RequestEstado.PENDIENTE) } }
            supabase.postgrest["trip_requests"].update(
                TripRequestStatusUpdate(RequestEstado.CANCELADO_CONDUCTOR)
            ) { filter { eq("trip_id", tripId); eq("estado", RequestEstado.ACEPTADO) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasAcceptedPassengers(tripId: String): Boolean {
        return try {
            supabase.postgrest["trip_requests"].select {
                filter { eq("trip_id", tripId); eq("estado", RequestEstado.ACEPTADO) }
            }.decodeList<TripRequest>().isNotEmpty()
        } catch (_: Exception) { false }
    }
}
