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
package com.acme.bestellung.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Spring-Konfiguration für Properties zu _Spring Boot_ `spring.mail.*`.
 *
 * @author Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @constructor Ein Objekt zu den Properties `mail.*` für den Präfix `spring`.
 * @property mail Properties `spring.mail.*`.
 */
@ConfigurationProperties(prefix = "spring")
@Suppress("unused", "UseDataClass")
class MailProps(var mail: Mail = Mail()) {
    /**
     * Properties zu _Spring Boot_ `spring.mail.*`.
     *
     * @author Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
     *
     * @constructor Ein Objekt zu den Properties `host` und `port` für den Präfix `spring.mail`.
     * @property host Property `spring.mail.host`.
     * @property port Property `spring.mail.port`.
     */
    @Suppress("UseDataClass")
    class Mail(
        val host: String = "localhost",

        @Suppress("MemberVisibilityCanBePrivate")
        val port: Int = PORT,
    ) {
        private companion object {
            @Suppress("UnderscoresInNumericLiterals")
            const val PORT = 5025
        }
    }
}
