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
package com.acme.kunde.service

import com.acme.kunde.config.security.CustomUser
import com.acme.kunde.config.security.CustomUserDetailsService
import com.acme.kunde.config.security.Rolle
import com.acme.kunde.config.security.findByUsernameAndAwait
import com.acme.kunde.db.CriteriaUtil.getCriteria
import com.acme.kunde.entity.Kunde
import com.acme.kunde.entity.KundeId
import com.acme.kunde.mail.Mailer
import com.acme.kunde.mail.SendResult
import jakarta.validation.ValidatorFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import org.springframework.context.annotation.Lazy
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.mongodb.core.ReactiveFluentMongoOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.allAndAwait
import org.springframework.data.mongodb.core.awaitExists
import org.springframework.data.mongodb.core.awaitOneOrNull
import org.springframework.data.mongodb.core.flow
import org.springframework.data.mongodb.core.insert
import org.springframework.data.mongodb.core.oneAndAwait
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import kotlin.reflect.full.isSubclassOf
import com.acme.kunde.config.security.CreateResult as CreateUserResult

@Suppress("TooManyFunctions")
/**
 * Anwendungslogik für Kunden.
 *
 * [Klassendiagramm](../../images/KundeService.svg)
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Service
class KundeService(
    // Annotation im zugehoerigen Parameter des Java-Konstruktors
    private val mongo: ReactiveFluentMongoOperations,
    @Lazy private val validatorFactory: ValidatorFactory,
    @Lazy private val userService: CustomUserDetailsService,
    @Lazy private val mailer: Mailer,
) {
    private val validator by lazy { validatorFactory.validator }

    /**
     * Einen Kunden anhand seiner ID suchen.
     * @param id Die Id des gesuchten Kunden.
     * @param username Der username beim Login
     * @return Der gefundene Kunde oder null.
     */
    suspend fun findById(id: KundeId, username: String): FindByIdResult {
        val kunde = findById(id)

        if (kunde != null && kunde.username == username) {
            return FindByIdResult.Success(kunde)
        }

        // es muss ein Objekt der Klasse UserDetails geben, weil der Benutzername beim Einloggen verwendet wurde
        val userDetails = userService.findByUsernameAndAwait(username) ?: return FindByIdResult.AccessForbidden()
        val rollen = userDetails
            .authorities
            .map { grantedAuthority -> grantedAuthority.authority }

        return if (!rollen.contains(Rolle.adminStr)) {
            FindByIdResult.AccessForbidden(rollen)
        } else if (kunde == null) {
            FindByIdResult.NotFound
        } else {
            FindByIdResult.Success(kunde)
        }
    }

    private suspend fun findById(id: KundeId): Kunde? {
        // ggf. TimeoutCancellationException
        val kunde = withTimeout(timeoutShort) {
            // https://github.com/spring-projects/spring-data-examples/tree/master/mongodb/fluent-api
            mongo.query<Kunde>()
                .matching(Kunde::id isEqualTo id)
                .awaitOneOrNull()
        }
        logger.debug { "findById: $kunde" }
        return kunde
    }

    /**
     * Alle Kunden ermitteln.
     * @return Alle Kunden
     */
    suspend fun findAll() = withTimeout(timeoutShort) {
        mongo.query<Kunde>()
            .flow()
            .onEach { logger.debug { "findall(): $it" } }
    }

    /**
     * Kunden anhand von Suchkriterien ermitteln.
     * @param queryParams Suchkriterien.
     * @return Gefundene Kunden.
     */
    suspend fun find(queryParams: MultiValueMap<String, String>): Flow<Kunde> {
        logger.debug { "find(): queryParams=$queryParams" }

        if (queryParams.isEmpty()) {
            return findAll()
        }

        if (queryParams.size == 1) {
            val property = queryParams.keys.first()
            val propertyValues = queryParams[property]
            return find(property, propertyValues)
        }

        val criteria = getCriteria(queryParams)
        if (criteria.contains(null)) {
            return emptyFlow()
        }

        val query = Query()
        criteria.filterNotNull()
            .forEach { query.addCriteria(it) }
        logger.debug { query }

        // http://www.baeldung.com/spring-data-mongodb-tutorial
        return withTimeout(timeoutLong) {
            mongo.query<Kunde>()
                .matching(query)
                .flow()
                .onEach { kunde -> logger.debug { "find: $kunde" } }
        }
    }

    private suspend fun find(property: String, propertyValues: List<String>?): Flow<Kunde> {
        val criteria = getCriteria(property, propertyValues) ?: return emptyFlow()
        return withTimeout(timeoutLong) {
            mongo.query<Kunde>()
                .matching(criteria)
                .flow()
                .onEach { kunde -> logger.debug { "find: $kunde" } }
        }
    }

    /**
     * Einen neuen Kunden anlegen.
     * @param kunde Das Objekt des neu anzulegenden Kunden.
     * @return Der neu angelegte Kunde mit generierter ID.
     */
    // FIXME suspend-Funktionen mit @Transactional https://github.com/spring-projects/spring-framework/issues/23575
    // @Transactional
    @Suppress("ReturnCount")
    suspend fun create(kunde: Kunde): CreateResult {
        val violations = validator.validate(kunde)
        if (violations.isNotEmpty()) {
            return CreateResult.ConstraintViolations(violations)
        }

        val user = kunde.user ?: return CreateResult.InvalidAccount

        val email = kunde.email
        if (emailExists(email)) {
            return CreateResult.EmailExists(email)
        }
        val createResult = create(user, kunde)
        if (createResult is CreateResult.UsernameExists) {
            return createResult
        }

        createResult as CreateResult.Success
        if (mailer.send(createResult.kunde) is SendResult.Success) {
            logger.debug { "Email gesendet" }
        } else {
            // TODO Exception analysieren und evtl. erneutes Senden der Email
            logger.warn { "Email nicht gesendet: Ist der Mailserver erreichbar?" }
        }
        return createResult
    }

    private suspend fun create(user: CustomUser, kunde: Kunde): CreateResult {
        // CustomUser ist keine "data class", deshalb kein copy()
        val neuerUser = CustomUser(
            id = null,
            username = user.username,
            password = user.password,
            authorities = listOfNotNull(SimpleGrantedAuthority("ROLE_KUNDE")),
        )

        val customUserCreated = withTimeout(timeoutShort) {
            userService.create(neuerUser)
        }
        if (customUserCreated is CreateUserResult.UsernameExists) {
            return CreateResult.UsernameExists(neuerUser.username)
        }

        customUserCreated as CreateUserResult.Success
        val kundeDb = create(kunde, customUserCreated.user)
        return CreateResult.Success(kundeDb)
    }

    private suspend fun emailExists(email: String) = withTimeout(timeoutShort) {
        mongo.query<Kunde>()
            .matching(Kunde::email isEqualTo email)
            .awaitExists()
    }

    private suspend fun create(kunde: Kunde, user: CustomUser): Kunde {
        val neuerKunde = kunde.copy(username = user.username)
        neuerKunde.user = user
        logger.trace { "Kunde mit user: $kunde" }

        val kundeDb = withTimeout(timeoutShort) { mongo.insert<Kunde>().oneAndAwait(neuerKunde) }
        checkNotNull(kundeDb) { "Fehler beim Neuanlegen von Kunde und CustomUser" }

        return kundeDb
    }

    /**
     * Einen vorhandenen Kunden aktualisieren.
     * @param kunde Das Objekt mit den neuen Daten.
     * @param id ID des Kunden.
     * @param versionStr Versionsnummer.
     * @return Der aktualisierte Kunde oder null, falls es keinen Kunden mit der angegebenen ID gibt.
     */
    @Suppress("KDocUnresolvedReference")
    suspend fun update(kunde: Kunde, id: KundeId, versionStr: String): UpdateResult {
        val violations = validator.validate(kunde)
        if (violations.isNotEmpty()) {
            return UpdateResult.ConstraintViolations(violations)
        }

        val kundeDb = findById(id) ?: return UpdateResult.NotFound

        logger.trace { "update: version=$versionStr, kundeDb=$kundeDb" }
        val version = versionStr.toIntOrNull() ?: return UpdateResult.VersionInvalid(versionStr)

        val email = kunde.email
        return if (emailExists(kundeDb, email)) {
            UpdateResult.EmailExists(email)
        } else {
            update(kunde, kundeDb, version)
        }
    }

    private suspend fun emailExists(kundeDb: Kunde, neueEmail: String): Boolean {
        // Hat sich die Emailadresse ueberhaupt geaendert?
        if (kundeDb.email == neueEmail) {
            logger.trace { "emailExists: Email nicht geaendert: $neueEmail" }
            return false
        }

        logger.trace { "Email geaendert: ${kundeDb.email} -> $neueEmail" }
        // Gibt es die neue Emailadresse bei einem existierenden Kunden?
        return emailExists(neueEmail)
    }

    private suspend fun update(kunde: Kunde, kundeDb: Kunde, version: Int): UpdateResult {
        check(mongo::class.isSubclassOf(ReactiveMongoTemplate::class)) {
            "MongoOperations ist nicht MongoTemplate oder davon abgeleitet: ${mongo::class.java.name}"
        }
        mongo as ReactiveMongoTemplate
        val kundeCache: MutableCollection<*> = mongo.converter.mappingContext.persistentEntities
        // das DB-Objekt aus dem Cache von Spring Data MongoDB entfernen: sonst doppelte IDs
        // Typecast: sonst gibt es bei remove Probleme mit "Type Inference" infolge von "Type Erasure"
        kundeCache.remove(kundeDb)

        val neuerKunde = kunde.copy(id = kundeDb.id, version = version)
        logger.trace { "update: neuerKunde= $neuerKunde" }
        // ggf. OptimisticLockingFailureException

        // FIXME Warum gibt es bei replaceWith() eine Exception?
//        return mongo.update<Kunde>()
//            .replaceWith(neuerKunde)
//            .asType<Kunde>()
//            .findReplaceAndAwait()

        @Suppress("SwallowedException")
        return withTimeout(timeoutShort) {
            try {
                val kundeUpdated = mongo.save(neuerKunde).awaitFirst()
                UpdateResult.Success(kundeUpdated)
            } catch (e: OptimisticLockingFailureException) {
                UpdateResult.VersionOutdated(version)
            }
        }
    }

    /**
     * Einen vorhandenen Kunden in der DB löschen.
     * @param id Die ID des zu löschenden Kunden.
     * @return DeleteResult falls es zur ID ein Kundenobjekt gab, das gelöscht wurde; null sonst.
     */
    // TODO https://github.com/spring-projects/spring-security/issues/8143
    // TODO https://github.com/spring-projects/spring-framework/issues/22462
    // @PreAuthorize("hasRole('ADMIN')")
    suspend fun deleteById(id: KundeId) = withTimeout(timeoutShort) {
        logger.debug { "deleteById(): id = $id" }
        val result = mongo.remove<Kunde>()
            .matching(Kunde::id isEqualTo id)
            .allAndAwait()
        logger.debug { "deleteByEmail(): Anzahl geloeschte Objekte = ${result.deletedCount}" }
        return@withTimeout result
    }

    /**
     * Einen vorhandenen Kunden löschen.
     * @param email Die Email des zu löschenden Kunden.
     * @return DeleteResult falls es zur Email ein Kundenobjekt gab, das gelöscht wurde; null sonst.
     */
    // @PreAuthorize("hasRole('ADMIN')")
    suspend fun deleteByEmail(email: String) = withTimeout(timeoutShort) {
        logger.debug { "deleteByEmail(): email = $email" }
        val result = mongo.remove<Kunde>()
            .matching(Kunde::email isEqualTo email)
            .allAndAwait()
        logger.debug { "deleteByEmail(): Anzahl geloeschte Objekte = ${result.deletedCount}" }
        return@withTimeout result
    }

    private companion object {
        val logger = KotlinLogging.logger {}

        const val timeoutShort = 500L
        const val timeoutLong = 2000L
    }
}
