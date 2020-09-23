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
import com.acme.kunde.config.security.CustomUser
import com.acme.kunde.entity.Adresse
import com.acme.kunde.entity.GeschlechtType.WEIBLICH
import com.acme.kunde.entity.InteresseType.LESEN
import com.acme.kunde.entity.InteresseType.REISEN
import com.acme.kunde.entity.InteresseType.SPORT
import com.acme.kunde.entity.Kunde
import com.acme.kunde.entity.Kunde.Companion.ID_PATTERN
import com.acme.kunde.entity.KundeId
import com.acme.kunde.entity.Umsatz
import com.acme.kunde.rest.patch.PatchOperation
import com.jayway.jsonpath.JsonPath
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE.JAVA_11
import org.junit.jupiter.api.condition.JRE.JAVA_15
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.aggregator.ArgumentsAccessor
import org.junit.jupiter.params.aggregator.get
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.MediaTypes.HAL_JSON
import org.springframework.hateoas.mediatype.hal.HalLinkDiscoverer
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.NOT_MODIFIED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.PRECONDITION_REQUIRED
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.bodyToFlow
import java.math.BigDecimal.ONE
import java.net.URL
import java.time.LocalDate
import java.util.Currency

// https://junit.org/junit5/docs/current/user-guide
// https://assertj.github.io/doc

@Tag("rest")
@DisplayName("REST-Schnittstelle fuer Kunden testen")
@ExtendWith(SoftAssertionsExtension::class)
// Alternative zu @ContextConfiguration von Spring
// Default: webEnvironment = MOCK, d.h.
//          Mocking mit ReactiveWebApplicationContext anstatt z.B. Netty oder Tomcat
@SpringBootTest(webEnvironment = RANDOM_PORT)
// @SpringBootTest(webEnvironment = DEFINED_PORT, ...)
// ggf.: @DirtiesContext, falls z.B. ein Spring Bean modifiziert wurde
@ActiveProfiles(DEV)
@EnabledForJreRange(min = JAVA_11, max = JAVA_15)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@Suppress("ClassName", "HasPlatformType")
class KundeRestTest(@LocalServerPort private val port: Int, ctx: ReactiveWebApplicationContext) {
    private val baseUrl = "$SCHEMA://$HOST:$port$apiPath"

    // WebClient auf der Basis von "Reactor Netty"
    // Alternative: Http Client von Java http://openjdk.java.net/groups/net/httpclient/intro.html
    // TODO https://github.com/spring-projects/spring-hateoas/issues/1225
    // https://github.com/spring-projects/spring-hateoas/commit/904a03a241a14bd03cb8e3cb01fdbf5f1bb95355
    private val client = WebClient.builder()
        .filter(basicAuthentication(USERNAME_ADMIN, PASSWORD))
        .baseUrl(baseUrl)
        .build()

    init {
        assertThat(ctx.getBean<KundeHandler>()).isNotNull
    }

    @Test
    @Order(100)
    fun `Immer erfolgreich`() {
        @Suppress("UsePropertyAccessSyntax")
        assertThat(true).isTrue()
    }

    @Test
    @Disabled("Noch nicht fertig")
    @Order(200)
    fun `Noch nicht fertig`() {
        @Suppress("UsePropertyAccessSyntax")
        assertThat(true).isFalse()
    }

