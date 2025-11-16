package com.esf.calendar.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

/**
 * Modèle de données pour un événement ESF
 * Correspond à la structure JSON retournée par l'API ESF
 */
@Entity(tableName = "esf_events")
data class ESFEvent(
    @PrimaryKey
    @SerializedName("ih")
    val horaireId: Long, // Identifiant unique de l'horaire

    @SerializedName("ip")
    val ip: Long? = null,

    @SerializedName("ipp")
    val ipp: Long? = null,

    @SerializedName("cp")
    val codePoste: String, // Code court du poste (ex: "CORVEE CHARMIEUX", "ABSENT")

    @SerializedName("lp")
    val labelPoste: String, // Label long du poste

    @SerializedName("ictp")
    val ictp: Int? = null,

    @SerializedName("ctp")
    val codeTypePrestation: String? = null, // Code type prestation (ex: "ABS")

    @SerializedName("ittp")
    val ittp: Long? = null,

    @SerializedName("dd")
    val dateDebut: String, // Format: /Date(1741158000000+0100)/

    @SerializedName("df")
    val dateFin: String, // Format: /Date(1741161600000+0100)/

    @SerializedName("ilr")
    val ilr: Int? = null,

    @SerializedName("llr")
    val labelLieu: String? = null, // Lieu (ex: "CHARMIEUX")

    @SerializedName("is")
    val skiId: Long? = null,

    @SerializedName("cs")
    val codeSki: String? = null, // Code ski (ex: "SKIALPIN")

    @SerializedName("ls")
    val labelSki: String? = null, // Label ski (ex: "SKI ALPIN")

    @SerializedName("ine")
    val niveauEleveId: Int? = null,

    @SerializedName("cne")
    val codeNiveau: String? = null, // Code niveau (ex: "ALPOUR")

    @SerializedName("lne")
    val labelNiveau: String? = null, // Label niveau (ex: "Ourson (A)")

    @SerializedName("ile")
    val langueId: Long? = null,

    @SerializedName("cle")
    val codeLangue: String? = null, // Code langue (ex: "FRA")

    @SerializedName("lle")
    val labelLangue: String? = null, // Label langue (ex: "Français")

    @SerializedName("nl")
    val nombreEleves: Int? = null, // Nombre d'élèves

    @SerializedName("cm")
    val commentaire: String? = null, // Commentaires/Notes

    @SerializedName("cmmono")
    val commentaireMono: String? = null,

    @SerializedName("im")
    val moniteurId: Long? = null, // ID du moniteur

    @SerializedName("dm")
    val dateModification: String? = null, // Date de modification

    @SerializedName("nu")
    val nu: Int? = null,

    @SerializedName("re")
    val re: Boolean? = false,

    // Champs calculés/dérivés (non dans le JSON)
    val startDateTime: LocalDateTime? = null, // Calculé depuis dateDebut
    val endDateTime: LocalDateTime? = null, // Calculé depuis dateFin
    val isAbsence: Boolean = false, // true si c'est une absence
    val syncedAt: Long = System.currentTimeMillis() // Timestamp de synchro
) {
    /**
     * Vérifie si l'événement est une absence
     */
    fun isAbsenceEvent(): Boolean {
        val absenceCodes = setOf("ABSENT", "ABSENCEMONO")
        return codePoste in absenceCodes || labelPoste in absenceCodes
    }

    /**
     * Récupère une description formatée de l'événement
     */
    fun getFormattedDescription(): String {
        return buildString {
            labelNiveau?.let { append("Niveau: $it\n") }
            labelLangue?.let { nombreEleves?.let { nb -> append("$it $nb\n") } }
            labelLieu?.let { append("Lieu: $it\n") }
            commentaire?.takeIf { it.isNotBlank() }?.let { append("\n$it") }
        }
    }

    /**
     * Récupère le titre de l'événement
     */
    fun getTitle(): String = labelPoste
}
