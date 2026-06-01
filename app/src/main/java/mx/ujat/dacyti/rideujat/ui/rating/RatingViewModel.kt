package mx.ujat.dacyti.rideujat.ui.rating

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.ujat.dacyti.rideujat.core.supabase
import mx.ujat.dacyti.rideujat.data.model.Profile
import mx.ujat.dacyti.rideujat.data.repository.RatingRepository

class RatingViewModel : ViewModel() {

    private val repository = RatingRepository()

    private val _uiState = MutableStateFlow(RatingUiState())
    val uiState: StateFlow<RatingUiState> = _uiState.asStateFlow()

    fun initialize(tripId: String, isConductor: Boolean) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        _uiState.update { it.copy(currentUserId = userId) }
        loadUsersToRate(tripId, userId, isConductor)
    }

    private fun loadUsersToRate(tripId: String, userId: String, isConductor: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getUsersToRate(tripId, userId, isConductor).fold(
                onSuccess = { users ->
                    _uiState.update { it.copy(isLoading = false, usersToRate = users, currentUserIndex = 0) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun setCurrentRating(rating: Int) { _uiState.update { it.copy(currentRating = rating) } }

    fun setCurrentComment(comment: String) { _uiState.update { it.copy(currentComment = comment) } }

    fun submitRating(tripId: String) {
        val state = _uiState.value
        val user = state.currentUser ?: return
        if (state.currentRating <= 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            repository.submitRating(
                tripId = tripId,
                evaluadorId = state.currentUserId,
                evaluadoId = user.id ?: "",
                estrellas = state.currentRating,
                comentario = state.currentComment.ifBlank { null }
            ).fold(
                onSuccess = {
                    val nextIndex = state.currentUserIndex + 1
                    if (nextIndex < state.usersToRate.size) {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                currentUserIndex = nextIndex,
                                currentRating = 5,
                                currentComment = ""
                            )
                        }
                    } else {
                        // Incrementar viajes una sola vez al terminar todas las calificaciones
                        repository.incrementViajesCount(state.currentUserId)
                        _uiState.update { it.copy(isSubmitting = false, ratingComplete = true) }
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSubmitting = false, error = "No se pudo enviar la calificación. Intenta de nuevo.") }
                }
            )
        }
    }

    fun skipUser() {
        val state = _uiState.value
        val nextIndex = state.currentUserIndex + 1
        if (nextIndex < state.usersToRate.size) {
            _uiState.update {
                it.copy(currentUserIndex = nextIndex, currentRating = 5, currentComment = "")
            }
        } else {
            _uiState.update { it.copy(ratingComplete = true) }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}

data class RatingUiState(
    val currentUserId: String = "",
    val usersToRate: List<Profile> = emptyList(),
    val currentUserIndex: Int = 0,
    val currentRating: Int = 5,
    val currentComment: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val ratingComplete: Boolean = false
) {
    val currentUser: Profile? = if (currentUserIndex < usersToRate.size) usersToRate[currentUserIndex] else null
    val progress: Float = if (usersToRate.isEmpty()) 0f else (currentUserIndex + 1f) / usersToRate.size
}
