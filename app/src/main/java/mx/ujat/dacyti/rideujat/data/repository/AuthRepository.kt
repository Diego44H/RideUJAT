package mx.ujat.dacyti.rideujat.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import mx.ujat.dacyti.rideujat.core.supabase
import mx.ujat.dacyti.rideujat.data.model.Profile

class AuthRepository {

    suspend fun login(correo: String, contrasena: String): Result<Unit> {
        return try {
            supabase.auth.signInWith(Email) {
                email = correo
                password = contrasena
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        nombre: String,
        matricula: String,
        correo: String,
        contrasena: String
    ): Result<Unit> {
        return try {
            supabase.auth.signUpWith(Email) {
                email = correo
                password = contrasena
            }
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Error al crear la sesión. Verifica tu correo."))

            supabase.postgrest["users"].insert(
                mapOf(
                    "id" to userId,
                    "nombre" to nombre,
                    "matricula" to matricula,
                    "correo" to correo,
                    "rating_promedio" to 5.0,
                    "viajes_count" to 0
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isLoggedIn(): Boolean = supabase.auth.currentUserOrNull() != null
}
