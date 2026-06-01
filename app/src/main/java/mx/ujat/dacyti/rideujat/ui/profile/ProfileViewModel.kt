package mx.ujat.dacyti.rideujat.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.ujat.dacyti.rideujat.core.supabase
import mx.ujat.dacyti.rideujat.data.model.Profile
import mx.ujat.dacyti.rideujat.data.model.SosContact
import mx.ujat.dacyti.rideujat.data.repository.ProfileRepository
import io.github.jan.supabase.auth.auth

class ProfileViewModel : ViewModel() {

    private val repository = ProfileRepository()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { loadData() }

    private fun loadData() {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val profileResult = repository.loadProfile(userId)
            val contactsResult = repository.loadSosContacts(userId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    profile = profileResult.getOrNull() ?: it.profile,
                    sosContacts = contactsResult.getOrElse { emptyList() },
                    error = profileResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun uploadAvatar(bytes: ByteArray) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true, error = null) }
            repository.uploadAvatar(bytes, userId).fold(
                onSuccess = { url ->
                    _uiState.update { state ->
                        state.copy(
                            isUploadingAvatar = false,
                            profile = state.profile?.copy(fotoUrl = url)
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isUploadingAvatar = false, error = e.message) }
                }
            )
        }
    }

    fun addSosContact(nombre: String, telefono: String) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            repository.addSosContact(userId, nombre, telefono).fold(
                onSuccess = { loadData() },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun deleteSosContact(contactId: String) {
        viewModelScope.launch {
            repository.deleteSosContact(contactId).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(sosContacts = state.sosContacts.filter { it.id != contactId })
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            supabase.auth.signOut()
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}

data class ProfileUiState(
    val profile: Profile? = null,
    val sosContacts: List<SosContact> = emptyList(),
    val isLoading: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val error: String? = null
)
