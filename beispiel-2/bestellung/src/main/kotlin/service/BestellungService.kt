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
package com.acme.bestellung.service

import com.acme.bestellung.entity.Bestellung
import com.acme.bestellung.entity.Kunde
import com.acme.bestellung.entity.KundeId
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidatorFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import mu.KotlinLogging
import org.springframework.context.annotation.Lazy
import org.springframework.data.mongodb.core.ReactiveFluentMongoOperations
import org.springframework.data.mongodb.core.awaitOneOrNull
import org.springframework.data.mongodb.core.flow
import org.springframework.data.mongodb.core.insert
import org.springframework.data.mongodb.core.oneAndAwait
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.net.http.HttpHeaders
import java.net.http.HttpRequest

/**
 * Anwendungslogik für Bestellungen.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Service
class BestellungService(
    private val mongo: ReactiveFluentMongoOperations,
    @Lazy private val validatorFactory: ValidatorFactory,
    // org.springframework.web.reactive.function.client.DefaultWebClientBuilder
    // org.springframework.web.reactive.function.client.DefaultWebClient
    @Lazy private val clientBuilder: WebClient.Builder,
) {
    private val validator by lazy { validatorFactory.validator }

    /**
     * Alle Bestellungen ermitteln.
     * @return Alle Bestellungen.
     */
    suspend fun findAll(token: String): Flow<Bestellung> = mongo.query<Bestellung>()
        .flow()
        .onEach { bestellung ->
            logger.debug { "findAll: $bestellung" }
            val kunde = findKundeById(bestellung.kundeId,token)
            bestellung.kundeNachname = kunde.nachname
        }

    /**
     * Eine Bestellung anhand der ID suchen.
     * @param id Die Id der gesuchten Bestellung.
     * @return Die gefundene Bestellung oder null.
     */
    suspend fun findById(id: String,token: String): Bestellung? {
        val bestellung = mongo.query<Bestellung>()
            .matching(query(Bestellung::id isEqualTo id))
            .awaitOneOrNull()
        logger.debug { "findById: $bestellung" }
        if (bestellung == null) {
            return bestellung
        }

        val (nachname) = findKundeById(bestellung.kundeId,token)
        return bestellung.apply { kundeNachname = nachname }
    }

    /**
     * Kunde anhand der Kunde-ID suchen.
     * @param kundeId Die Id des gesuchten Kunden.
     * @return Der gefundene Kunde oder null.
     */
    suspend fun findKundeById(kundeId: KundeId,token: String): Kunde {
        logger.debug { "findKundeById: $kundeId" }

        // org.springframework.web.reactive.function.client.DefaultWebClient
        val client = clientBuilder
            .baseUrl("http://$kundeService:$kundePort")
            // TODO: Use JWT Bearer Token for Authentication to Kunde-Service
//            .filter(basicAuthentication(username, password))
            .defaultHeader("Authorization",token)
            .build()

        return client
            .get()
            .uri("/api/$kundeId")
            .retrieve()
            .awaitBody()
    }

    /**
     * Bestellungen zur Kunde-ID suchen.
     * @param kundeId Die Id des gegebenen Kunden.
     * @return Die gefundenen Bestellungen oder ein leeres Flux-Objekt.
     */
    suspend fun findByKundeId(kundeId: KundeId,token: String): Flow<Bestellung> {
        val (nachname) = findKundeById(kundeId,token)

        val criteria = where(Bestellung::kundeId).regex("\\.*$kundeId\\.*", "i")
        return mongo.query<Bestellung>().matching(Query(criteria))
            .flow()
            .onEach { bestellung ->
                logger.debug { "findByKundeId(): $bestellung" }
                bestellung.kundeNachname = nachname
            }
    }

    /**
     * Eine neue Bestellung anlegen.
     * @param bestellung Das Objekt der neu anzulegenden Bestellung.
     * @return Die neu angelegte Bestellung mit generierter ID.
     */
    suspend fun create(bestellung: Bestellung): CreateResult {
        logger.debug { "create(): $bestellung" }
        val violations = validator.validate(bestellung)
        if (violations.isNotEmpty()) {
            return CreateResult.ConstraintViolations(violations)
        }

        val neueBestellung = mongo.insert<Bestellung>().oneAndAwait(bestellung)
        return CreateResult.Success(neueBestellung)
    }

    companion object {
        /**
         * Rechnername des Kunde-Service durch _Service Registry_ von Kubernetes (und damit Istio).
         */
        // https://github.com/istio/istio/blob/master/samples/bookinfo/src/reviews/reviews-application/src/main/java/application/rest/LibertyRestEndpoint.java#L43
//        val kundeService = System.getenv("KUNDE_HOSTNAME") ?: "kunde"
        val kundeService = System.getenv("KUNDE_HOSTNAME") ?: "localhost"

        /**
         * Port des Kunde-Service durch _Service Registry_ von Kubernetes (und damit Istio).
         */
        val kundePort = System.getenv("KUNDE_SERVICE_PORT") ?: "8080"

        private const val username = "admin"
        private const val password = "p"
        private val logger = KotlinLogging.logger {}
    }
}

/**
 * Resultat-Typ für [BestellungService.create]
 */
sealed class CreateResult {
    /**
     * Resultat-Typ, wenn eine neue Bestellung erfolgreich angelegt wurde.
     * @property bestellung Die neu angelegte Bestellung
     */
    data class Success(val bestellung: Bestellung) : CreateResult()

    /**
     * Resultat-Typ, wenn eine Bestellung wegen Constraint-Verletzungen nicht angelegt wurde.
     * @property violations Die verletzten Constraints
     */
    data class ConstraintViolations(val violations: Set<ConstraintViolation<Bestellung>>) : CreateResult()
}
