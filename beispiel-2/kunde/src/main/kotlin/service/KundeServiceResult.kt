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

import com.acme.kunde.entity.Kunde
import jakarta.validation.ConstraintViolation

/**
 * Resultat-Typ für [KundeService.findById]
 */
sealed class FindByIdResult {
    /**
     * Resultat-Typ, wenn ein Kunde gefunden wurde.
     * @property kunde Der gefundene Kunde
     */
    data class Success(val kunde: Kunde) : FindByIdResult()

    /**
     * Resultat-Typ, wenn kein Kunde gefunden wurde.
     */
    object NotFound : FindByIdResult()

    /**
     * Resultat-Typ, wenn ein Kunde wegen unzureichender Rollen _nicht_ gesucht werden darf.
     * @property rollen Die vorhandenen
     */
    data class AccessForbidden(val rollen: List<String>? = null) : FindByIdResult()
}

/**
 * Resultat-Typ für [KundeService.create]
 */
sealed class CreateResult {
    /**
     * Resultat-Typ, wenn ein neuer Kunde erfolgreich angelegt wurde.
     * @property kunde Der neu angelegte Kunde
     */
    data class Success(val kunde: Kunde) : CreateResult()

    /**
     * Resultat-Typ, wenn ein Kunde wegen Constraint-Verletzungen nicht angelegt wurde.
     * @property violations Die verletzten Constraints
     */
    data class ConstraintViolations(val violations: Set<ConstraintViolation<Kunde>>) : CreateResult()

    /**
     * Resultat-Typ, wenn bei einem neu anzulegenden Kunden kein gültiger Account angegeben ist.
     */
    object InvalidAccount : CreateResult()

    /**
     * Resultat-Typ, wenn der Username eines neu anzulegenden Kunden bereits existiert.
     * @property username Der existierende Username
     */
    data class UsernameExists(val username: String) : CreateResult()

    /**
     * Resultat-Typ, wenn die Email eines neu anzulegenden Kunden bereits existiert.
     * @property email Die existierende Email
     */
    data class EmailExists(val email: String) : CreateResult()
}

/**
 * Resultat-Typ für [KundeService.update]
 */
sealed class UpdateResult {
    /**
     * Resultat-Typ, wenn ein Kunde erfolgreich aktualisiert wurde.
     * @property kunde Der aktualisierte Kunde
     */
    data class Success(val kunde: Kunde) : UpdateResult()

    /**
     * Resultat-Typ, wenn ein Kunde wegen Constraint-Verletzungen nicht aktualisiert wurde.
     * @property violations Die verletzten Constraints
     */
    data class ConstraintViolations(val violations: Set<ConstraintViolation<Kunde>>) : UpdateResult()

    /**
     * Resultat-Typ, wenn die Versionsnummer eines zu öndernden Kunden ungültig ist.
     * @property version Die ungültige Versionsnummer
     */
    data class VersionInvalid(val version: String) : UpdateResult()

    /**
     * Resultat-Typ, wenn die Versionsnummer eines zu öndernden Kunden nicht aktuell ist.
     * @property version Die veraltete Versionsnummer
     */
    data class VersionOutdated(val version: Int) : UpdateResult()

    /**
     * Resultat-Typ, wenn die Email eines zu öndernden Kunden bereits existiert.
     * @property email Die existierende Email
     */
    data class EmailExists(val email: String) : UpdateResult()

    /**
     * Resultat-Typ, wenn ein nicht-vorhandener Kunde aktualisiert werden sollte.
     */
    object NotFound : UpdateResult()
}
