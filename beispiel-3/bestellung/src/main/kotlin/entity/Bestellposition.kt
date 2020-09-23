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
package com.acme.bestellung.entity

import java.math.BigDecimal
import java.util.UUID
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.Min

/**
 * Unveränderliche Daten einer Bestellungposition.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @property artikelId ID der bestellten Artikels.
 * @property einzelpreis Einzelpreis.
 * @property anzahl Anzahl des bestellten Artikels.a
 */
data class Bestellposition(
    val artikelId: ArtikelId,

    @get:DecimalMin(
        value = "0",
        inclusive = false,
        message = "{bestellposition.einzelpreis.decimalMin}",
    )
    val einzelpreis: BigDecimal,

    @get:Min(value = 1, message = "{bestellposition.anzahl.min}")
    val anzahl: Int = 1,
)

typealias ArtikelId = UUID