    // -------------------------------------------------------------------------
    // L E S E N
    // -------------------------------------------------------------------------
    @Nested
    inner class Lesen {
        @Nested
        inner class `Suche anhand der ID` {
            @ParameterizedTest
            @ValueSource(strings = [ID_VORHANDEN, ID_UPDATE_PUT, ID_UPDATE_PATCH])
            @Order(1000)
            fun `Suche mit vorhandener ID`(id: String, softly: SoftAssertions) = runBlocking<Unit> {
                // act
                val response = client.get()
                    .uri(ID_PATH, id)
                    .accept(HAL_JSON)
                    .awaitExchange()

                // assert
                assertThat(response.statusCode()).isEqualTo(OK)
                val content = response.awaitBody<String>()

                // Pact https://docs.pact.io ist eine Alternative zu JsonPath
                with(softly) {
                    val nachname: String = JsonPath.read(content, "$.nachname")
                    assertThat(nachname).isNotBlank
                    val email: String = JsonPath.read(content, "$.email")
                    assertThat(email).isNotBlank
                    val selfLink = HalLinkDiscoverer().findLinkWithRel("self", content).get().href
                    assertThat(selfLink).isEqualTo("$baseUrl/$id")
                }
            }

            @ParameterizedTest
            @CsvSource("$ID_VORHANDEN, 0")
            @Order(1100)
            fun `Suche mit vorhandener ID und vorhandener Version`(id: String, version: String) = runBlocking<Unit> {
                // act
                val response = client.get()
                    .uri(ID_PATH, id)
                    .accept(HAL_JSON)
                    .ifNoneMatch("\"$version\"")
                    .awaitExchange()

                // assert
                assertThat(response.statusCode()).isEqualTo(NOT_MODIFIED)
            }

            @ParameterizedTest
            @CsvSource("$ID_VORHANDEN, xxx")
            @Order(1200)
            fun `Suche mit vorhandener ID und falscher Version`(
                id: String,
                version: String,
                softly: SoftAssertions,
            ) = runBlocking<Unit> {
                // act
                val response = client.get()
                    .uri(ID_PATH, id)
                    .accept(HAL_JSON)
                    .ifNoneMatch("\"$version\"")
                    .awaitExchange()

                // assert
                assertThat(response.statusCode()).isEqualTo(OK)
                val content = response.awaitBody<String>()

                with(softly) {
                    val nachname: String = JsonPath.read(content, "$.nachname")
                    assertThat(nachname).isNotBlank
                    val email: String = JsonPath.read(content, "$.email")
                    assertThat(email).isNotBlank
                    val linkDiscoverer = HalLinkDiscoverer()
                    val selfLink = linkDiscoverer.findLinkWithRel("self", content).get().href
                    assertThat(selfLink).endsWith("/$id")
                }
            }

            @ParameterizedTest
            @ValueSource(strings = [ID_NICHT_VORHANDEN])
            @Order(1300)
            fun `Suche mit nicht-vorhandener ID`(id: String) = runBlocking<Unit> {
                // act
                val response = client.get()
                    .uri(ID_PATH, id)
                    .awaitExchange()

                // assert
                assertThat(response.statusCode()).isEqualTo(NOT_FOUND)
            }

            @ParameterizedTest
            @ValueSource(strings = [ID_NICHT_VORHANDEN])
            @Order(1300)
            fun `Suche mit nicht-vorhandener ID und Rolle kunde`(id: String) = runBlocking<Unit> {
                // arrange
                val clientKunde = WebClient.builder()
                    .filter(basicAuthentication(USERNAME_KUNDE, PASSWORD))
                    .baseUrl(baseUrl)
                    .build()

                // act
                val response = clientKunde.get()
                    .uri(ID_PATH, id)
                    .awaitExchange()

                // assert
                assertThat(response.statusCode()).isEqualTo(FORBIDDEN)
            }

            @ParameterizedTest
            @ValueSource(strings = [ID_INVALID])
            @Order(1300)
            fun `Suche mit syntaktisch ungueltiger ID`(id: String) = runBlocking<Unit> {
                // act
                val response = client.get()
                    .uri(ID_PATH, id)
                    .awaitExchange()

                // assert
                assertThat(response.statusCode()).isEqualTo(NOT_FOUND)
            }

            @ParameterizedTest
            @CsvSource("$USERNAME_ADMIN, $PASSWORD_FALSCH, $ID_VORHANDEN")
            @Order(1400)
            fun `Suche mit ID, aber falschem Passwort`(
                username: String,
                password: String,
                id: String,
            ) = runBlocking<Unit> {
                // arrange
                val clientFalsch = WebClient.builder()
                    .filter(basicAuthentication(username, password))
                    .baseUrl(baseUrl)
                    .build()

                // act
                val response = clientFalsch.get()
                    .uri(ID_PATH, id)
                    .awaitExchange()

                // assert
                assertThat(response.statusCode()).isEqualTo(UNAUTHORIZED)
            }
        }

        @Test
        @Order(2000)
        fun `Suche nach allen Kunden`() = runBlocking<Unit> {
            // act
            val kundenModel = client.get()
                .retrieve()
                .awaitBody<KundenModel>()

            // assert
            assertThat(kundenModel._embedded.kundeList).isNotEmpty
        }

        @ParameterizedTest
        @ValueSource(strings = [NACHNAME])
        @Order(2100)
        fun `Suche mit vorhandenem Nachnamen`(nachname: String, softly: SoftAssertions) = runBlocking<Unit> {
            // arrange
            val nachnameLower = nachname.toLowerCase()

            // act
            val kundenModel = client.get()
                .uri { builder ->
                    builder
                        .path(KUNDE_PATH)
                        .queryParam(NACHNAME_PARAM, nachnameLower)
                        .build()
                }
                .retrieve()
                .awaitBody<KundenModel>()

            // assert
            with(softly) {
                val kundeList = kundenModel._embedded.kundeList
                assertThat(kundeList).isNotEmpty
                kundeList.onEach { kunde ->
                    assertThat(kunde.content?.nachname).isEqualToIgnoringCase(nachnameLower)
                }
            }
        }

        @ParameterizedTest
        @ValueSource(strings = [EMAIL_VORHANDEN])
        @Order(2200)
        fun `Suche mit vorhandener Email`(email: String) = runBlocking<Unit> {
            // act
            val kunde = client.get()
                .uri { builder ->
                    builder
                        .path(KUNDE_PATH)
                        .queryParam(EMAIL_PARAM, email)
                        .build()
                }
                .retrieve()
                .awaitBody<EntityModel<Kunde>>()

            // assert
            assertThat(kunde.content?.email).isEqualToIgnoringCase(email)
        }
    }

