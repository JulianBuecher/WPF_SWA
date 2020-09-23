@file:Suppress("StringLiteralDuplication")

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
package com.acme.kunde.config.dev

import com.acme.kunde.entity.Adresse
import com.acme.kunde.entity.FamilienstandType
import com.acme.kunde.entity.GeschlechtType
import com.acme.kunde.entity.InteresseType
import com.acme.kunde.entity.Kunde
import com.acme.kunde.entity.Kunde.Companion.NACHNAME_PATTERN
import com.acme.kunde.entity.KundeId
import com.acme.kunde.entity.Umsatz
import com.mongodb.reactivestreams.client.MongoCollection
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import mu.KLogger
import mu.KotlinLogging
import org.bson.Document
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.data.domain.Range
import org.springframework.data.domain.Range.Bound
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.mongodb.core.CollectionOptions
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.createCollection
import org.springframework.data.mongodb.core.dropCollection
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.indexOps
import org.springframework.data.mongodb.core.insert
import org.springframework.data.mongodb.core.oneAndAwait
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty.`object`
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty.array
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty.bool
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty.date
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty.int32
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty.string
import org.springframework.data.mongodb.core.schema.MongoJsonSchema
import java.math.BigDecimal
import java.net.URL
import java.time.LocalDate
import java.util.Currency

// Default-Implementierungen in einem Interface gibt es ab Java 8, d.h. ab 2013 !!!
// Eine abstrakte Klasse kann uebrigens auch Properties / Attribute / Felder sowie einen Konstruktor haben.
// In C# gibt es "Default Interface Methods", damit man mit Xamarin Android- und iOS-Apps entwickeln kann.
// https://docs.microsoft.com/en-us/dotnet/csharp/language-reference/proposals/csharp-8.0/default-interface-methods

