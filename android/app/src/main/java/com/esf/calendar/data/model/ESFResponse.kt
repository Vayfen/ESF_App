package com.esf.calendar.data.model

import com.google.gson.annotations.SerializedName

/**
 * Modèle de réponse de l'API ESF
 * Structure globale du JSON retourné
 */
data class ESFResponse(
    @SerializedName("Page")
    val page: Int,

    @SerializedName("Pages")
    val pages: Int,

    @SerializedName("Total")
    val total: Int,

    @SerializedName("ServerTime")
    val serverTime: String, // Format: /Date(1740953500937)/

    @SerializedName("Items")
    val items: List<ESFEvent>
)

/**
 * Wrapper pour la requête AJAX vers l'API ESF
 */
data class ESFApiRequest(
    val serviceContract: String = "IPlanningParticulierServicePublic",
    val serviceMethod: String = "GetListeHorairesMoniteur",
    val methodParams: ESFMethodParams
)

/**
 * Paramètres de la méthode API
 */
data class ESFMethodParams(
    val typeLibelle: String = "1",
    val language: String = "1",
    @SerializedName("IdGenCaisse")
    val idGenCaisse: String = "0",
    @SerializedName("IdGenPosteTechnique")
    val idGenPosteTechnique: String = "6862462",
    @SerializedName("IdComLangue")
    val idComLangue: String = "1",
    @SerializedName("IdComSaison")
    val idComSaison: String = "63",
    @SerializedName("NoEcole")
    val noEcole: String = "356",
    @SerializedName("CodeUc")
    val codeUc: String = "TECH-UC002-M",
    @SerializedName("CodeApplication")
    val codeApplication: String = "PLANNING-PARTICULIER-MONITEUR",
    val idTecMoniteurList: List<String>,
    val dateHeureDebut: String, // Format: /Date(timestamp)/
    val dateHeureFin: String, // Format: /Date(timestamp)/
    val dateReferenceDelta: String? = null
)