    // -------------------------------------------------------------------------
    // S C H R E I B E N
    // -------------------------------------------------------------------------
    @Nested
    inner class Schreiben {
        @Nested
        inner class Erzeugen {
            @ParameterizedTest
            @CsvSource(
                "$NEUER_NACHNAME, $NEUE_EMAIL, $NEUES_GEBURTSDATUM, $CURRENCY_CODE, $NEUE_HOMEPAGE, $NEUE_PLZ, " +
                    "$NEUER_ORT, $NEUER_USERNAME",
            )
            @Order(5000)
            fun `Abspeichern eines neuen Kunden`(args: ArgumentsAccessor, softly: SoftAssertions) = runBlocking<Unit> {
                // arrange
                val neuerKunde = Kunde(
                    id = null,
                    nachname = args.get<String>(0),
                    email = args.get<String>(1),
                    newsletter = true,
                    geburtsdatum = args.get<LocalDate>(2),
                    umsatz = Umsatz(betrag = ONE, waehrung = Currency.getInstance(args.get<String>(3))),
                    homepage = args.get<URL>(4),
                    geschlecht = WEIBLICH,
                    interessen = listOfNotNull(LESEN, REISEN),
                    adresse = Adresse(plz = args.get<String>(5), ort = args.get<String>(6)),
                )
                neuerKunde.user = CustomUser(
                    id = null,
                    username = args.get<String>(7),
                    password = "p",
                    authorities = emptyList(),
                )

                // act
                val response = client.post()
                    .contentType(APPLICATION_JSON)
                    .bodyValue(neuerKunde)
                    .awaitExchange()

                // assert
                val id: String
                with(response) {
                    with(softly) {
                        assertThat(statusCode()).isEqualTo(CREATED)
                        val location = headers().asHttpHeaders().location
                        id = location.toString().substringAfterLast('/')
                        assertThat(id).matches(ID_PATTERN)
                    }
                }

                // Ist der neue Kunde auch wirklich abgespeichert?
                val kundeModel = client.get()
                    .uri(ID_PATH, id)
                    .retrieve()
                    .awaitBody<EntityModel<Kunde>>()
                assertThat(kundeModel.content?.nachname).isEqualTo(neuerKunde.nachname)
            }

            @ParameterizedTest
            @CsvSource(
                "$NEUER_NACHNAME_INVALID, $NEUE_EMAIL_INVALID, $NEUE_KATEGORIE_INVALID, $NEUES_GEBURTSDATUM_INVALID, " +
                    "$NEUE_PLZ_INVALID, $NEUER_ORT",
            )
            @Order(5100)
            fun `Abspeichern eines neuen Kunden mit ungueltigen Werten`(
                args: ArgumentsAccessor,
                softly: SoftAssertions,
            ) = runBlocking {
                // arrange
                val neuerKunde = Kunde(
                    id = null,
                    nachname = args.get<String>(0),
                    email = args.get<String>(1),
                    kategorie = args.get<Int>(2),
                    newsletter = true,
                    geburtsdatum = args.get<LocalDate>(3),
                    geschlecht = WEIBLICH,
                    interessen = listOfNotNull(LESEN, REISEN, REISEN),
                    adresse = Adresse(plz = args.get<String>(4), ort = args.get<String>(5)),
                )

                // act
                val response = client.post()
                    .contentType(APPLICATION_JSON)
                    .bodyValue(neuerKunde)
                    .awaitExchange()

                // assert
                with(response) {
                    with(softly) {
                        assertThat(statusCode()).isEqualTo(BAD_REQUEST)
                        val violationMsgPredicate = { msg: String ->
                            msg.contains("ist nicht 5-stellig") ||
                                msg.contains("Bei Nachnamen ist nach einem") ||
                                msg.contains("Die EMail-Adresse") ||
                                msg.contains("Kategorie") ||
                                msg.contains("Das Geburtsdatum") ||
                                msg.contains("Interessen")
                        }
                        bodyToFlow<KundeConstraintViolation>()
                            .onEach { violation -> assertThat(violation.message).matches(violationMsgPredicate) }
                            .first() // NoSuchElementException bei leerem Flow
                    }
                }
            }

            @ParameterizedTest
            @CsvSource(
                "$NEUER_NACHNAME, $NEUE_EMAIL, 2019-01-30, $CURRENCY_CODE, $NEUE_HOMEPAGE, $NEUE_PLZ, " +
                    "$NEUER_ORT, $NEUER_USERNAME",
            )
            @Order(5200)
            fun `Abspeichern eines neuen Kunden mit vorhandenem Usernamen`(
                args: ArgumentsAccessor,
                softly: SoftAssertions,
            ) = runBlocking<Unit> {
                // arrange
                val neuerKunde = Kunde(
                    id = null,
                    nachname = args.get<String>(0),
                    email = "${args.get<String>(1)}x",
                    newsletter = true,
                    geburtsdatum = args.get<LocalDate>(2),
                    umsatz = Umsatz(betrag = ONE, waehrung = Currency.getInstance(args.get<String>(3))),
                    homepage = args.get<URL>(4),
                    geschlecht = WEIBLICH,
                    interessen = listOfNotNull(LESEN, REISEN),
                    adresse = Adresse(plz = args.get<String>(5), ort = args.get<String>(6)),
                )
                neuerKunde.user = CustomUser(
                    id = null,
                    username = args.get<String>(7),
                    password = "p",
                    authorities = emptyList(),
                )

                // act
                val response = client.post()
                    .contentType(APPLICATION_JSON)
                    .bodyValue(neuerKunde)
                    .awaitExchange()

                // assert
                with(response) {
                    with(softly) {
                        assertThat(statusCode()).isEqualTo(BAD_REQUEST)
                        val body = awaitBody<String>()
                        assertThat(body).contains("Username")
                    }
                }
            }
        }

        @Nested
        inner class Aendern {
            @ParameterizedTest
            @ValueSource(strings = [ID_UPDATE_PUT])
            @Order(6000)
            fun `Aendern eines vorhandenen Kunden durch Put`(id: String) = runBlocking<Unit> {
                // arrange
                val responseOrig = client.get()
                    .uri(ID_PATH, id)
                    .awaitExchange()
                val model = responseOrig.awaitBody<EntityModel<Kunde>>()
                val kundeOrig = model.content
                assertThat(kundeOrig).isNotNull
                kundeOrig as Kunde
                val kunde = kundeOrig.copy(id = KundeId.fromString(id), email = "${kundeOrig.email}.put")

                val etag = responseOrig.headers().asHttpHeaders().eTag
                @Suppress("UsePropertyAccessSyntax")
                assertThat(etag).isNotNull()

                // act
                val response = client.put()
                    .uri(ID_PATH, id)
                    .contentType(APPLICATION_JSON)
                    .header(IF_MATCH, etag)
                    .bodyValue(kunde)
                    .awaitExchange()

                // assert
                assertThat(response.statusCode()).isEqualTo(NO_CONTENT)
                // ggf. noch GET-Request, um die Aenderung zu pruefen
            }

            @ParameterizedTest
            @CsvSource("$ID_UPDATE_PUT, $EMAIL_VORHANDEN", "$ID_UPDATE_PATCH, $EMAIL_VORHANDEN")
            @Order(6100)
            fun `Aendern eines Kunden durch Put und Email existiert`(
                id: String,
                email: String,
                softly: SoftAssertions,
            ) = runBlocking<Unit> {
                // arrange
                val responseOrig = client.get()
                    .uri(ID_PATH, id)
                    .awaitExchange()
                val kundeOrig = responseOrig.awaitBody<Kunde>()
                val kunde = kundeOrig.copy(id = KundeId.fromString(id), email = email)

                val etag = responseOrig.headers().asHttpHeaders().eTag
                @Suppress("UsePropertyAccessSyntax")
                assertThat(etag).isNotNull()
                etag as String
                val version = etag.substring(1, etag.length - 1)
                val versionInt = version.toInt() + 1

                // act
                val response = client.put()
                    .uri(ID_PATH, id)
                    .contentType(APPLICATION_JSON)
                    .header(IF_MATCH, "\"$versionInt\"")
                    .bodyValue(kunde)
                    .awaitExchange()

                // assert
                with(response) {
                    with(softly) {
                        assertThat(statusCode()).isEqualTo(BAD_REQUEST)
                        val body = awaitBody<String>()
                        assertThat(body).contains(email)
                    }
                }
            }

            @ParameterizedTest
            @CsvSource("$ID_UPDATE_PUT, $NEUER_NACHNAME_INVALID, $NEUE_EMAIL_INVALID, $NEUE_KATEGORIE_INVALID")
            @Order(6200)
            fun `Aendern eines Kunden durch Put mit ungueltigen Daten`(
                id: String,
                nachname: String,
                email: String,
                kategorie: Int,
                softly: SoftAssertions,
            ) = runBlocking {
                // arrange
                val responseOrig = client.get()
                    .uri(ID_PATH, id)
                    .awaitExchange()
                val kundeOrig = responseOrig.awaitBody<Kunde>()
                val kunde = kundeOrig.copy(
                    id = KundeId.fromString(id),
                    nachname = nachname,
                    email = email,
                    kategorie = kategorie,
                )

                val etag = responseOrig.headers().asHttpHeaders().eTag
                assertThat(etag).isNotNull()
                etag as String
                val version = etag.substring(1, etag.length - 1)
                val versionInt = version.toInt() + 1

                // act
                val response = client.put()
                    .uri(ID_PATH, id)
                    .contentType(APPLICATION_JSON)
                    .header(IF_MATCH, "\"$versionInt\"")
                    .bodyValue(kunde)
                    .awaitExchange()

                // assert
                with(response) {
                    with(softly) {
                        assertThat(statusCode()).isEqualTo(BAD_REQUEST)
                        val violationMsgPredicate = { msg: String ->
                            msg.contains("Nachname") ||
                                msg.contains("EMail-Adresse") ||
                                msg.contains("Kategorie")
                        }
                        val count = bodyToFlow<KundeConstraintViolation>()
                            .onEach { violation -> assertThat(violation.message).matches(violationMsgPredicate) }
                            .count()
                        assertThat(count).isEqualTo(2)
                    }
                }
            }

            @ParameterizedTest
            @ValueSource(strings = [ID_VORHANDEN, ID_UPDATE_PUT, ID_UPDATE_PATCH])
            @Order(6300)
            fun `Aendern eines Kunden durch Put ohne Version`(id: String, softly: SoftAssertions) = runBlocking<Unit> {
                val responseOrig = client.get()
                    .uri(ID_PATH, id)
                    .awaitExchange()
                val kunde = responseOrig.awaitBody<Kunde>()

                // act
                val response = client.put()
                    .uri(ID_PATH, id)
                    .contentType(APPLICATION_JSON)
                    .bodyValue(kunde)
                    .awaitExchange()

                // assert
                with(response) {
                    with(softly) {
                        assertThat(statusCode()).isEqualTo(PRECONDITION_REQUIRED)
                        val body = awaitBody<String>()
                        assertThat(body).contains("Versionsnummer")
                    }
                }
            }

            @ParameterizedTest
            @CsvSource("$ID_UPDATE_PATCH, $NEUE_EMAIL")
            @Order(7000)
            fun `Aendern eines vorhandenen Kunden durch Patch`(id: String, email: String) = runBlocking<Unit> {
                // arrange
                val replaceOp = PatchOperation(
                    op = "replace",
                    path = "/email",
                    value = "$email.patch",
                )
                val addOp = PatchOperation(
                    op = "add",
                    path = "/interessen",
                    value = NEUES_INTERESSE.value,
                )
                val removeOp = PatchOperation(
                    op = "remove",
                    path = "/interessen",
                    value = ZU_LOESCHENDES_INTERESSE.value,
                )
                val operations = listOfNotNull(replaceOp, addOp, removeOp)

                val responseOrig = client.get()
                    .uri(ID_PATH, id)
                    .awaitExchange()
                val etag = responseOrig.headers().asHttpHeaders().eTag
                assertThat(etag).isNotNull()

                // act
                val response = client.patch()
                    .uri(ID_PATH, id)
                    .contentType(APPLICATION_JSON)
                    .header(IF_MATCH, etag)
                    .bodyValue(operations)
                    .awaitExchange()

                // assert
                assertThat(response.statusCode()).isEqualTo(NO_CONTENT)
                // ggf. noch GET-Request, um die Aenderung zu pruefen
            }

            @ParameterizedTest
            @CsvSource("$ID_UPDATE_PATCH, $NEUE_EMAIL_INVALID")
            @Order(7100)
            fun `Aendern eines Kunden durch Patch mit ungueltigen Daten`(
                id: String,
                email: String,
                softly: SoftAssertions,
            ) = runBlocking<Unit> {
                // arrange
                val replaceOp = PatchOperation(
                    op = "replace",
                    path = "/email",
                    value = email,
                )
                val operations = listOfNotNull(replaceOp)

                val responseOrig = client.get()
                    .uri(ID_PATH, id)
                    .awaitExchange()
                val etag = responseOrig.headers().asHttpHeaders().eTag
                @Suppress("UsePropertyAccessSyntax")
                assertThat(etag).isNotNull()
                etag as String
                val version = etag.substring(1, etag.length - 1)
                val versionInt = version.toInt() + 1

                // act
                val response = client.patch()
                    .uri(ID_PATH, id)
                    .contentType(APPLICATION_JSON)
                    .header(IF_MATCH, "\"$versionInt\"")
                    .bodyValue(operations)
                    .awaitExchange()

                // assert
                with(response) {
                    with(softly) {
                        assertThat(statusCode()).isEqualTo(BAD_REQUEST)
                        val count = bodyToFlow<KundeConstraintViolation>()
                            .onEach { violation -> assertThat(violation.message).contains("EMail-Adresse") }
                            .count()
                        assertThat(count).isOne()
                    }
                }
                // ggf. noch GET-Request, um die Aenderung zu pruefen
            }

            @ParameterizedTest
            @CsvSource(
                "$ID_VORHANDEN, $NEUE_EMAIL_INVALID",
                "$ID_UPDATE_PUT, $NEUE_EMAIL_INVALID",
                "$ID_UPDATE_PATCH, $NEUE_EMAIL_INVALID",
            )
            @Order(7200)
            fun `Aendern eines Kunden durch Patch ohne Versionsnr`(
                id: String,
                email: String,
                softly: SoftAssertions,
            ) = runBlocking<Unit> {
                // arrange
                val replaceOp = PatchOperation(
                    op = "replace",
                    path = "/email",
                    value = "${email}patch",
                )
                val operations = listOfNotNull(replaceOp)

                // act
                val response = client.patch()
                    .uri(ID_PATH, id)
                    .contentType(APPLICATION_JSON)
                    .bodyValue(operations)
                    .awaitExchange()

                // assert
                with(response) {
                    with(softly) {
                        assertThat(statusCode()).isEqualTo(PRECONDITION_REQUIRED)
                        val body = awaitBody<String>()
                        assertThat(body).contains("Versionsnummer")
                    }
                }
            }
        }

        @Nested
        inner class Loeschen {
            @ParameterizedTest
            @ValueSource(strings = [ID_DELETE])
            @Order(8000)
            fun `Loeschen eines vorhandenen Kunden mit der ID`(id: String) = runBlocking<Unit> {
                // act
                val response = client.delete()
                    .uri(ID_PATH, id)
                    .awaitExchange()

                // assert
                assertThat(response.statusCode()).isEqualTo(NO_CONTENT)
            }

            @ParameterizedTest
            @ValueSource(strings = [EMAIL_DELETE])
            @Order(8100)
            fun `Loeschen eines vorhandenen Kunden mit Emailadresse`(email: String) = runBlocking<Unit> {
                // act
                val response = client.delete()
                    .uri { builder ->
                        builder
                            .path(KUNDE_PATH)
                            .queryParam(EMAIL_PARAM, email)
                            .build()
                    }
                    .awaitExchange()

                // assert
                assertThat(response.statusCode()).isEqualTo(NO_CONTENT)
            }

            @ParameterizedTest
            @ValueSource(strings = [EMAIL_DELETE])
            @Order(8200)
            fun `Loeschen mit nicht-vorhandener Emailadresse`(email: String) = runBlocking<Unit> {
                // act
                val response = client.delete()
                    .uri { builder ->
                        builder
                            .path(KUNDE_PATH)
                            .queryParam(EMAIL_PARAM, "${email}xxxx")
                            .build()
                    }
                    .awaitExchange()

                // assert
                assertThat(response.statusCode()).isEqualTo(NO_CONTENT)
            }

            @Test
            fun `Loeschen ohne Emailadresse`() = runBlocking<Unit> {
                // act
                val response = client.delete()
                    .uri { builder ->
                        builder
                            .path(KUNDE_PATH)
                            .queryParam(EMAIL_PARAM, null)
                            .build()
                    }
                    .awaitExchange()

                // assert
                assertThat(response.statusCode()).isEqualTo(NO_CONTENT)
            }
        }
    }

