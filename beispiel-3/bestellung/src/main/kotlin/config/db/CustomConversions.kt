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
package com.acme.bestellung.config.db

import com.acme.bestellung.entity.BestellungId
import org.springframework.context.annotation.Bean
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions

/**
 * Spring-Konfiguration für Enum-Konvertierungen beim Zugriff auf _MongoDB_.
 *
 * @author Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface CustomConversions {
    /**
     * Liste mit Konvertern für Lesen und Schreiben in _MongoDB_ ermitteln.
     * @return Liste mit Konvertern für Lesen und Schreiben in _MongoDB_.
     */
    @Bean
    fun customConversions() = MongoCustomConversions(
        listOfNotNull(BestellungIdConverter.ReadConverter(), BestellungIdConverter.WriteConverter())
    )

    /**
     * Konverterklassen, um BestellungIds in _MongoDB_ zu speichern und auszulesen.
     *
     * @author Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
     */
    interface BestellungIdConverter {
        /**
         * Konvertierungsklasse für MongoDB, um einen String einzulesen und eine BestellungId zu erzeugen.
         * Wegen @ReadingConverter ist kein Lambda-Ausdruck möglich.
         */
        @ReadingConverter
        class ReadConverter : Converter<String, BestellungId> {
            /**
             * Konvertierung eines Strings in eine BestellungId.
             * @param bestellungId String mit einer BestellungId.
             * @return Zugehörige BestellungId
             */
            override fun convert(bestellungId: String): BestellungId = BestellungId.fromString(bestellungId)
        }

        /**
         * Konvertierungsklasse für MongoDB, um eine BestellungId in einen String zu konvertieren.
         * Wegen @WritingConverter ist kein Lambda-Ausdruck möglich.
         */
        @WritingConverter
        class WriteConverter : Converter<BestellungId, String> {
            /**
             * Konvertierung einer BestellungId in einen String, z.B. beim Abspeichern.
             * @param bestellungId Objekt von BestellungId
             * @return String z.B. zum Abspeichern.
             */
            override fun convert(bestellungId: BestellungId): String? = bestellungId.toString()
        }
    }
}
