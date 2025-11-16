package com.esf.calendar.util

import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * ⚠️ DÉVELOPPEMENT UNIQUEMENT ⚠️
 *
 * Client OkHttp qui accepte tous les certificats SSL (même invalides)
 * Utilisé pour contourner les problèmes de certificats auto-signés du serveur ESF
 *
 * NE JAMAIS UTILISER EN PRODUCTION !
 */
object UnsafeOkHttpClient {

    fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            // Créer un TrustManager qui accepte tous les certificats
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            // Installer le TrustManager qui accepte tout
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            // Créer un SSLSocketFactory avec notre TrustManager
            val sslSocketFactory = sslContext.socketFactory

            return OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true } // Accepter tous les hostnames
                .build()

        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
