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

import com.acme.kunde.Router.Companion.apiPath
import com.acme.kunde.entity.KundeId
import org.springframework.http.HttpHeaders
import java.net.URI

/**
 * Basis-URI ermitteln, d.h. ohne angehängten Pfad-Parameter für die ID und ohne Query-Parameter
 * @param headers Header-Daten des Request-Objekts
 * @param uri URI zum eingegangenen Request
 * @param id Eine Kunde-ID oder null als Defaultwert
 * @return Die Basis-URI als String
 */
fun getBaseUri(headers: HttpHeaders, uri: URI, id: KundeId? = null): String {
    val forwardedHost = headers.getFirst("x-forwarded-host")

    return if (forwardedHost == null) {
        // KEIN Forwarding von einem API-Gateway
        val baseUri = uri.toString().substringBefore('?').removeSuffix("/")
        if (id == null) {
            baseUri
        } else {
            baseUri.removeSuffix("/$id")
        }
    } else {
        // x-forwarded-proto: "https"
        // x-forwarded-host: "localhost:8443"
        // x-forwarded-prefix: "/kunden"
        val forwardedProto = headers.getFirst("x-forwarded-proto")
        val forwardedPrefix = headers.getFirst("x-forwarded-prefix")
        "$forwardedProto://$forwardedHost$forwardedPrefix$apiPath"
    }
}
