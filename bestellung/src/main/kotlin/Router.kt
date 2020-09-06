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
package com.acme.bestellung

import com.acme.bestellung.entity.Bestellung
import com.acme.bestellung.rest.BestellungHandler
import org.springframework.context.annotation.Bean
import org.springframework.hateoas.MediaTypes.HAL_JSON
import org.springframework.web.reactive.function.server.coRouter

/**
 * Spring-Konfiguration mit der Router-Function für die REST-Schnittstelle.
 *
 * @author Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface Router {
    /**
     * Bean-Function, um das Routing mit _Spring WebFlux_ funktional zu
     * konfigurieren.
     *
     * @param handler Objekt der Handler-Klasse [BestellungHandler] zur
     *      Behandlung von Requests.
     * @return Die konfigurierte Router-Function.
     */
    @Bean
    fun router(handler: BestellungHandler) = coRouter {
        val idPathPattern = "{$idPathVar:${Bestellung.ID_PATTERN}}"

        accept(HAL_JSON).nest {
            GET(apiPath, handler::find)
            GET("$apiPath/$idPathPattern", handler::findById)
        }

        POST(apiPath, handler::create)
    }

    companion object {
        /**
         * Basis-Pfad der REST-Schnittstelle.
         * const: "compile time constant"
         */
        const val apiPath = "/api"

        /**
         * Name der Pfadvariablen für IDs.
         */
        const val idPathVar = "id"
    }
}
