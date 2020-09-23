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

import com.acme.bestellung.entity.BestellungId
import org.springframework.http.HttpHeaders
import java.net.URI

/**
 * Basis-URI ermitteln, d.h. ohne angehängten Pfad-Parameter für die ID und ohne Query-Parameter
 * @param headers Header-Daten des Request-Objekts
 * @param uri URI zum eingegangenen Request
 * @param id Eine Bestellung-ID oder null als Defaultwert
 * @return Die Basis-URI als String
 */
fun getBaseUri(headers: HttpHeaders, uri: URI, id: BestellungId? = null): String {
    var baseUri = uri.toString().substringBefore('?').removeSuffix("/")
    if (id != null) {
        baseUri = baseUri.removeSuffix("/$id")
    }

    // Forwarding durch Istio ?
    val forwardedPath = headers.getFirst("x-envoy-original-path")
    if (forwardedPath != null) {
        baseUri = baseUri.replaceAfterLast('/', forwardedPath.substring(1).removeSuffix("/$id"))
    }

    return baseUri
}
