package mx.ujat.dacyti.rideujat.data.repository

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import mx.ujat.dacyti.rideujat.core.supabase
import mx.ujat.dacyti.rideujat.data.model.Profile
import mx.ujat.dacyti.rideujat.data.model.ProfileFotoUpdate
import mx.ujat.dacyti.rideujat.data.model.SosContact

class ProfileRepository {

    suspend fun loadProfile(userId: String): Result<Profile> {
        return try {
            val profile = supabase.postgrest["users"].select {
                filter { eq("id", userId) }
            }.decodeSingle<Profile>()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadSosContacts(userId: String): Result<List<SosContact>> {
        return try {
            val contacts = supabase.postgrest["sos_contacts"].select {
                filter { eq("user_id", userId) }
            }.decodeList<SosContact>()
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAvatar(bytes: ByteArray, userId: String): Result<String> {
        return try {
            val bucket = supabase.storage.from("avatars")
            bucket.upload("$userId/avatar.jpg", bytes) { upsert = true }
            val publicUrl = bucket.publicUrl("$userId/avatar.jpg")
            supabase.postgrest["users"].update(ProfileFotoUpdate(publicUrl)) {
                filter { eq("id", userId) }
            }
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addSosContact(userId: String, nombre: String, telefono: String): Result<Unit> {
        return try {
            val count = supabase.postgrest["sos_contacts"].select {
                filter { eq("user_id", userId) }
            }.decodeList<SosContact>().size
            if (count >= 3) return Result.failure(Exception("Máximo 3 contactos SOS permitidos"))
            supabase.postgrest["sos_contacts"].insert(
                mapOf(
                    "user_id" to userId,
                    "nombre" to nombre,
                    "telefono" to telefono
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSosContact(contactId: String): Result<Unit> {
        return try {
            supabase.postgrest["sos_contacts"].delete {
                filter { eq("id", contactId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
