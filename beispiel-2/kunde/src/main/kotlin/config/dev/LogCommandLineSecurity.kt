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
package com.acme.kunde.config.dev

import mu.KotlinLogging
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import java.security.Security

/**
 * Konfigurationsklasse mit Bean-Funktionen, die über einen _CommandLineRunner_ Informationen für die Entwickler/innen
 * im Hinblick auf Security (-Algorithmen) zu protokollieren. Da es viele Algorithmen gibt und die Ausgabe lang wird,
 * sollte diese Klasse nur mit einem bestimmten Profile und nicht allgemein verwendet werden.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
class LogCommandLineSecurity {
    /**
     * Bean-Definition, um einen CommandLineRunner bereitzustellen, damit die im JDK vorhandenen _Signature_-Algorithmen
     * aufgelistet werden.
     * @return CommandLineRunner
     */
    @Bean
    fun logSignatureAlgorithms() = CommandLineRunner {
        val logger = KotlinLogging.logger {}
        Security.getProviders().forEach { provider ->
            provider.services.forEach { service ->
                if (service.type == "Signature") {
                    logger.warn { service.algorithm }
                }
            }
        }
    }
}
