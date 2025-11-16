package com.esf.calendar.data.remote

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.esf.calendar.data.model.ESFCredentials
import com.esf.calendar.util.Constants
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Service d'authentification ESF
 * Utilise WebView pour gérer le flux OAuth complexe de l'ESF
 */
class ESFAuthService(private val context: Context) {

    /**
     * Authentifie l'utilisateur via WebView et récupère les cookies de session
     *
     * @param credentials Les identifiants de l'utilisateur
     * @param webView La WebView à utiliser pour l'authentification
     * @param onAuthSuccess Callback appelé en cas de succès
     * @param onAuthError Callback appelé en cas d'erreur
     */
    fun authenticate(
        credentials: ESFCredentials,
        webView: WebView,
        onAuthSuccess: (String) -> Unit,
        onAuthError: (String) -> Unit
    ) {
        try {
            // Configurer CookieManager
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.removeAllCookies(null)

            // Configurer WebView
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
            }

            // Détecter la redirection après connexion réussie
            var authCompleted = false

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    // Log pour déboguer
                    android.util.Log.d("ESFAuthService", "Page loaded: $url")

                    // Éviter d'appeler le callback plusieurs fois
                    if (authCompleted) return

                    // Détecter la connexion réussie selon plusieurs critères
                    val isAuthSuccess = url?.let { currentUrl ->
                        // Méthode 1: URL contient "PlanningParticulier" ou "planning"
                        currentUrl.contains("PlanningParticulier", ignoreCase = true) ||
                        currentUrl.contains("/planning", ignoreCase = true) ||
                        // Méthode 2: URL du domaine ESF (pas identity.w-esf.com)
                        (currentUrl.contains("w-esf.com") &&
                         !currentUrl.contains("identity.w-esf.com")) ||
                        // Méthode 3: Présence de cookies de session
                        (currentUrl.contains("carnet-rouge-esf.app") &&
                         cookieManager.getCookie(url)?.contains(".ASPXAUTH") == true)
                    } ?: false

                    if (isAuthSuccess) {
                        // Récupérer les cookies de toutes les URLs possibles
                        val cookiesBase = cookieManager.getCookie(Constants.ESF_BASE_URL) ?: ""
                        val cookiesCurrent = cookieManager.getCookie(url) ?: ""
                        val allCookies = "$cookiesBase; $cookiesCurrent"

                        android.util.Log.d("ESFAuthService", "Auth success! Cookies: ${allCookies.take(100)}...")

                        if (allCookies.isNotBlank()) {
                            authCompleted = true
                            onAuthSuccess(allCookies)
                        }
                    }

                    // Détecter une erreur d'authentification
                    if (url?.contains("error", ignoreCase = true) == true) {
                        authCompleted = true
                        onAuthError("Identifiants incorrects")
                    }
                }
            }

            // Charger la page de connexion
            webView.loadUrl(Constants.ESF_BASE_URL)

        } catch (e: Exception) {
            onAuthError("Erreur d'authentification: ${e.message}")
        }
    }

    /**
     * Récupère les cookies actuels de la session
     */
    fun getCurrentCookies(): String? {
        val cookieManager = CookieManager.getInstance()
        return cookieManager.getCookie(Constants.ESF_BASE_URL)
    }

    /**
     * Restaure les cookies d'une session précédente
     */
    fun restoreCookies(cookies: String) {
        val cookieManager = CookieManager.getInstance()
        val cookieList = cookies.split(";")

        for (cookie in cookieList) {
            cookieManager.setCookie(Constants.ESF_BASE_URL, cookie.trim())
        }

        cookieManager.flush()
    }

    /**
     * Nettoie tous les cookies
     */
    fun clearCookies() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }
}
