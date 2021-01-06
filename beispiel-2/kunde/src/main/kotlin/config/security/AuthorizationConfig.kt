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
package com.acme.kunde.config.security

import com.acme.kunde.Router.Companion.apiPath
import mu.KotlinLogging
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers
import reactor.core.publisher.Mono
import java.util.stream.Collectors
import kotlin.streams.toList


/**
 * Security-Konfiguration.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
// https://github.com/spring-projects/spring-security/tree/master/samples
@EnableReactiveMethodSecurity
interface AuthorizationConfig {
    /**
     * Bean-Definition, um den Zugriffsschutz an der REST-Schnittstelle zu konfigurieren.
     *
     * @param http Injiziertes Objekt von `ServerHttpSecurity` als Ausgangspunkt für die Konfiguration.
     * @return Objekt von `SecurityWebFilterChain`.
     */
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain = http {
        authorizeExchange {
            val apiPath = String.format("$apiPath/**")
            authorize(pathMatchers(POST, apiPath), hasRole("STUDENT"))
            authorize(pathMatchers(GET, apiPath), hasRole("STUDENT"))

            authorize(EndpointRequest.to("health"), permitAll)
            authorize(EndpointRequest.toAnyEndpoint(), permitAll)
        }
        httpBasic {disable()}
        formLogin { disable() }
        csrf { disable() }
        oauth2ResourceServer {
            jwt { jwtAuthenticationConverter = grantedAuthoritiesExtractor() }
        }
    }

    private fun grantedAuthoritiesExtractor(): Converter<Jwt, out Mono<out AbstractAuthenticationToken>> {
        val extractor = GrantedAuthoritiesExtractor()
        return ReactiveJwtAuthenticationConverterAdapter(extractor)
    }

    class GrantedAuthoritiesExtractor : JwtAuthenticationConverter() {
        override fun extractAuthorities(jwt: Jwt): Collection<GrantedAuthority> {
            val logger = KotlinLogging.logger {}

            // Wir wollen realm access und nicht account
            val resource = jwt.getClaimAsMap("realm_access")

            val roles: List<String> =
                if (resource["roles"] == null) listOf() else (resource["roles"] as List<*>).filterIsInstance<String>()
            println(roles.stream().toList().toString())

            val p = roles.stream()
                .map { role: String? -> SimpleGrantedAuthority(role?.toUpperCase()) }
                .collect(Collectors.toList())
            logger.info { p }

            return roles.stream()
                .map { role: String? -> SimpleGrantedAuthority(String.format("ROLE_${role?.toUpperCase()}")) }
                .collect(Collectors.toList())
        }
    }
}
