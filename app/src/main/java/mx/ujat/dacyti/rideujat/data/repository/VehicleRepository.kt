package mx.ujat.dacyti.rideujat.data.repository

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import mx.ujat.dacyti.rideujat.core.supabase
import mx.ujat.dacyti.rideujat.data.model.Trip
import mx.ujat.dacyti.rideujat.data.model.TripEstado
import mx.ujat.dacyti.rideujat.data.model.Vehicle
import mx.ujat.dacyti.rideujat.data.model.VehicleInsert
import mx.ujat.dacyti.rideujat.data.model.VehicleUpdate

class VehicleRepository {

    suspend fun loadVehicles(userId: String): Result<List<Vehicle>> {
        return try {
            val vehicles = supabase.postgrest["vehicles"].select {
                filter { eq("user_id", userId) }
            }.decodeList<Vehicle>()
            Result.success(vehicles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addVehicle(
        userId: String,
        marca: String,
        modelo: String,
        color: String,
        placas: String
    ): Result<Unit> {
        return try {
            supabase.from("vehicles").insert(
                VehicleInsert(
                    user_id = userId,
                    marca = marca,
                    modelo = modelo,
                    color = color,
                    placas = placas
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = if (e.message?.contains("unique", ignoreCase = true) == true)
                "Esas placas ya están registradas" else e.message ?: "Error al agregar vehículo"
            Result.failure(Exception(msg))
        }
    }

    suspend fun updateVehicle(
        vehicleId: String,
        marca: String,
        modelo: String,
        color: String,
        placas: String
    ): Result<Unit> {
        return try {
            supabase.postgrest["vehicles"].update(
                VehicleUpdate(marca = marca, modelo = modelo, color = color, placas = placas)
            ) {
                filter { eq("id", vehicleId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = if (e.message?.contains("unique", ignoreCase = true) == true)
                "Esas placas ya están registradas" else e.message ?: "Error al actualizar vehículo"
            Result.failure(Exception(msg))
        }
    }

    suspend fun deleteVehicle(vehicleId: String): Result<Unit> {
        return try {
            // No eliminar si tiene viaje publicado o en curso
            val activeTrips = supabase.postgrest["trips"].select {
                filter {
                    eq("vehiculo_id", vehicleId)
                    neq("estado", TripEstado.FINALIZADO)
                    neq("estado", TripEstado.CANCELADO)
                }
            }.decodeList<Trip>()

            if (activeTrips.isNotEmpty()) {
                return Result.failure(Exception("No puedes eliminar un vehículo con viaje activo o publicado"))
            }

            supabase.postgrest["vehicles"].delete {
                filter { eq("id", vehicleId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
