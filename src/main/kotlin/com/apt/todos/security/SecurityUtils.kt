@file:JvmName("SecurityUtils")

package com.apt.todos.security

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.userdetails.UserDetails
import reactor.core.publisher.Mono

/**
 * Get the login of the current user.
 *
 * @return the login of the current user.
 */
fun getCurrentUserLogin(): Mono<String> =
    ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .flatMap { Mono.justOrEmpty(extractPrincipal(it)) }

fun extractPrincipal(authentication: Authentication?): String? {

    if (authentication == null) {
        return null
    }

    return when (val principal = authentication.principal) {
        is UserDetails -> principal.username
        is String -> principal
        else -> null
    }
}

/**
 * Get the JWT of the current user.
 *
 * @return the JWT of the current user.
 */
fun getCurrentUserJWT(): Mono<String> =
    ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .filter { it.credentials is String }
        .map { it.credentials as String }

/**
 * Check if a user is authenticated.
 *
 * @return true if the user is authenticated, false otherwise.
 */
fun isAuthenticated(): Mono<Boolean> {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(Authentication::getAuthorities)
        .map {
            it
            .map(GrantedAuthority::getAuthority)
            .none { it == ANONYMOUS }
        }
}

/**
 * If the current user has a specific authority (security role).
 *
 * The name of this method comes from the `isUserInRole()` method in the Servlet API
 *
 * @param authority the authority to check.
 * @return true if the current user has the authority, false otherwise.
 */
fun isCurrentUserInRole(authority: String): Mono<Boolean> {
  return ReactiveSecurityContextHolder.getContext()
      .map(SecurityContext::getAuthentication)
      .map(Authentication::getAuthorities)
      .map {
          it
          .map(GrantedAuthority::getAuthority)
          .any { it == authority }
      }
}