/**
 * Interface, um im Profil _dev_ die (Test-) DB neu zu laden.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface DbPopulate {
    /**
     * Bean-Definition, um einen CommandLineRunner für das Profil "dev" bereitzustellen,
     * damit die (Test-) DB neu geladen wird.
     * @param mongo Template für MongoDB
     * @return CommandLineRunner
     */
    @Bean
    fun dbPopulate(mongo: ReactiveMongoOperations) = CommandLineRunner {
        val logger = KotlinLogging.logger {}
        logger.warn("Neuladen der Collection 'Kunde'")

        runBlocking {
            mongo.dropCollection<Kunde>().awaitFirstOrNull()
            createCollectionAndSchema(mongo, logger)
            createIndexNachname(mongo, logger)
            createIndexEmail(mongo, logger)
            createIndexUmsatz(mongo, logger)

            testdaten.onEach { kunde -> mongo.insert<Kunde>().oneAndAwait(kunde) }
                .collect { kunde -> logger.warn { kunde } }
        }
    }

    @Suppress("MagicNumber", "LongMethod")
    private suspend fun createCollectionAndSchema(
        mongo: ReactiveMongoOperations,
        logger: KLogger,
    ): MongoCollection<Document> {
        val maxKategorie = 9
        val plzLength = 5

        // https://docs.mongodb.com/manual/core/schema-validation
        // https://docs.mongodb.com/manual/release-notes/3.6/#json-schema
        // https://www.mongodb.com/blog/post/mongodb-36-json-schema-validation-expressive-query-syntax
        val schema = MongoJsonSchema.builder()
            .required("nachname", "email", "kategorie", "newsletter", "adresse")
            .properties(
                // Die Kunde-ID wird durch ReactiveBeforeSaveCallback gesetzt
                // deshalb NICHT:   string("id").matching(ID_PATTERN)
                int32("version"),
                string("nachname").matching(NACHNAME_PATTERN),
                string("email"),
                int32("kategorie")
                    .within(Range.of(Bound.inclusive(0), Bound.inclusive(maxKategorie))),
                bool("newsletter"),
                date("geburtsdatum"),
                `object`("umsatz")
                    .properties(string("betrag"), string("waehrung")),
                string("homepage"),
                string("geschlecht").possibleValues("M", "W", "D"),
                string("familienstand").possibleValues("L", "VH", "G", "VW"),
                array("interessen").uniqueItems(true),
                `object`("adresse")
                    .properties(
                        string("plz").minLength(plzLength).maxLength(plzLength),
                        string("ort"),
                    ),
                string("username"),
                date("erzeugt"),
                date("aktualisiert"),
            )
            .build()
        logger.info { "JSON Schema fuer Kunde: ${schema.toDocument().toJson()}" }
        return mongo.createCollection<Kunde>(CollectionOptions.empty().schema(schema)).awaitFirst()
    }

    private suspend fun createIndexNachname(mongo: ReactiveMongoOperations, logger: KLogger): String {
        logger.warn("Index fuer 'nachname'")
        val idx = Index("nachname", ASC).named("nachname")
        return mongo.indexOps<Kunde>().ensureIndex(idx).awaitFirst()
    }

    private suspend fun createIndexEmail(mongo: ReactiveMongoOperations, logger: KLogger): String {
        logger.warn("Index fuer 'email'")
        // Emailadressen sollen paarweise verschieden sein
        val idx = Index("email", ASC).unique().named("email")
        return mongo.indexOps<Kunde>().ensureIndex(idx).awaitFirst()
    }

    private suspend fun createIndexUmsatz(mongo: ReactiveMongoOperations, logger: KLogger): String {
        logger.warn("Index fuer 'umsatz'")
        // "sparse" statt NULL bei relationalen DBen
        // Keine Indizierung der Kunden, bei denen es kein solches Feld gibt
        val umsatzIdx = Index("umsatz", ASC).sparse().named("umsatz")
        return mongo.indexOps<Kunde>().ensureIndex(umsatzIdx).awaitFirst()
    }

    private companion object {
        @Suppress("MagicNumber", "UnderscoresInNumericLiterals")
        val testdaten = flowOf(
            Kunde(
                id = KundeId.fromString("00000000-0000-0000-0000-000000000000"),
                nachname = "Admin",
                email = "admin@acme.de",
                kategorie = 0,
                newsletter = true,
                geburtsdatum = LocalDate.of(2019, 1, 31),
                umsatz = Umsatz(
                    BigDecimal("0"),
                    Currency.getInstance("EUR"),
                ),
                homepage = URL("https://www.acme.de"),
                geschlecht = GeschlechtType.build("W"),
                familienstand = FamilienstandType.build("VH"),
                interessen = listOfNotNull(InteresseType.build("L")!!),
                adresse = Adresse("00000", "Aachen"),
                username = "admin",
            ),
            Kunde(
                id = KundeId.fromString("00000000-0000-0000-0000-000000000001"),
                nachname = "Alpha",
                email = "alpha@acme.edu",
                kategorie = 1,
                newsletter = true,
                geburtsdatum = LocalDate.of(2019, 1, 1),
                umsatz = Umsatz(
                    BigDecimal("10"),
                    Currency.getInstance("USD"),
                ),
                homepage = URL("https://www.acme.edu"),
                geschlecht = GeschlechtType.build("M"),
                familienstand = FamilienstandType.build("L"),
                interessen = listOfNotNull(
                    InteresseType.build("S")!!,
                    InteresseType.build("L")!!,
                ),
                adresse = Adresse("11111", "Augsburg"),
                username = "alpha1",
            ),
            Kunde(
                id = KundeId.fromString("00000000-0000-0000-0000-000000000002"),
                nachname = "Alpha",
                email = "alpha@acme.ch",
                kategorie = 2,
                newsletter = true,
                geburtsdatum = LocalDate.of(2019, 1, 2),
                umsatz = Umsatz(
                    BigDecimal("20"),
                    Currency.getInstance("CHF"),
                ),
                homepage = URL("https://www.acme.ch"),
                geschlecht = GeschlechtType.build("W"),
                familienstand = FamilienstandType.build("G"),
                interessen = listOfNotNull(
                    InteresseType.build("S")!!,
                    InteresseType.build("R")!!,
                ),
                adresse = Adresse("22222", "Aalen"),
                username = "alpha2",
            ),
            Kunde(
                id = KundeId.fromString("00000000-0000-0000-0000-000000000003"),
                nachname = "Alpha",
                email = "alpha@acme.uk",
                kategorie = 3,
                newsletter = true,
                geburtsdatum = LocalDate.of(2019, 1, 3),
                umsatz = Umsatz(
                    BigDecimal("30"),
                    Currency.getInstance("GBP"),
                ),
                homepage = URL("https://www.acme.uk"),
                geschlecht = GeschlechtType.build("M"),
                familienstand = FamilienstandType.build("VW"),
                interessen = listOfNotNull(
                    InteresseType.build("L")!!,
                    InteresseType.build("R")!!,
                ),
                adresse = Adresse("33333", "Ahlen"),
                username = "alpha3",
            ),
            Kunde(
                id = KundeId.fromString("00000000-0000-0000-0000-000000000004"),
                nachname = "Delta",
                email = "delta@acme.jp",
                kategorie = 4,
                newsletter = true,
                geburtsdatum = LocalDate.of(2019, 1, 4),
                umsatz = Umsatz(
                    BigDecimal("40"),
                    Currency.getInstance("JPY"),
                ),
                homepage = URL("https://www.acme.jp"),
                geschlecht = GeschlechtType.build("W"),
                familienstand = FamilienstandType.build("VH"),
                interessen = null,
                adresse = Adresse("44444", "Dortmund"),
                username = "delta",
            ),
            Kunde(
                id = KundeId.fromString("00000000-0000-0000-0000-000000000005"),
                nachname = "Epsilon",
                email = "epsilon@acme.cn",
                kategorie = 5,
                newsletter = true,
                geburtsdatum = LocalDate.of(2019, 1, 5),
                umsatz = null,
                homepage = URL("https://www.acme.cn"),
                geschlecht = GeschlechtType.build("M"),
                familienstand = FamilienstandType.build("L"),
                interessen = null,
                adresse = Adresse("55555", "Essen"),
                username = "epsilon",
            ),
            Kunde(
                id = KundeId.fromString("00000000-0000-0000-0000-000000000006"),
                nachname = "Phi",
                email = "phi@acme.cn",
                kategorie = 6,
                newsletter = true,
                geburtsdatum = LocalDate.of(2019, 1, 6),
                umsatz = null,
                homepage = URL("https://www.acme.cn"),
                geschlecht = GeschlechtType.build("M"),
                familienstand = FamilienstandType.build("L"),
                interessen = null,
                adresse = Adresse("66666", "Freiburg"),
                username = "phi",
            ),
        )
    }
}
