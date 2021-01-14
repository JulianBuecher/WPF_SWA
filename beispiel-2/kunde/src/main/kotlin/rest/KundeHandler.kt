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
package com.acme.kunde.rest

import com.acme.kunde.Router.Companion.idPathVar
import com.acme.kunde.entity.Kunde
import com.acme.kunde.entity.KundeId
import com.acme.kunde.rest.patch.KundePatcher
import com.acme.kunde.rest.patch.PatchOperation
import com.acme.kunde.service.CreateResult
import com.acme.kunde.service.FindByIdResult
import com.acme.kunde.service.KundeService
import com.acme.kunde.service.UpdateResult
import jakarta.validation.ConstraintViolation
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import mu.KotlinLogging
import org.springframework.hateoas.server.reactive.toCollectionModelAndAwait
import org.springframework.hateoas.server.reactive.toModelAndAwait
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_MODIFIED
import org.springframework.http.HttpStatus.PRECONDITION_FAILED
import org.springframework.http.HttpStatus.PRECONDITION_REQUIRED
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.badRequest
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.ServerResponse.status
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import java.net.URI

/**
 * Eine Handler-Function wird von der Router-Function [com.acme.kunde.Router.router]
 * aufgerufen, nimmt einen Request entgegen und erstellt den Response.
 *
 * [Klassendiagramm](../../images/KundeHandler.svg)
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @constructor Einen KundeHandler mit einem injizierten [KundeService] erzeugen.
 */
