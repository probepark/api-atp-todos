package com.apt.todos.security

import com.apt.todos.TodosApp
import com.apt.todos.domain.User
import com.apt.todos.repository.UserRepository
import java.util.Locale
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.userdetails.ReactiveUserDetailsService

private const val USER_ONE_LOGIN = "test-user-one"
private const val USER_ONE_EMAIL = "test-user-one@localhost"
private const val USER_TWO_LOGIN = "test-user-two"
private const val USER_TWO_EMAIL = "test-user-two@localhost"
private const val USER_THREE_LOGIN = "test-user-three"
private const val USER_THREE_EMAIL = "test-user-three@localhost"

/**
 * Integration tests for [DomainUserDetailsService].
 */
@SpringBootTest(classes = [TodosApp::class])
class DomainUserDetailsServiceIT {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var domainUserDetailsService: ReactiveUserDetailsService

    private lateinit var userOne: User
    private lateinit var userTwo: User
    private lateinit var userThree: User

    @BeforeEach
    fun init() {
        userRepository.deleteAll().block()

        userOne = User(
            login = USER_ONE_LOGIN,
            password = RandomStringUtils.random(60),
            activated = true,
            email = USER_ONE_EMAIL,
            firstName = "userOne",
            lastName = "doe",
            langKey = "en"
        )
        userRepository.save(userOne).block()

        userTwo = User(
            login = USER_TWO_LOGIN,
            password = RandomStringUtils.random(60),
            activated = true,
            email = USER_TWO_EMAIL,
            firstName = "userTwo",
            lastName = "doe",
            langKey = "en"
        )
        userRepository.save(userTwo).block()

        userThree = User(
            login = USER_THREE_LOGIN,
            password = RandomStringUtils.random(60),
            activated = false,
            email = USER_THREE_EMAIL,
            firstName = "userThree",
            lastName = "doe",
            langKey = "en"
        )
        userRepository.save(userThree).block()
    }

    @Test
    fun assertThatUserCanBeFoundByLogin() {
        val userDetails = domainUserDetailsService.findByUsername(USER_ONE_LOGIN).block()
        assertThat(userDetails).isNotNull
        assertThat(userDetails.username).isEqualTo(USER_ONE_LOGIN)
    }

    @Test
    fun assertThatUserCanBeFoundByLoginIgnoreCase() {
        val userDetails = domainUserDetailsService.findByUsername(USER_ONE_LOGIN.toUpperCase(Locale.ENGLISH)).block()
        assertThat(userDetails).isNotNull
        assertThat(userDetails.username).isEqualTo(USER_ONE_LOGIN)
    }

    @Test
    fun assertThatUserCanBeFoundByEmail() {
        val userDetails = domainUserDetailsService.findByUsername(USER_TWO_EMAIL).block()
        assertThat(userDetails).isNotNull
        assertThat(userDetails.username).isEqualTo(USER_TWO_LOGIN)
    }

    @Test
    fun assertThatUserCanBeFoundByEmailIgnoreCase() {
        val userDetails = domainUserDetailsService.findByUsername(USER_TWO_EMAIL.toUpperCase(Locale.ENGLISH)).block()
        assertThat(userDetails).isNotNull
        assertThat(userDetails.username).isEqualTo(USER_TWO_LOGIN)
    }

    @Test
    fun assertThatEmailIsPrioritizedOverLogin() {
        val userDetails = domainUserDetailsService.findByUsername(USER_ONE_EMAIL).block()
        assertThat(userDetails).isNotNull
        assertThat(userDetails.username).isEqualTo(USER_ONE_LOGIN)
    }

    @Test
    fun assertThatUserNotActivatedExceptionIsThrownForNotActivatedUsers() {
        assertThatExceptionOfType(UserNotActivatedException::class.java).isThrownBy {
            domainUserDetailsService.findByUsername(USER_THREE_LOGIN).block()
        }
    }
}
