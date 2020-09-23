/*
 * Copyright (C) 2016 - present Juergen Zimmermann, Hochschule Karlsruhe
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
package com.acme.kunde.service

import com.acme.kunde.entity.Kunde
import kotlinx.coroutines.flow.map
import org.springframework.data.mongodb.core.ReactiveFindOperation
import org.springframework.data.mongodb.core.asType
import org.springframework.data.mongodb.core.awaitOneOrNull
import org.springframework.data.mongodb.core.distinct
import org.springframework.data.mongodb.core.flow
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service

/**
 * Anwendungslogik für Werte zu Kunden (für "Software Engineering").
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Service
class KundeValuesService(private val mongo: ReactiveFindOperation) {
    /**
     * Nachnamen anhand eines Präfix ermitteln.
     *
     * @param prefix Präfix für Nachnamen
     * @return Gefundene Nachnamen
     */
    @Suppress("DEPRECATION")
    fun findNachnamenByPrefix(prefix: String) = mongo
        .query<Kunde>()
        .distinct(Kunde::nachname)
        .asType<NachnameProj>()
        .matching(where(Kunde::nachname).regex("^$prefix", "i"))
        .flow()
        .map { it.nachname }

    /**
     * Emailadressen anhand eines Präfix ermitteln.
     * @param prefix Präfix für Emailadressen.
     * @return Gefundene Emailadressen.
     */
    fun findEmailsByPrefix(prefix: String) = mongo
        .query<Kunde>()
        .asType<EmailProj>()
        .matching(where(Kunde::email).regex("^$prefix", "i"))
        .flow()
        .map { it.email }

    /**
     * Version zur Kunde-ID ermitteln.
     * @param id Kunde-ID.
     * @return Versionsnummer.
     */
    suspend fun findVersionById(id: String) = mongo
        .query<Kunde>()
        .asType<VersionProj>()
        .matching(Kunde::id isEqualTo id)
        .awaitOneOrNull()
        ?.version
}

/**
 * Hilfsklasse für die Projektion, wenn bei der DB-Suche nur der Nachname benötigt wird
 * @param nachname Der Nachname, auf den projeziert wird
 */
data class NachnameProj(val nachname: String)

/**
 * Hilfsklasse für die Projektion, wenn bei der DB-Suche nur die Emailadresse benötigt wird
 * @param email Die Emailadresse, auf die projeziert wird
 */
data class EmailProj(val email: String)

/**
 * Hilfsklasse für die Projektion, wenn bei der DB-Suche nur die Versionsnummer benötigt wird
 * @param version Die Versionsnummer, auf die projeziert wird
 */
data class VersionProj(val version: Int)
