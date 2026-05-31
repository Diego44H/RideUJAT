package mx.ujat.dacyti.rideujat.data.repository

import io.github.jan.supabase.postgrest.postgrest
import mx.ujat.dacyti.rideujat.core.supabase
import mx.ujat.dacyti.rideujat.data.model.Message
import mx.ujat.dacyti.rideujat.data.model.Profile

class ChatRepository {

    suspend fun loadMessages(tripId: String): Result<List<Message>> {
        return try {
            val messages = supabase.postgrest["messages"].select {
                filter { eq("trip_id", tripId) }
            }.decodeList<Message>()
            Result.success(messages.sortedBy { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(tripId: String, senderId: String, texto: String): Result<Unit> {
        return try {
            supabase.postgrest["messages"].insert(
                mapOf(
                    "trip_id" to tripId,
                    "sender_id" to senderId,
                    "texto" to texto
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSenderNames(senderIds: List<String>): Map<String, String> {
        return senderIds.distinct().mapNotNull { id ->
            runCatching {
                supabase.postgrest["users"].select {
                    filter { eq("id", id) }
                }.decodeSingle<Profile>()
            }.getOrNull()
        }.associateBy({ it.id ?: "" }, { it.nombre })
    }
}