    private companion object {
        const val SCHEMA = "https"
        val HOST: String? = System.getenv("COMPUTERNAME") ?: System.getenv("HOSTNAME")
        const val KUNDE_PATH = "/"
        const val ID_PATH = "/{id}"
        const val NACHNAME_PARAM = "nachname"
        const val EMAIL_PARAM = "email"

        const val USERNAME_ADMIN = "admin"
        const val USERNAME_KUNDE = "alpha1"
        const val PASSWORD = "p"
        const val PASSWORD_FALSCH = "Falsches Passwort!"

        const val ID_VORHANDEN = "00000000-0000-0000-0000-000000000001"
        const val ID_INVALID = "YYYYYYYY-YYYY-YYYY-YYYY-YYYYYYYYYYYY"
        const val ID_NICHT_VORHANDEN = "99999999-9999-9999-9999-999999999999"
        const val ID_UPDATE_PUT = "00000000-0000-0000-0000-000000000002"
        const val ID_UPDATE_PATCH = "00000000-0000-0000-0000-000000000003"
        const val ID_DELETE = "00000000-0000-0000-0000-000000000004"
        const val EMAIL_VORHANDEN = "alpha@acme.edu"
        const val EMAIL_DELETE = "phi@acme.cn"

        const val NACHNAME = "alpha"

        const val NEUE_PLZ = "12345"
        const val NEUE_PLZ_INVALID = "1234"
        const val NEUER_ORT = "Testort"
        const val NEUER_NACHNAME = "Neuernachname"
        const val NEUER_NACHNAME_INVALID = "?!&NachnameUngueltig"
        const val NEUE_EMAIL = "test@acme.de"
        const val NEUE_EMAIL_INVALID = "email.ungueltig@"
        const val NEUE_KATEGORIE_INVALID = 11
        const val NEUES_GEBURTSDATUM = "2019-01-31"
        const val NEUES_GEBURTSDATUM_INVALID = "3000-01-01"
        const val CURRENCY_CODE = "EUR"
        const val NEUE_HOMEPAGE = "https://test.de"
        const val NEUER_USERNAME = "test"

        val NEUES_INTERESSE = SPORT
        val ZU_LOESCHENDES_INTERESSE = LESEN
    }
}
