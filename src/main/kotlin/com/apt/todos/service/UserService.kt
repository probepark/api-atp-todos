package com.apt.todos.service

import com.apt.todos.config.ANONYMOUS_USER
import com.apt.todos.config.DEFAULT_LANGUAGE
import com.apt.todos.config.SYSTEM_ACCOUNT
import com.apt.todos.domain.Authority
import com.apt.todos.domain.User
import com.apt.todos.repository.AuthorityRepository
import com.apt.todos.repository.UserRepository
import com.apt.todos.security.USER
import com.apt.todos.security.getCurrentUserLogin
import com.apt.todos.service.dto.UserDTO
import io.github.jhipster.security.RandomUtil
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Service class for managing users.
 */
@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authorityRepository: AuthorityRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun activateRegistration(key: String): Mono<User> {
        log.debug("Activating user for activation key {}", key)
        return userRepository.findOneByActivationKey(key)
            .flatMap { user ->
                // activate given user for the registration key.
                user.activated = true
                user.activationKey = null
                updateUser(user)
            }
            .doOnNext { user -> log.debug("Activated user: {}", user) }
    }

    fun completePasswordReset(newPassword: String, key: String): Mono<User> {
        log.debug("Reset user password for reset key {}", key)
        return userRepository.findOneByResetKey(key)
            .filter { user -> user.resetDate?.isAfter(Instant.now().minusSeconds(86400)) ?: false }
            .flatMap { user ->
                user.password = passwordEncoder.encode(newPassword)
                user.resetKey = null
                user.resetDate = null
                updateUser(user)
            }
    }

    fun requestPasswordReset(mail: String): Mono<User> {
        return userRepository.findOneByEmailIgnoreCase(mail)
            .filter(User::activated)
            .flatMap { user ->
                user.resetKey = RandomUtil.generateResetKey()
                user.resetDate = Instant.now()
                updateUser(user)
            }
    }

    fun registerUser(userDTO: UserDTO, password: String): Mono<User> {
        val login = userDTO.login ?: throw IllegalArgumentException("Empty login not allowed")
        val email = userDTO.email
        return userRepository.findOneByLogin(login.toLowerCase())
            .flatMap { existingUser ->
                if (!existingUser.activated) {
                    userRepository.delete(existingUser)
                } else {
                    throw UsernameAlreadyUsedException()
                }
            }
            .then(userRepository.findOneByEmailIgnoreCase(email))
            .flatMap { existingUser ->
                if (!existingUser.activated) {
                    userRepository.delete(existingUser)
                } else {
                    throw EmailAlreadyUsedException()
                }
            }
            .thenReturn(User())
            .flatMap { newUser ->
                newUser.apply {
                    val encryptedPassword = passwordEncoder.encode(password)
                    this.login = login.toLowerCase()
                    // new user gets initially a generated password
                    this.password = encryptedPassword
                    firstName = userDTO.firstName
                    lastName = userDTO.lastName
                    this.email = email?.toLowerCase()
                    imageUrl = userDTO.imageUrl
                    langKey = userDTO.langKey
                    // new user is not active
                    activated = false
                    // new user gets registration key
                    activationKey = RandomUtil.generateActivationKey()
                }

                val authorities = mutableSetOf<Authority>()
                authorityRepository.findById(USER)
                    .map(authorities::add)
                    .thenReturn(newUser)
                    .doOnNext { user -> user.authorities = authorities }
                    .flatMap(this::createUser)
                    .doOnNext { user -> log.debug("Created Information for User: {}", user) }
            }
    }

    fun createUser(userDTO: UserDTO): Mono<User> {
        val encryptedPassword = passwordEncoder.encode(RandomUtil.generatePassword())
        val user = User(
            login = userDTO.login?.toLowerCase(),
            firstName = userDTO.firstName,
            lastName = userDTO.lastName,
            email = userDTO.email?.toLowerCase(),
            imageUrl = userDTO.imageUrl,
            langKey = userDTO.langKey ?: DEFAULT_LANGUAGE, // default language
            password = encryptedPassword,
            resetKey = RandomUtil.generateResetKey(),
            resetDate = Instant.now(),
            activated = true
        )
        return Flux.fromIterable(Optional.ofNullable(userDTO.authorities).orElse(mutableSetOf()))
            .flatMap<Authority>(authorityRepository::findById)
            .doOnNext { authority -> user.authorities.add(authority) }
            .then(createUser(user))
            .doOnNext { user1 -> log.debug("Created Information for User: {}", user1) }
    }

    /**
     * Update basic information (first name, last name, email, language) for the current user.
     *
     * @param firstName first name of user.
     * @param lastName last name of user.
     * @param email email id of user.
     * @param langKey language key.
     * @param imageUrl image URL of user.
     */
    fun updateUser(firstName: String?, lastName: String?, email: String?, langKey: String?, imageUrl: String?): Mono<Void> {
        return getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .flatMap { user ->
                user.firstName = firstName
                user.lastName = lastName
                user.email = email?.toLowerCase()
                user.langKey = langKey
                user.imageUrl = imageUrl
                updateUser(user)
            }
            .doOnNext { user -> log.debug("Changed Information for User: {}", user) }
            .then()
    }

    /**
     * Update all information for a specific user, and return the modified user.
     *
     * @param userDTO user to update.
     * @return updated user.
     */
    fun updateUser(userDTO: UserDTO): Mono<UserDTO> {
        return userRepository.findById(userDTO.id!!)
            .flatMap { user ->
                user.apply {
                    login = userDTO.login!!.toLowerCase()
                    firstName = userDTO.firstName
                    lastName = userDTO.lastName
                    email = userDTO.email?.toLowerCase()
                    imageUrl = userDTO.imageUrl
                    activated = userDTO.activated
                    langKey = userDTO.langKey
                }
                val managedAuthorities = user.authorities
                managedAuthorities.clear()
                Flux.fromIterable(userDTO.authorities!!)
                    .flatMap(authorityRepository::findById)
                    .map(managedAuthorities::add)
                    .then(Mono.just(user))
            }
            .flatMap(this::updateUser)
            .doOnNext { log.debug("Changed Information for User: {}", it) }
            .map { UserDTO(it) }
    }

    private fun updateUser(user: User): Mono<User> {
        return getCurrentUserLogin()
            .switchIfEmpty(Mono.just(SYSTEM_ACCOUNT))
            .flatMap { login ->
                user.lastModifiedBy = login
                userRepository.save(user)
            }
    }

    private fun createUser(user: User): Mono<User> {
        return getCurrentUserLogin()
            .switchIfEmpty(Mono.just(SYSTEM_ACCOUNT))
            .flatMap { login ->
                user.createdBy = login
                user.lastModifiedBy = login
                userRepository.save(user)
            }
    }

    fun deleteUser(login: String): Mono<Void> {
        return userRepository.findOneByLogin(login)
            .flatMap { user -> userRepository.delete(user).thenReturn(user) }
            .doOnNext { user -> log.debug("Deleted User: {}", user) }
            .then()
    }

    fun changePassword(currentClearTextPassword: String, newPassword: String): Mono<Void> {
        return getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .flatMap { user ->
                val currentEncryptedPassword = user.password
                if (!passwordEncoder.matches(currentClearTextPassword, currentEncryptedPassword)) {
                    throw InvalidPasswordException()
                }
                val encryptedPassword = passwordEncoder.encode(newPassword)
                user.password = encryptedPassword
                updateUser(user)
            }
            .doOnNext { user -> log.debug("Changed password for User: {}", user) }
            .then()
    }

    fun getAllManagedUsers(pageable: Pageable): Flux<UserDTO> =
        userRepository.findAllByLoginNot(pageable, ANONYMOUS_USER).map { UserDTO(it) }

    fun countManagedUsers() = userRepository.countAllByLoginNot(ANONYMOUS_USER)

    fun getUserWithAuthoritiesByLogin(login: String): Mono<User> =
        userRepository.findOneByLogin(login)

    @Suppress("unused")
    fun getUserWithAuthorities(id: String): Mono<User> =
        userRepository.findById(id)

    fun getUserWithAuthorities(): Mono<User> =
        getCurrentUserLogin().flatMap(userRepository::findOneByLogin)

    /**
     * Not activated users should be automatically deleted after 3 days.
     *
     * This is scheduled to get fired everyday, at 01:00 (am).
     */
    @Scheduled(cron = "0 0 1 * * ?")
    fun removeNotActivatedUsers() {
        userRepository
            .findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(
                Instant.now().minus(3, ChronoUnit.DAYS)
            )
            .flatMap { user -> userRepository.delete(user).thenReturn(user) }
            .doOnNext { user -> log.debug("Deleted User: {}", user) }
            .blockLast()
    }

    /**
     * @return a list of all the authorities
     */
    fun getAuthorities(): Flux<String> =
        authorityRepository.findAll().map(Authority::name)
}
