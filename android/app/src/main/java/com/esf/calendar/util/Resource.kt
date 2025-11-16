package com.esf.calendar.util

/**
 * Classe wrapper pour gérer les états de chargement, succès et erreur
 * Utilisée dans l'architecture MVVM pour communiquer entre Repository et ViewModel
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val exception: Exception? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()

    /**
     * Exécute une action si la resource est en état Success
     */
    inline fun onSuccess(action: (T) -> Unit): Resource<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    /**
     * Exécute une action si la resource est en état Error
     */
    inline fun onError(action: (String) -> Unit): Resource<T> {
        if (this is Error) {
            action(message)
        }
        return this
    }

    /**
     * Exécute une action si la resource est en état Loading
     */
    inline fun onLoading(action: () -> Unit): Resource<T> {
        if (this is Loading) {
            action()
        }
        return this
    }
}

/**
 * État UI générique
 */
data class UiState<T>(
    val isLoading: Boolean = false,
    val data: T? = null,
    val error: String? = null
) {
    val isSuccess: Boolean get() = data != null && error == null
    val isError: Boolean get() = error != null
}
