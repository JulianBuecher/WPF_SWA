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

import com.acme.kunde.entity.Kunde
import com.acme.kunde.entity.KundeId.randomUUID
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.mapping.event.ReactiveBeforeConvertCallback
import reactor.kotlin.core.publisher.toMono

/**
 * Spring-Konfiguration für die ID-Generierung beim Abspeichern in _MongoDB_.
 *
 * @author Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface GenerateKundeId {
    /**
     * Bean zur Generierung der Kunde-ID beim Anlegen eines neuen Kunden
     * @return Kunde-Objekt mit einer Kunde-ID
     */
    @Bean
    fun generateKundeId() = ReactiveBeforeConvertCallback<Kunde> { kunde, _ ->
        if (kunde.id == null) {
            kunde.copy(id = randomUUID(), email = kunde.email.toLowerCase())
        } else {
            kunde
        }.toMono()
    }
}
