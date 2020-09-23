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

package com.acme.bestellung

import com.acme.bestellung.config.MailProps
import com.acme.bestellung.config.Settings.banner
import kotlinx.coroutines.InternalCoroutinesApi
import org.springframework.boot.WebApplicationType.REACTIVE
import org.springframework.boot.actuate.autoconfigure.context.ShutdownEndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.mail.MailHealthContributorAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.mongo.MongoReactiveHealthContributorAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.web.reactive.ReactiveManagementContextAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.function.client.ClientHttpConnectorAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.context.ApplicationPidFileWriter
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.hateoas.config.EnableHypermediaSupport
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL
import org.springframework.hateoas.support.WebStack.WEBFLUX
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity

/**
 * Die Klasse, die beim Start des Hauptprogramms verwendet wird, um zu konfigurieren, dass es sich um eine Anwendung mit
 * _Spring Boot_ handelt. Dadurch werden auch viele von Spring Boot gelieferte Konfigurationsklassen automatisch
 * konfiguriert.
 *
 * [Use Cases](../../images/use-cases.svg)
 *
 * [Komponentendiagramm](../../images/komponenten.svg)
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@SpringBootApplication(proxyBeanMethods = false)
@ImportAutoConfiguration(
    classes = [
        // Spring Framework and Spring Boot
        ConfigurationPropertiesAutoConfiguration::class,
        ProjectInfoAutoConfiguration::class,
        PropertyPlaceholderAutoConfiguration::class,

        // Spring WebFlux
        CodecsAutoConfiguration::class,
        ErrorWebFluxAutoConfiguration::class,
        HttpHandlerAutoConfiguration::class,
        HttpMessageConvertersAutoConfiguration::class,
        ReactiveWebServerFactoryAutoConfiguration::class,
        WebFluxAutoConfiguration::class,

        // Jackson
        JacksonAutoConfiguration::class,

        // Spring Security: s. Annotation
        // ReactiveSecurityAutoConfiguration::class,
        // SecurityAutoConfiguration::class,

        // Spring Data MongoDB: s. Annotation
        // MongoReactiveAutoConfiguration::class,
        // MongoReactiveDataAutoConfiguration::class,

        // Spring Mail
        MailSenderAutoConfiguration::class,

        // WebClient
        ClientHttpConnectorAutoConfiguration::class,
        WebClientAutoConfiguration::class,

        // Actuator
        EndpointAutoConfiguration::class,
        HealthContributorAutoConfiguration::class,
        HealthEndpointAutoConfiguration::class,
        MailHealthContributorAutoConfiguration::class,
        ManagementContextAutoConfiguration::class,
        MongoReactiveHealthContributorAutoConfiguration::class,
        ReactiveManagementContextAutoConfiguration::class,
        ShutdownEndpointAutoConfiguration::class,
        WebEndpointAutoConfiguration::class,
        // BeansEndpointAutoConfiguration::class,
    ]
)
@EnableHypermediaSupport(type = [HAL], stacks = [WEBFLUX])
@EnableWebFluxSecurity
@EnableMongoAuditing
@EnableConfigurationProperties(MailProps::class)
class Application

/**
 * Hauptprogramm, um den Microservice zu starten.
 *
 * @param args Evtl. zusätzliche Argumente für den Start des Microservice
 */
@InternalCoroutinesApi
fun main(args: Array<String>) {
    @Suppress("SpreadOperator")
    runApplication<Application>(*args) {
        webApplicationType = REACTIVE
        // addInitializers(beans)
        setBanner(banner)
        addListeners(ApplicationPidFileWriter())
    }
}
