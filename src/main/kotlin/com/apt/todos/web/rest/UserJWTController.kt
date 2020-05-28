package com.apt.todos.web.rest

import com.apt.todos.config.ANONYMOUS_USER
import com.apt.todos.security.jwt.JWTFilter
import com.apt.todos.security.jwt.TokenProvider
import com.apt.todos.service.AuditEventService
import com.apt.todos.web.rest.vm.LoginVM
import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping("/api")
class UserJWTController(
    private val tokenProvider: TokenProvider,

    private val authenticationManager: ReactiveAuthenticationManager,
    private val auditEventService: AuditEventService

) {
    @PostMapping("/authenticate")
    fun authorize(@Valid @RequestBody loginVM: Mono<LoginVM>): Mono<ResponseEntity<JWTToken>> =
        loginVM.flatMap { login ->
            authenticationManager.authenticate(UsernamePasswordAuthenticationToken(login.username, login.password))
                .onErrorResume { onAuthenticationError(login, it) }
                .flatMap { onAuthenticationSuccess(login, it) }
                .map { tokenProvider.createToken(it, true == login.isRememberMe) }
        }.map {
            jwt ->
            val httpHeaders = HttpHeaders()
            httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer $jwt")
            ResponseEntity(JWTToken(jwt), httpHeaders, HttpStatus.OK)
        }

    private fun onAuthenticationSuccess(login: LoginVM, auth: Authentication): Mono<Authentication> {
        return Mono.just(login)
            .map { it.username }
            .filter { ANONYMOUS_USER != it }
            .flatMap { auditEventService.saveAuthenticationSuccess(it!!) }
            .thenReturn(auth)
    }

    private fun onAuthenticationError(login: LoginVM, throwable: Throwable): Mono<Authentication> {
        return Mono.just(login)
                .map { it.username }
                .filter { ANONYMOUS_USER != it }
                .flatMap { auditEventService.saveAuthenticationError(it!!, throwable) }
                .then(Mono.error(throwable))
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    class JWTToken(@get:JsonProperty("id_token") var idToken: String?)
}