@Component
@Suppress("TooManyFunctions", "LargeClass")
class KundeHandler(
    private val service: KundeService,
    private val modelAssembler: KundeModelAssembler,
) {
    /**
     * Suche anhand der Kunde-ID
     * @param request Der eingehende Request
     * @return Ein ServerResponse mit dem Statuscode 200 und dem gefundenen Kunden einschließlich HATEOAS-Links, oder
     *      aber Statuscode 204.
     */
    suspend fun findById(request: ServerRequest): ServerResponse {
        val idStr = request.pathVariable(idPathVar)
        val id = KundeId.fromString(idStr)

        return when (val result = service.findById(id)) {
            is FindByIdResult.Success -> handleFound(result.kunde, request)
            is FindByIdResult.NotFound -> notFound().buildAndAwait()
        }
    }

    private suspend fun handleFound(kunde: Kunde, request: ServerRequest): ServerResponse {
        logger.debug { "handleFound: $kunde" }
        // https://tools.ietf.org/html/rfc7232#section-2.3
        val versionHeader = request.headers()
            .asHttpHeaders()
            .ifNoneMatch
            .firstOrNull()
        logger.debug { "versionHeader: $versionHeader" }
        val versionStr = "\"${kunde.version}\""
        if (versionStr == versionHeader) {
            return status(NOT_MODIFIED).buildAndAwait()
        }

        val kundeModel = modelAssembler.toModelAndAwait(kunde, request.exchange())
        // Entity Tag, um Aenderungen an der angeforderten
        // Ressource erkennen zu koennen.
        // Client: GET-Requests mit Header "If-None-Match"
        //         ggf. Response mit Statuscode NOT MODIFIED (s.o.)
        return ok().eTag(versionStr).bodyValueAndAwait(kundeModel)
    }


    /**
     * Suche mit diversen Suchkriterien als Query-Parameter. Es wird `List<Kunde>` statt `Flow<Kunde>` zurückgeliefert,
     * damit auch der Statuscode 204 möglich ist.
     * @param request Der eingehende Request mit den Query-Parametern.
     * @return Ein ServerResponse mit dem Statuscode 200 und einer Liste mit den gefundenen Kunden einschließlich
     *      Atom-Links, oder aber Statuscode 204.
     */
    suspend fun find(request: ServerRequest): ServerResponse {
        val queryParams = request.queryParams()
        val principal = request.principal().awaitFirst()
        // printend UUID des Users -> könnte man als username nehmen und als Identifizierung
        val principalName = request.principal().awaitFirst().name
        println(principal.toString())
        println(principalName.toString())

        // https://stackoverflow.com/questions/45903813/webflux-functional-how-to-detect-an-empty-flux-and-return-404
        val kunden = mutableListOf<Kunde>()
        service.find(queryParams)
            .onEach { kunde -> logger.debug { "find: $kunde" } }
            .toList(kunden)

        if (kunden.isEmpty()) {
            logger.debug("find(): Keine Kunden gefunden")
            return notFound().buildAndAwait()
        }

        // genau 1 Treffer bei der Suche anhand der Emailadresse
        if (queryParams.keys.contains("email")) {
            val kundeModel = modelAssembler.toModelAndAwait(kunden[0], request.exchange())
            logger.debug { "find(): Kunde mit email: $kundeModel" }
            return ok().bodyValueAndAwait(kundeModel)
        }

        val kundenModel = modelAssembler.toCollectionModelAndAwait(kunden.asFlow(), request.exchange())
        logger.debug { "find(): $kundenModel" }
        return ok().bodyValueAndAwait(kundenModel)
    }

    /**
     * Einen neuen Kunde-Datensatz anlegen.
     * @param request Der eingehende Request mit dem Kunde-Datensatz im Body.
     * @return Response mit Statuscode 201 einschließlich Location-Header oder Statuscode 400 falls Constraints verletzt
     *      sind oder der JSON-Datensatz syntaktisch nicht korrekt ist.
     */
    @Suppress("LongMethod", "ReturnCount")
    suspend fun create(request: ServerRequest): ServerResponse {
        val kunde = request.awaitBody<Kunde>()

        return when (val result = service.create(kunde)) {
            is CreateResult.Success -> handleCreated(result.kunde, request)

            is CreateResult.ConstraintViolations -> handleConstraintViolations(result.violations)

            is CreateResult.EmailExists ->
                badRequest().bodyValueAndAwait("Die Emailadresse ${result.email} existiert bereits")
        }
    }

    private suspend fun handleCreated(kunde: Kunde, request: ServerRequest): ServerResponse {
        logger.trace { "Kunde abgespeichert: $kunde" }
        val baseUri = getBaseUri(request.headers().asHttpHeaders(), request.uri())
        val location = URI("$baseUri/${kunde.id}")
        return created(location).buildAndAwait()
    }

    // z.B. Service-Funktion "create|update" mit Parameter "kunde" hat dann Meldungen mit "create.kunde.nachname:"
    private suspend fun handleConstraintViolations(violations: Set<ConstraintViolation<Kunde>>): ServerResponse {
        if (violations.isEmpty()) {
            return badRequest().buildAndAwait()
        }

        val kundeViolations = violations.map { violation ->
            KundeConstraintViolation(
                property = violation.propertyPath.toString(),
                message = violation.message,
            )
        }
        logger.trace { "violations: $kundeViolations" }
        return badRequest().bodyValueAndAwait(kundeViolations)
    }

    /**
     * Einen vorhandenen Kunde-Datensatz überschreiben.
     * @param request Der eingehende Request mit dem neuen Kunde-Datensatz im Body.
     * @return Response mit Statuscode 204 oder Statuscode 400, falls Constraints verletzt sind oder der JSON-Datensatz
     *      syntaktisch nicht korrekt ist.
     */
    @Suppress("LongMethod", "DuplicatedCode")
    suspend fun update(request: ServerRequest): ServerResponse {
        var version = getIfMatch(request)
            ?: return status(PRECONDITION_REQUIRED).bodyValueAndAwait("Versionsnummer fehlt")
        logger.trace { "Versionsnummer $version" }

        @Suppress("MagicNumber")
        if (version.length < 3) {
            return status(PRECONDITION_FAILED).bodyValueAndAwait("Falsche Versionsnummer $version")
        }
        version = version.substring(1, version.length - 1)

        val idStr = request.pathVariable(idPathVar)
        val id = KundeId.fromString(idStr)

        val kunde = request.awaitBody<Kunde>()
        return update(kunde, id, version)
    }

    private fun getIfMatch(request: ServerRequest): String? {
        // https://tools.ietf.org/html/rfc7232#section-2.3
        val versionList = request
            .headers()
            .asHttpHeaders()
            .ifMatch
        logger.trace { "versionList: $versionList" }
        return versionList.firstOrNull()
    }

    private suspend fun update(kunde: Kunde, id: KundeId, version: String) =
        when (val result = service.update(kunde, id, version)) {
            is UpdateResult.Success -> noContent().eTag("\"${result.kunde.version}\"").buildAndAwait()

            is UpdateResult.NotFound -> notFound().buildAndAwait()

            is UpdateResult.ConstraintViolations -> handleConstraintViolations(result.violations)

            is UpdateResult.VersionInvalid ->
                status(PRECONDITION_FAILED).bodyValueAndAwait("Falsche Versionsnummer ${result.version}")

            is UpdateResult.VersionOutdated ->
                status(PRECONDITION_FAILED).bodyValueAndAwait("Falsche Versionsnummer ${result.version}")

            is UpdateResult.EmailExists ->
                badRequest().bodyValueAndAwait("Die Emailadresse $${result.email} existiert bereits")
        }

    /**
     * Einen vorhandenen Kunde-Datensatz durch PATCH aktualisieren.
     * @param request Der eingehende Request mit dem PATCH-Datensatz im Body.
     * @return Response mit Statuscode 204 oder Statuscode 400, falls Constraints verletzt sind oder der JSON-Datensatz
     *      syntaktisch nicht korrekt ist.
     */
    @Suppress("LongMethod", "ReturnCount", "DuplicatedCode")
    suspend fun patch(request: ServerRequest): ServerResponse {
        var version = getIfMatch(request)
            ?: return status(PRECONDITION_REQUIRED).bodyValueAndAwait("Versionsnummer fehlt")

        // Im Header:    If-Match: "1234"
        @Suppress("MagicNumber")
        if (version.length < 3) {
            return status(PRECONDITION_FAILED).bodyValueAndAwait("Falsche Versionsnummer $version")
        }
        logger.trace { "Versionsnummer $version" }

        val idStr = request.pathVariable(idPathVar)
        val id = KundeId.fromString(idStr)

        val patchOps = request.awaitBody<List<PatchOperation>>()

        val kunde = when (val findByIdResult = service.findById(id)) {
            is FindByIdResult.Success -> findByIdResult.kunde
            is FindByIdResult.NotFound -> return notFound().buildAndAwait()
        }

        val patchedKunde = KundePatcher.patch(kunde, patchOps)
        logger.trace { "Kunde mit Patch-Ops: $patchedKunde" }
        version = version.substring(1, version.length - 1)

        return update(patchedKunde, id, version)
    }

    /**
     * Einen vorhandenen Kunden anhand seiner ID löschen.
     * @param request Der eingehende Request mit der ID als Pfad-Parameter.
     * @return Response mit Statuscode 204.
     */
    suspend fun deleteById(request: ServerRequest): ServerResponse {
        val idStr = request.pathVariable(idPathVar)
        val id = KundeId.fromString(idStr)

        val deleteResult = service.deleteById(id)
        logger.debug { "deleteById(): $deleteResult" }

        return noContent().buildAndAwait()
    }

    /**
     * Einen vorhandenen Kunden anhand seiner Emailadresse löschen.
     * @param request Der eingehende Request mit der Emailadresse als Query-Parameter.
     * @return Response mit Statuscode 204.
     */
    suspend fun deleteByEmail(request: ServerRequest): ServerResponse {
        val email = request.queryParam("email")

        return if (email.isEmpty) {
            noContent().buildAndAwait()
        } else {
            val deleteResult = service.deleteByEmail(email.get())
            logger.debug { "deleteByEmail(): $deleteResult" }
            noContent().buildAndAwait()
        }
    }

    private companion object {
        val logger = KotlinLogging.logger {}
    }
}
