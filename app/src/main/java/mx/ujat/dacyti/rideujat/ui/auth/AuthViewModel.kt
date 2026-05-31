package mx.ujat.dacyti.rideujat.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.ujat.dacyti.rideujat.data.repository.AuthRepository

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(correo: String, contrasena: String) {
        val error = validarCorreo(correo) ?: validarCampoRequerido(contrasena, "contraseña")
        if (error != null) { _uiState.value = AuthUiState.Error(error); return }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.login(correo, contrasena).fold(
                onSuccess = { _uiState.value = AuthUiState.Success },
                onFailure = { _uiState.value = AuthUiState.Error(it.message ?: "Error al iniciar sesión") }
            )
        }
    }

    fun register(nombre: String, matricula: String, correo: String, contrasena: String) {
        val error = validarCampoRequerido(nombre, "nombre")
            ?: validarCampoRequerido(matricula, "matrícula")
            ?: validarCorreo(correo)
            ?: validarContrasena(contrasena)
        if (error != null) { _uiState.value = AuthUiState.Error(error); return }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            repository.register(nombre, matricula, correo, contrasena).fold(
                onSuccess = { _uiState.value = AuthUiState.Success },
                onFailure = { _uiState.value = AuthUiState.Error(it.message ?: "Error al registrarse") }
            )
        }
    }

    fun resetState() { _uiState.value = AuthUiState.Idle }

    private fun validarCorreo(correo: String): String? =
        if (!correo.endsWith("@ujat.mx")) "Solo se permiten correos @ujat.mx" else null

    private fun validarCampoRequerido(valor: String, campo: String): String? =
        if (valor.isBlank()) "El campo $campo es requerido" else null

    private fun validarContrasena(contrasena: String): String? =
        if (contrasena.length < 6) "La contraseña debe tener al menos 6 caracteres" else null
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
