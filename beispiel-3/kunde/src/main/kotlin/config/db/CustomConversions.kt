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
package com.acme.kunde.config.db

import com.acme.kunde.config.security.CustomUser
import com.acme.kunde.entity.FamilienstandType
import com.acme.kunde.entity.GeschlechtType
import com.acme.kunde.entity.InteresseType
import org.springframework.context.annotation.Bean
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
        listOfNotNull(
            // Enums
            GeschlechtType.ReadConverter(),
            GeschlechtType.WriteConverter(),
            FamilienstandType.ReadConverter(),
            FamilienstandType.WriteConverter(),
            InteresseType.ReadConverter(),
            InteresseType.WriteConverter(),

            // Rollen fuer Security
            CustomUser.RoleReadConverter(),
            CustomUser.RoleWriteConverter(),
        )
    )
}
