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
@file:Suppress("PackageDirectoryMismatch")

package com.acme.kunde.rest

import com.acme.kunde.Router.Companion.apiPath
import com.acme.kunde.config.Settings.DEV
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE.JAVA_11
import org.junit.jupiter.api.condition.JRE.JAVA_15
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange

@Tag("streamingRest")
@DisplayName("Streaming fuer Kunden testen")
@ExtendWith(SoftAssertionsExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles(DEV)
@EnabledForJreRange(min = JAVA_11, max = JAVA_15)
class KundeStreamingRestTest(@LocalServerPort private val port: Int, ctx: ReactiveWebApplicationContext) {
    private val baseUrl = "$SCHEMA://$HOST:$port$apiPath"
    private val client = WebClient.builder()
        .filter(basicAuthentication(USERNAME, PASSWORD))
        .baseUrl(baseUrl)
        .build()

    init {
        assertThat(ctx.getBean<KundeStreamHandler>()).isNotNull
    }

    @Test
    fun `Streaming mit allen Kunden`(softly: SoftAssertions) = runBlocking<Unit> {
        // act
        val response = client.get()
            .header(ACCEPT, TEXT_EVENT_STREAM.toString())
            .awaitExchange()

        // assert
        with(softly) {
            assertThat(response.statusCode()).isEqualTo(OK)
            val body = response.awaitBody<String>()
            assertThat(body).startsWith("data:")

            // TODO List<Kunde> durch ObjectMapper von Jackson
        }
    }

    private companion object {
        const val SCHEMA = "https"
        val HOST: String? = System.getenv("COMPUTERNAME") ?: System.getenv("HOSTNAME")
        const val USERNAME = "admin"
        const val PASSWORD = "p"
    }
}
