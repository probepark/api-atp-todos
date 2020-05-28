package com.apt.todos.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder

/**
 * Test class for the Security Utility methods.
 */
class SecurityUtilsUnitTest {

    @Test
    fun testgetCurrentUserLogin() {
        val login = getCurrentUserLogin()
            .subscriberContext(
                ReactiveSecurityContextHolder.withAuthentication(
                    UsernamePasswordAuthenticationToken("admin", "admin")
                )
            )
            .block()
        assertThat(login).isEqualTo("admin")
    }

    @Test
    fun testgetCurrentUserJWT() {
        val jwt = getCurrentUserJWT()
            .subscriberContext(
                ReactiveSecurityContextHolder.withAuthentication(
                    UsernamePasswordAuthenticationToken("admin", "token")
                )
            )
            .block()
        assertThat(jwt).isEqualTo("token")
    }

    @Test
    fun testIsAuthenticated() {
        val isAuthenticated = isAuthenticated()
            .subscriberContext(
                ReactiveSecurityContextHolder.withAuthentication(
                    UsernamePasswordAuthenticationToken("admin", "admin")
                )
            )
            .block()
        assertThat(isAuthenticated!!).isTrue()
    }

    @Test
    fun testAnonymousIsNotAuthenticated() {
        val authorities = mutableListOf(SimpleGrantedAuthority(ANONYMOUS))
        val isAuthenticated = isAuthenticated()
            .subscriberContext(
                ReactiveSecurityContextHolder.withAuthentication(
                    UsernamePasswordAuthenticationToken("admin", "admin", authorities)
                )
            )
            .block()
        assertThat(isAuthenticated!!).isFalse()
    }

    @Test
    fun testIsCurrentUserInRole() {
        val authorities = mutableListOf(SimpleGrantedAuthority(USER))
        val context = ReactiveSecurityContextHolder.withAuthentication(
            UsernamePasswordAuthenticationToken("admin", "admin", authorities)
        )
        var isCurrentUserInRole = isCurrentUserInRole(USER)
            .subscriberContext(context)
            .block()
        assertThat(isCurrentUserInRole!!).isTrue()

        isCurrentUserInRole = isCurrentUserInRole(ADMIN)
            .subscriberContext(context)
            .block()
        assertThat(isCurrentUserInRole!!).isFalse()
    }
}
