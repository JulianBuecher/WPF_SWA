/*
 * Copyright (C) 2020 - present Juergen Zimmermann, Hochschule Karlsruhe
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
@file:Suppress("unused")

package com.acme.kunde

import com.acme.kunde.config.Settings.DEV
import com.acme.kunde.config.db.customConversions
import com.acme.kunde.config.db.generateKundeId
import com.acme.kunde.config.db.writeConcernResolver
import com.acme.kunde.config.dev.LogCommandLine
import com.acme.kunde.config.dev.LogCommandLineSecurity
import com.acme.kunde.config.dev.loggingMappingEvents
import com.acme.kunde.config.validatorFactory
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.beans

// https://github.com/sdeleuze/spring-kotlin-functional/blob/boot/src/main/kotlin/functional/Beans.kt
// https://stackoverflow.com/questions/45935931/...
//         ...how-to-use-functional-bean-definition-kotlin-dsl-with-spring-boot-and-spring-w#answer-46033685

/**
 * Funktionale Registrierung der eigenen Spring Beans, anstatt @Component, @Service oder @Component zu verwenden.
 */
class BeansInitializer : ApplicationContextInitializer<GenericApplicationContext> {
    /**
     * Initialisierung des aktuellen Spring-Applikationskontexts.
     * @param ctx Die zu konfigurierende Spring-Anwendung.
     */
    override fun initialize(ctx: GenericApplicationContext) = beans.initialize(ctx)
}

/**
 * Funktionale Deklaration der eigenen Spring Beans, anstatt @Component, @Service oder @Component zu verwenden.
 */
val beans = beans {
    bean(::validatorFactory)
    bean(::customConversions)
    bean(::generateKundeId)
    bean(::writeConcernResolver)

    profile(DEV) {
        bean<LogCommandLine>()
    }

    profile("logSecurity") {
        bean<LogCommandLineSecurity>()
    }

    profile("loggingDB") {
        bean(::loggingMappingEvents)
    }
}
