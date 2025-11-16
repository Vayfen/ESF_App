package com.esf.calendar.data.remote

import com.esf.calendar.data.model.ESFApiRequest
import com.esf.calendar.data.model.ESFResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Interface Retrofit pour l'API ESF
 */
interface ESFApiService {

    /**
     * Récupère la liste des horaires/événements du moniteur
     * Endpoint dynamique car l'URL complète est construite avec les paramètres
     */
    @POST
    suspend fun getHorairesMoniteur(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body request: ESFApiRequest
    ): Response<ESFResponse>
}
