package com.acme.kunde.config.security
//
//import org.keycloak.adapters.springsecurity.KeycloakConfiguration
//import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.context.annotation.Bean
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
//import org.springframework.security.config.annotation.web.builders.HttpSecurity
//import org.springframework.security.core.session.SessionRegistryImpl
//import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy
//import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
//
//@KeycloakConfiguration
//class SecurityConfiguration : KeycloakWebSecurityConfigurerAdapter {
//    /**
//     * Registers the KeycloakAuthenticationProvider with the authentication manager.
//     */
//    @Autowired
//    fun configureGlobal(auth: AuthenticationManagerBuilder) {
//            auth.authenticationProvider(keycloakAuthenticationProvider())
//    }
//
//    @Bean
//    override fun sessionAuthenticationStrategy(): SessionAuthenticationStrategy {
//        return RegisterSessionAuthenticationStrategy(SessionRegistryImpl())
//    }
//
//    override fun configure(http: HttpSecurity) {
//        super.configure(http)
//        http.authorizeRequests()
//            .antMatchers("/kunde/**").hasRole("admin")
//            .anyRequest().permitAll()
//    }
//}
