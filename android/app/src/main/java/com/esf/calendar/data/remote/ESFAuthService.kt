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
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    // Si on est redirigé vers l'app ESF, la connexion a réussi
                    if (url?.startsWith(Constants.ESF_BASE_URL) == true &&
                        url.contains("planning", ignoreCase = true)) {

                        // Récupérer les cookies
                        val cookies = cookieManager.getCookie(Constants.ESF_BASE_URL) ?: ""

                        if (cookies.isNotEmpty()) {
                            onAuthSuccess(cookies)
                        } else {
                            onAuthError("Échec de la récupération des cookies")
                        }
                    }

                    // Détecter une erreur d'authentification
                    if (url?.contains("error", ignoreCase = true) == true) {
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
