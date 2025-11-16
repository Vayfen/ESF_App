package com.esf.calendar.ui.screens.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.esf.calendar.data.model.ESFCredentials
import com.esf.calendar.data.repository.ESFRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pour l'écran de login
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ESFRepository(application)

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Initial)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Vérifie si l'utilisateur est déjà connecté au démarrage
     */
    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            if (repository.isLoggedIn()) {
                _uiState.value = LoginUiState.Success
            }
        }
    }

    /**
     * Gère la réussite de l'authentification WebView
     */
    fun onAuthSuccess(cookies: String, moniteurId: String) {
        viewModelScope.launch {
            try {
                // Sauvegarder les credentials et cookies
                repository.saveCookies(cookies)
                repository.saveMoniteurId(moniteurId)

                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    /**
     * Gère l'échec de l'authentification
     */
    fun onAuthError(error: String) {
        _uiState.value = LoginUiState.Error(error)
    }

    /**
     * Démarre le processus de connexion
     */
    fun startLogin() {
        _uiState.value = LoginUiState.Loading
    }

    /**
     * Réinitialise l'état d'erreur
     */
    fun resetError() {
        _uiState.value = LoginUiState.Initial
    }
}

/**
 * États possibles de l'écran de login
 */
sealed class LoginUiState {
    object Initial : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
