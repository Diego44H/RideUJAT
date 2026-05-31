package mx.ujat.dacyti.rideujat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.ujat.dacyti.rideujat.core.supabase
import mx.ujat.dacyti.rideujat.data.model.MessageWithSender
import mx.ujat.dacyti.rideujat.data.repository.ChatRepository

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var currentTripId = ""
    private val senderCache = mutableMapOf<String, String>()

    fun initialize(tripId: String) {
        currentTripId = tripId
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        _uiState.update { it.copy(currentUserId = userId) }
        startPolling(tripId)
    }

    private fun startPolling(tripId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                fetchMessages(tripId)
                delay(2_000L)
            }
        }
    }

    private suspend fun fetchMessages(tripId: String) {
        repository.loadMessages(tripId).onSuccess { messages ->
            val unknownSenders = messages.map { it.senderId }
                .distinct().filter { !senderCache.containsKey(it) }
            if (unknownSenders.isNotEmpty()) {
                repository.getSenderNames(unknownSenders).forEach { (id, name) ->
                    senderCache[id] = name
                }
            }
            val enriched = messages.map { msg ->
                MessageWithSender(msg, senderCache[msg.senderId] ?: "…")
            }
            _uiState.update { it.copy(messages = enriched) }
        }
    }

    fun onInputChange(text: String) { _uiState.update { it.copy(inputText = text) } }

    fun sendMessage() {
        val state = _uiState.value
        if (state.inputText.isBlank() || state.isSending) return
        val userId = supabase.auth.currentUserOrNull()?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            repository.sendMessage(currentTripId, userId, state.inputText.trim()).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSending = false, inputText = "") }
                    fetchMessages(currentTripId)
                },
                onFailure = { e -> _uiState.update { it.copy(isSending = false, error = e.message) } }
            )
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}

data class ChatUiState(
    val messages: List<MessageWithSender> = emptyList(),
    val currentUserId: String = "",
    val inputText: String = "",
    val isSending: Boolean = false,
    val error: String? = null
)
