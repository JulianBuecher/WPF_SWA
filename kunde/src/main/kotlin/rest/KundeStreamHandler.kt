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

import com.acme.kunde.service.KundeStreamService
import kotlinx.coroutines.flow.map
import org.springframework.hateoas.server.reactive.toModelAndAwait
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyAndAwait

/**
 * Eine Handler-Function wird von der Router-Function [com.acme.kunde.Router.router]
 * aufgerufen, nimmt einen Request entgegen und erstellt den Response.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @constructor Einen KundeStreamHandler mit einem injizierten
 *      [com.acme.kunde.service.KundeStreamService] erzeugen.
 */
@Component
class KundeStreamHandler(private val service: KundeStreamService, private val modelAssembler: KundeModelAssembler) {
    /**
     * Alle Kunden als Event-Stream zurückliefern.
     * @param request Das eingehende Request-Objekt.
     * @return Response mit dem MIME-Typ `text/event-stream`.
     */
    suspend fun findAll(request: ServerRequest): ServerResponse {
        val kunden = service.findAll()
            .map { modelAssembler.toModelAndAwait(it, request.exchange()) }

        return ok().contentType(TEXT_EVENT_STREAM).bodyAndAwait(kunden)
    }
}
