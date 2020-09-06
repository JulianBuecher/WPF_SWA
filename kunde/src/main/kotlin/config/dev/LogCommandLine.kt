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
package com.acme.kunde.config.dev

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Base64.getEncoder

/**
 * Konfigurationsklasse mit Bean-Funktionen, die über einen _CommandLineRunner_ Informationen für die Entwickler/innen
 * protokollieren. Bei der Verwendung von `context.initializer.classes` statt `addInitializers(beans)` werden diese
 * Bean-Funktionen jedoch oft aufgerufen, wenn sie nicht in einer Klasse zusammengefasst, sondern eigenständige
 * Funktionen sind.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
class LogCommandLine {
    /**
     * Bean-Definition, um einen CommandLineRunner bereitzustellen, der verschiedene Verschlüsselungsverfahren anwendet
     * @return CommandLineRunner für die Ausgabe
     */
    @Bean
    fun logPasswordEncoding(passwordEncoder: PasswordEncoder, @Value("\${kunde.password}") password: String) =
        CommandLineRunner {
            // scrypt und Argon2 benoetigen BouncyCastle
            val verschluesselt = passwordEncoder.encode(password)
            logger.warn { "bcrypt mit \"$password\":   $verschluesselt" }
        }

    /**
     * Bean-Definition, um einen CommandLineRunner  bereitzustellen, der verschiedene Benutzerkennungen für
     * _BASIC_-Authentifizierung codiert.
     * @return CommandLineRunner
     */
    @Bean
    fun logBasicAuth(
        @Value("\${kunde.password}") password: String,
        @Value("\${kunde.password-falsch}") passwordFalsch: String,
    ) = CommandLineRunner {
        val usernameAdmin = "admin"
        val usernameAlpha1 = "alpha1"
        val charset = charset("ISO-8859-1")

        var input = "$usernameAdmin:$password".toByteArray(charset)
        var encoded = "Basic ${getEncoder().encodeToString(input)}"
        logger.warn { "BASIC Authentication \"$usernameAdmin:$password\" -> $encoded" }
        input = "$usernameAdmin:$passwordFalsch".toByteArray(charset)
        encoded = "Basic ${getEncoder().encodeToString(input)}"
        logger.warn { "BASIC Authentication \"$usernameAdmin:$passwordFalsch\" -> $encoded" }
        input = "$usernameAlpha1:$password".toByteArray(charset)
        encoded = "Basic ${getEncoder().encodeToString(input)}"
        logger.warn { "BASIC Authentication \"$usernameAlpha1:$password\" -> $encoded" }
    }

    private companion object {
        val logger = KotlinLogging.logger {}
    }
}
