package mx.ujat.dacyti.rideujat.data.repository

import io.github.jan.supabase.postgrest.postgrest
import mx.ujat.dacyti.rideujat.core.supabase
import mx.ujat.dacyti.rideujat.data.model.LocationUpdate
import mx.ujat.dacyti.rideujat.data.model.Trip
import mx.ujat.dacyti.rideujat.data.model.TripEstado
import mx.ujat.dacyti.rideujat.data.model.TripStatusUpdate
import mx.ujat.dacyti.rideujat.data.model.TripWithDetails

class ActiveTripRepository {

    private val searchRepo = SearchTripRepository()

    suspend fun loadActiveTrip(tripId: String): Result<TripWithDetails> =
        searchRepo.loadTripWithDetails(tripId)

    suspend fun startTrip(tripId: String): Result<Unit> {
        return try {
            supabase.postgrest["trips"].update(TripStatusUpdate(TripEstado.EN_CURSO)) {
                filter { eq("id", tripId) }
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun finishTrip(tripId: String): Result<Unit> {
        return try {
            supabase.postgrest["trips"].update(TripStatusUpdate(TripEstado.FINALIZADO)) {
                filter { eq("id", tripId) }
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateConductorLocation(tripId: String, lat: Double, lng: Double): Result<Unit> {
        return try {
            supabase.postgrest["trips"].update(LocationUpdate(lat, lng)) {
                filter { eq("id", tripId) }
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getConductorLocation(tripId: String): Result<Pair<Double, Double>?> {
        return try {
            val trip = supabase.postgrest["trips"].select {
                filter { eq("id", tripId) }
            }.decodeSingle<Trip>()
            val pair = if (trip.latConductor != null && trip.lngConductor != null)
                Pair(trip.latConductor, trip.lngConductor) else null
            Result.success(pair)
        } catch (e: Exception) { Result.failure(e) }
    }
}
