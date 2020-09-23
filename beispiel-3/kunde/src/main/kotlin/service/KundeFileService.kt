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

import com.acme.kunde.db.deleteAllAndAwait
import com.acme.kunde.db.getResourceAndAwait
import com.acme.kunde.db.storeAndAwait
import com.acme.kunde.entity.Kunde
import com.acme.kunde.entity.KundeId
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging
import org.bson.types.ObjectId
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.asType
import org.springframework.data.mongodb.core.awaitExists
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.gridfs.ReactiveGridFsOperations
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service

/**
 * Anwendungslogik für Binärdateien zu Kunden.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Service
class KundeFileService(
    private val mongo: ReactiveMongoOperations,
    private val gridFs: ReactiveGridFsOperations,
) {
    /**
     * Binärdatei (z.B. Bild oder Video) zu einem Kunden mit gegebener ID ermitteln.
     * @param kundeId Kunde-ID
     * @return Binärdatei, falls sie existiert. Sonst empty().
     */
    suspend fun findFile(kundeId: KundeId): ReactiveGridFsResource? {
        if (!kundeExists(kundeId)) {
            logger.debug { "findFile(): Kein Kunde mit der ID $kundeId" }
            return null
        }

        return gridFs.getResourceAndAwait(kundeId.toString())
    }

    private suspend fun kundeExists(kundeId: KundeId) = mongo
        .query<Kunde>()
        .asType<IdProj>()
        .matching(Kunde::id isEqualTo kundeId)
        .awaitExists()

    /**
     * Binäre Daten aus einem DataBuffer werden persistent mit der gegebenen Kunden-ID als Dateiname abgespeichert.
     * Der Inputstream wird am Ende geschlossen.
     *
     * @param dataBuffer DataBuffer mit binären Daten.
     * @param kundeId Kunde-ID
     * @param mediaType MIME-Type, z.B. image/png
     * @return ID der neuangelegten Binärdatei oder null
     */
    // FIXME @Transactional
    suspend fun save(dataBuffer: Flow<DataBuffer>, kundeId: KundeId, mediaType: MediaType): ObjectId? {
        if (!kundeExists(kundeId)) {
            logger.debug { "save(): Kein Kunde mit der ID $kundeId" }
            return null
        }
        logger.debug { "save(): kundeId=$kundeId, mediaType=$mediaType" }

        // TODO MIME-Type ueberpruefen
        logger.warn("TODO: MIME-Type ueberpruefen")

        // ggf. Binaerdatei loeschen
        val filename = kundeId.toString()
        gridFs.deleteAllAndAwait(filename)

        val objectId = gridFs.storeAndAwait(dataBuffer, filename, mediaType)
        logger.debug { "save(): Binaerdatei angelegt: ObjectId=$objectId, mediaType=$mediaType" }
        return objectId
    }

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    /**
     * Hilfsklasse für die Projektion, wenn bei der DB-Suche nur IDs benötigt wird
     * @param id Die IDs, auf die projeziert wird
     */
    data class IdProj(val id: String)
}
