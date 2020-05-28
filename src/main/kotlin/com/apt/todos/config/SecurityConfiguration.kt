package com.apt.todos.config

import com.apt.todos.security.ADMIN
import com.apt.todos.security.jwt.JWTFilter
import com.apt.todos.security.jwt.TokenProvider
import com.apt.todos.web.filter.SpaWebFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers
import org.zalando.problem.spring.webflux.advice.security.SecurityProblemSupport

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Import(SecurityProblemSupport::class)
class SecurityConfiguration(
    private val userDetailsService: ReactiveUserDetailsService,
    private val tokenProvider: TokenProvider,
    private val problemSupport: SecurityProblemSupport
) {

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    fun reactiveAuthenticationManager() =
        UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService).apply {
            setPasswordEncoder(passwordEncoder())
        }

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        // @formatter:off
        http
            .securityMatcher(
                NegatedServerWebExchangeMatcher(
                    OrServerWebExchangeMatcher(
                        pathMatchers("/app/**", "/i18n/**", "/content/**", "/swagger-ui/**", "/test/**", "/webjars/**"),
                        pathMatchers(HttpMethod.OPTIONS, "/**")
                    )
                )
            )
            .csrf()
            .disable()
            .addFilterAt(SpaWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAt(JWTFilter(tokenProvider), SecurityWebFiltersOrder.HTTP_BASIC)
            .authenticationManager(reactiveAuthenticationManager())
            .exceptionHandling()
            .accessDeniedHandler(problemSupport)
            .authenticationEntryPoint(problemSupport)
        .and()
            .headers()
            .contentSecurityPolicy("default-src 'self'; frame-src 'self' data:; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://storage.googleapis.com; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:")
        .and()
            .referrerPolicy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
        .and()
            .featurePolicy("geolocation 'none'; midi 'none'; sync-xhr 'none'; microphone 'none'; camera 'none'; magnetometer 'none'; gyroscope 'none'; speaker 'none'; fullscreen 'self'; payment 'none'")
        .and()
            .frameOptions().disable()
        .and()
            .authorizeExchange()
            .pathMatchers("/").permitAll()
            .pathMatchers("/*.*").permitAll()
            .pathMatchers("/api/register").permitAll()
            .pathMatchers("/api/activate").permitAll()
            .pathMatchers("/api/authenticate").permitAll()
            .pathMatchers("/api/account/reset-password/init").permitAll()
            .pathMatchers("/api/account/reset-password/finish").permitAll()
            .pathMatchers("/api/auth-info").permitAll()
            .pathMatchers("/api/**").authenticated()
            .pathMatchers("/services/**", "/swagger-resources/**", "/v2/api-docs").authenticated()
            .pathMatchers("/management/health").permitAll()
            .pathMatchers("/management/info").permitAll()
            .pathMatchers("/management/prometheus").permitAll()
            .pathMatchers("/management/**").hasAuthority(ADMIN)
        // @formatter:on
        return http.build()
    }
}
