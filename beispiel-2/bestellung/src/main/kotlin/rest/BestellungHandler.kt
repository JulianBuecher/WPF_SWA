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
package com.acme.bestellung.rest

import com.acme.bestellung.Router.Companion.idPathVar
import com.acme.bestellung.entity.Bestellung
import com.acme.bestellung.entity.KundeId
import com.acme.bestellung.service.BestellungService
import com.acme.bestellung.service.CreateResult
import jakarta.validation.ConstraintViolation
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import org.springframework.hateoas.server.reactive.toCollectionModelAndAwait
import org.springframework.hateoas.server.reactive.toModelAndAwait
import org.springframework.http.HttpHeaders.IF_NONE_MATCH
import org.springframework.http.HttpStatus.NOT_MODIFIED
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.ServerResponse.status
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import java.net.URI

/**
 * Eine Handler-Function wird von der Router-Function [com.acme.bestellung.Router.router] aufgerufen,
 * nimmt einen Request entgegen und erstellt den Response.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @constructor Einen BestellungHandler mit einem injizierten [BestellungService] erzeugen.
 */
@Component
class BestellungHandler(private val service: BestellungService, private val modelAssembler: BestellungModelAssembler) {
    /**
     * Suche anhand der Bestellung-ID
     * @param request Der eingehende Request
     * @return Ein Response mit dem Statuscode 200 und der gefundenen Bestellung einschließlich Atom-Links,
     *      oder aber Statuscode 204.
     */
    suspend fun findById(request: ServerRequest): ServerResponse {
        val id = request.pathVariable(idPathVar)

        val bestellung = service.findById(id) ?: return notFound().buildAndAwait()
        logger.debug { "findById: $bestellung" }

        val version = bestellung.version
        val versionHeader = request.headers()
            .header(IF_NONE_MATCH)
            .firstOrNull()
            ?.toIntOrNull()

        if (version == versionHeader) {
            return status(NOT_MODIFIED).buildAndAwait()
        }

        val bestellungModel = modelAssembler.toModelAndAwait(bestellung, request.exchange())
        // Entity Tag, um Aenderungen an der angeforderten Ressource erkennen zu koennen.
        // Client: GET-Requests mit Header "If-None-Match"
        //         ggf. Response mit Statuscode NOT MODIFIED (s.o.)
        return ok().eTag("\"$version\"").bodyValueAndAwait(bestellungModel)
    }

    /**
     * Suche mit diversen Suchkriterien als Query-Parameter. Es wird eine Liste zurückgeliefert, damit auch der
     * Statuscode 204 möglich ist.
     * @param request Der eingehende Request mit den Query-Parametern.
     * @return Ein Response mit dem Statuscode 200 und einer Liste mit den gefundenen Bestellungen einschließlich
     *      Atom-Links, oder aber Statuscode 204.
     */
    @Suppress("ReturnCount", "LongMethod")
    suspend fun find(request: ServerRequest): ServerResponse {
        val queryParams = request.queryParams()
        if (queryParams.size > 1) {
            return notFound().buildAndAwait()
        }

        val bestellungen = if (queryParams.isEmpty()) {
            service.findAll()
        } else {
            val kundeId = request.queryParam("kundeId")
            if (!kundeId.isPresent) {
                return notFound().buildAndAwait()
            }

            service.findByKundeId(KundeId.fromString(kundeId.get()))
        }

        val bestellungenList = mutableListOf<Bestellung>()
        bestellungen.toList(bestellungenList)

        return if (bestellungenList.isEmpty()) {
            notFound().buildAndAwait()
        } else {
            val bestellungenModel =
                modelAssembler.toCollectionModelAndAwait(bestellungenList.asFlow(), request.exchange())
            logger.debug { "find(): $bestellungenModel" }
            ok().bodyValueAndAwait(bestellungenModel)
        }
    }

    /**
     * Einen neuen Bestellung-Datensatz anlegen.
     * @param request Der eingehende Request mit dem Bestellung-Datensatz im Body.
     * @return Response mit Statuscode 201 einschließlich Location-Header oder Statuscode 400 falls Constraints verletzt
     *      sind oder der JSON-Datensatz syntaktisch nicht korrekt ist.
     */
    suspend fun create(request: ServerRequest): ServerResponse {
        val bestellung = request.awaitBody<Bestellung>()

        return when (val result = service.create(bestellung)) {
            is CreateResult.Success -> handleCreated(result.bestellung, request)
            is CreateResult.ConstraintViolations -> handleConstraintViolations(result.violations)
        }
    }

    private suspend fun handleCreated(bestellung: Bestellung, request: ServerRequest): ServerResponse {
        logger.debug { "handleCreated(): $bestellung" }
        val baseUri = getBaseUri(request.headers().asHttpHeaders(), request.uri())
        val location = URI("$baseUri/${bestellung.id}")
        logger.debug { "handleCreated(): $location" }
        return created(location).buildAndAwait()
    }

    // z.B. Service-Funktion "create|update" mit Parameter "bestellung" hat dann
    // Meldungen mit "create.bestellung.nachname:"
    private suspend fun handleConstraintViolations(violations: Set<ConstraintViolation<Bestellung>>): ServerResponse {
        if (violations.isEmpty()) {
            return ServerResponse.badRequest().buildAndAwait()
        }

        val bestellungViolations = violations.map { violation ->
            BestellungConstraintViolation(property = violation.propertyPath.toString(), message = violation.message)
        }
        logger.trace { "violations: $bestellungViolations" }
        return ServerResponse.badRequest().bodyValueAndAwait(bestellungViolations)
    }

    private companion object {
        val logger = KotlinLogging.logger {}
    }
}
