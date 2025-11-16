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
                // ⚠️ DÉVELOPPEMENT UNIQUEMENT : Accepter tous les certificats SSL
                override fun onReceivedSslError(
                    view: WebView?,
                    handler: android.webkit.SslErrorHandler?,
                    error: android.net.http.SslError?
                ) {
                    android.util.Log.w("ESFAuthService", "SSL Error ignoré: ${error?.toString()}")
                    handler?.proceed() // ATTENTION : Ignorer l'erreur SSL
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    // Log pour déboguer
                    android.util.Log.d("ESFAuthService", "Page loaded: $url")

                    // Log des cookies pour déboguer
                    val cookiesForUrl = cookieManager.getCookie(url)
                    android.util.Log.d("ESFAuthService", "Cookies for URL: ${cookiesForUrl?.take(100)}")

                    // Éviter d'appeler le callback plusieurs fois
                    if (authCompleted) return

                    // DÉTECTION STRICTE : Basée sur le cookie de session ESF
                    // Le cookie "idsrv-siesf" n'existe QUE après une connexion réussie
                    // IMPORTANT: Récupérer les cookies de TOUS les domaines ESF
                    val cookiesCurrentUrl = cookieManager.getCookie(url) ?: ""
                    val cookiesBase = cookieManager.getCookie(Constants.ESF_BASE_URL) ?: ""
                    val cookiesIdentity = cookieManager.getCookie("https://identity.w-esf.com") ?: ""
                    val cookiesCarnetRouge = cookieManager.getCookie("https://carnet-rouge-esf.app") ?: ""
                    val allCookies = "$cookiesCurrentUrl; $cookiesBase; $cookiesIdentity; $cookiesCarnetRouge"

                    android.util.Log.d("ESFAuthService", "All cookies combined (${allCookies.length} chars): ${allCookies.take(200)}...")

                    // Vérifier la présence du cookie de session ESF (Identity Server)
                    // idsrv-siesf = cookie créé par le serveur d'identité ESF après login
                    val hasAuthCookie = allCookies.contains("idsrv-siesf", ignoreCase = false) ||
                                       allCookies.contains("idsrv", ignoreCase = false)

                    android.util.Log.d("ESFAuthService", "Has auth cookie: $hasAuthCookie")

                    // NOUVELLE STRATÉGIE: Détecter le succès via le code OAuth dans l'URL
                    // Après connexion réussie, ESF redirige vers carnet-rouge-esf.app/?code=...
                    val isRedirectWithCode = url?.contains("carnet-rouge-esf.app/?code=") == true

                    android.util.Log.d("ESFAuthService", "isRedirectWithCode: $isRedirectWithCode")

                    if (isRedirectWithCode) {
                        android.util.Log.d("ESFAuthService", "OAuth success detected via redirect URL!")
                        android.util.Log.d("ESFAuthService", "All cookies: ${allCookies.take(200)}...")

                        // Même si les cookies semblent vides, on les sauvegarde
                        // L'app web doit gérer l'échange du code contre un token
                        authCompleted = true
                        onAuthSuccess(allCookies)
                    }

                    // Ancienne méthode de détection (backup)
                    else if (hasAuthCookie && !url.orEmpty().contains("identity.w-esf.com")) {
                        android.util.Log.d("ESFAuthService", "Auth success detected via cookies! Cookies: ${allCookies.take(100)}...")

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
