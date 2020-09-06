/*
 * Copyright (C) 2017 - present Juergen Zimmermann, Hochschule Karlsruhe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.acme.kunde.db

import com.acme.kunde.entity.Adresse
import com.acme.kunde.entity.FamilienstandType
import com.acme.kunde.entity.GeschlechtType
import com.acme.kunde.entity.Kunde
import mu.KotlinLogging
import org.springframework.data.mongodb.core.query.CriteriaDefinition
import org.springframework.data.mongodb.core.query.div
import org.springframework.data.mongodb.core.query.gte
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.regex
import org.springframework.util.MultiValueMap

/**
 * Singleton-Klasse, um _Criteria Queries_ für _MongoDB_ zu bauen.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Suppress("TooManyFunctions", "unused")
object CriteriaUtil {
    private const val nachname = "nachname"
    private const val email = "email"
    private const val kategorie = "kategorie"
    private const val plz = "plz"
    private const val ort = "ort"
    private const val umsatzMin = "umsatzmin"
    private const val geschlecht = "geschlecht"
    private const val familienstand = "familienstand"
    private const val interessen = "interessen"
    private val logger = KotlinLogging.logger {}

    /**
     * Eine `MultiValueMap` von _Spring_ wird in eine Liste von `CriteriaDefinition` für _MongoDB_ konvertiert,
     * um flexibel nach Kunden suchen zu können.
     * @param queryParams Die Query-Parameter in einer `MultiValueMap`.
     * @return Eine Liste von `CriteriaDefinition`.
     */
    fun getCriteria(queryParams: MultiValueMap<String, String>): List<CriteriaDefinition?> {
        val criteria = queryParams.map { (key, value) ->
            getCriteria(key, value)
        }

        logger.debug { "#Criteria: ${criteria.size}" }
        criteria.forEach { logger.debug { "Criteria: ${it?.criteriaObject}" } }
        return criteria
    }

    /**
     * Ein Schlüssel und evtl. mehrere Werte aus einer `MultiValueMap` von _Spring_ wird in eine `CriteriaDefinition`
     * für _MongoDB_ konvertiert, um nach Kunden suchen zu können.
     * @param propertyName Der Property-Name als Schlüssel aus der `MultiValueMap`
     * @param propertyValues Liste von Werten zum Property-Namen.
     * @return Eine CriteriaDefinition` oder null.
     */
    fun getCriteria(propertyName: String, propertyValues: List<String>?): CriteriaDefinition? {
        // zu "interessen" kann es mehrere Werte geben
        // https://tools.ietf.org/html/rfc3986#section-3.4
        if (propertyName == interessen) {
            return getCriteriaInteressen(propertyValues)
        }

        if (propertyValues?.size != 1) {
            return null
        }

        val value = propertyValues[0]
        return when (propertyName) {
            nachname -> getCriteriaNachname(value)
            email -> getCriteriaEmail(value)
            kategorie -> getCriteriaKategorie(value)
            plz -> getCriteriaPlz(value)
            ort -> getCriteriaOrt(value)
            umsatzMin -> getCriteriaUmsatz(value)
            geschlecht -> getCriteriaGeschlecht(value)
            familienstand -> getCriteriaFamilienstand(value)
            else -> null
        }
    }

    // Nachname: Suche nach Teilstrings ohne Gross-/Kleinschreibung
    private fun getCriteriaNachname(nachname: String): CriteriaDefinition = Kunde::nachname.regex(nachname, "i")

    // Email: Suche mit Teilstring ohne Gross-/Kleinschreibung
    private fun getCriteriaEmail(email: String): CriteriaDefinition = Kunde::email.regex(email, "i")

    private fun getCriteriaKategorie(kategorieStr: String): CriteriaDefinition? {
        val kategorieVal = kategorieStr.toIntOrNull() ?: return null
        return Kunde::kategorie isEqualTo kategorieVal
    }

    // PLZ: Suche mit Praefix
    private fun getCriteriaPlz(plz: String): CriteriaDefinition = Kunde::adresse / Adresse::plz regex "^$plz"

    // Ort: Suche nach Teilstrings ohne Gross-/Kleinschreibung
    private fun getCriteriaOrt(ort: String): CriteriaDefinition = (Kunde::adresse / Adresse::ort).regex(ort, "i")

    private fun getCriteriaUmsatz(umsatzStr: String): CriteriaDefinition? {
        val umsatzVal = umsatzStr.toBigDecimalOrNull() ?: return null
        return Kunde::umsatz gte umsatzVal
    }

    private fun getCriteriaGeschlecht(geschlechtStr: String): CriteriaDefinition? {
        val geschlechtVal = GeschlechtType.build(geschlechtStr)
        return Kunde::geschlecht isEqualTo geschlechtVal
    }

    private fun getCriteriaFamilienstand(familienstandStr: String): CriteriaDefinition? {
        val familienstandVal = FamilienstandType.build(familienstandStr)
        return Kunde::familienstand isEqualTo familienstandVal
    }

    private fun getCriteriaInteressen(interessenValues: List<String>?): CriteriaDefinition? {
        if (interessenValues == null) {
            return null
        }

        return when (interessenValues.size) {
            0 -> null

            1 -> Kunde::interessen isEqualTo interessenValues[0]

            else -> {
                val first = Kunde::interessen isEqualTo interessenValues[0]
                val weitereInteressen = interessenValues.toMutableList().removeAt(0)
                val criteriaArray = weitereInteressen
                    .map { interesse -> Kunde::interessen isEqualTo interesse }
                    .toTypedArray()
                @Suppress("SpreadOperator")
                first.andOperator(*criteriaArray)
            }
        }
    }
}
