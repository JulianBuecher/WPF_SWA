/*
 * Copyright (C) 2018 - present Juergen Zimmermann, Hochschule Karlsruhe
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
package com.acme.bestellung.config.dev

import com.acme.bestellung.config.Settings.DEV
import com.acme.bestellung.entity.KundeId
import com.acme.bestellung.service.BestellungService
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction


/**
 * Den Microservice _kunde_ mit WebClient aufrufen.
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface LogWebClientKunde {
    /**
     * Bean-Definition, um einen CommandLineRunner für das Profil "dev" bereitzustellen, damit der Microservice _kunde_
     * mit WebClient aufgerufen wird.
     * @param bestellungService BestellungService mit WebClient Builder
     * @return CommandLineRunner
     */
    @Bean
    @Profile(DEV)
    @Suppress("LongMethod")
    fun logWebClientKunde(bestellungService: BestellungService) = CommandLineRunner {
        val logger = KotlinLogging.logger {}

        runBlocking {
            val kundeId = KundeId.fromString("00000000-0000-0000-0000-000000000001")
            // TODO: Hier muss noch eine Token-Weiterleitung eingebaut werden (für was auch immer :D)
//            val token = oAuthToken().toString()
//            val kunde = bestellungService.findKundeById(kundeId,token)
//            logger.warn { "Kunde zur ID $kundeId: $kunde" }
        }
    }

    // Fuer OAuth siehe
    // https://github.com/bclozel/spring-reactive-university/blob/master/src/main/java/com/example/integration/...
    //      ...gitter/GitterClient.java
    private fun oAuthToken(token: String): ExchangeFilterFunction? {
        return ExchangeFilterFunction { clientRequest: ClientRequest, exchangeFunction: ExchangeFunction ->
            exchangeFunction
                .exchange(
                    ClientRequest
                        .from(clientRequest)
                        .header("Authorization", "Bearer $token")
                        .build()
                )
        }
    }
}
