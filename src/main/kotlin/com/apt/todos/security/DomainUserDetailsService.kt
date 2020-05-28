package com.apt.todos.security

import com.apt.todos.domain.User
import com.apt.todos.repository.UserRepository
import java.util.Locale
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator
import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Authenticate a user from the database.
 */
@Component("userDetailsService")
class DomainUserDetailsService(private val userRepository: UserRepository) : ReactiveUserDetailsService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun findByUsername(login: String): Mono<UserDetails> {
        log.debug("Authenticating {}", login)

        if (EmailValidator().isValid(login, null)) {
            return userRepository.findOneByEmailIgnoreCase(login)
                .switchIfEmpty(Mono.error(UsernameNotFoundException("User with email $login was not found in the database")))
                .map { createSpringSecurityUser(login, it) }
        }

        val lowercaseLogin = login.toLowerCase(Locale.ENGLISH)
        return userRepository.findOneByLogin(lowercaseLogin)
            .switchIfEmpty(Mono.error(UsernameNotFoundException("User $lowercaseLogin was not found in the database")))
            .map { createSpringSecurityUser(lowercaseLogin, it) }
    }

    private fun createSpringSecurityUser(lowercaseLogin: String, user: User):
        org.springframework.security.core.userdetails.User {
        if (!user.activated) {
            throw UserNotActivatedException("User $lowercaseLogin was not activated")
        }
        val grantedAuthorities = user.authorities.map { SimpleGrantedAuthority(it.name) }
        return org.springframework.security.core.userdetails.User(
            user.login!!,
            user.password!!,
            grantedAuthorities
        )
    }
}
