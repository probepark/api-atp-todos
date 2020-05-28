package com.apt.todos.web.rest

import com.apt.todos.TodosApp
import com.apt.todos.domain.Authority
import com.apt.todos.domain.User
import com.apt.todos.repository.UserRepository
import com.apt.todos.security.ADMIN
import com.apt.todos.security.USER
import com.apt.todos.service.dto.UserDTO
import com.apt.todos.service.mapper.UserMapper
import com.apt.todos.web.rest.vm.ManagedUserVM
import java.time.Instant
import kotlin.test.assertNotNull
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Integration tests for the [UserResource] REST controller.
 */
@AutoConfigureWebTestClient
@WithMockUser(authorities = [ADMIN])
@SpringBootTest(classes = [TodosApp::class])
class UserResourceIT {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userMapper: UserMapper

    @Autowired
    private lateinit var webTestClient: WebTestClient

    private lateinit var user: User

    @BeforeEach
    fun initTest() {
        userRepository.deleteAll().block()
        user = createEntity()
    }

    @Test
    @Throws(Exception::class)
    fun createUser() {
        val databaseSizeBeforeCreate = userRepository.findAll()
            .collectList().block()!!.size

        // Create the User
        val managedUserVM = ManagedUserVM().apply {
            login = DEFAULT_LOGIN
            password = DEFAULT_PASSWORD
            firstName = DEFAULT_FIRSTNAME
            lastName = DEFAULT_LASTNAME
            email = DEFAULT_EMAIL
            activated = true
            imageUrl = DEFAULT_IMAGEURL
            langKey = DEFAULT_LANGKEY
            authorities = setOf(USER)
        }

        webTestClient.post().uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(convertObjectToJsonBytes(managedUserVM))
            .exchange()
            .expectStatus().isCreated

        assertPersistedUsers { userList ->
            // Validate the User in the database
            assertThat(userList).hasSize(databaseSizeBeforeCreate + 1)
            val testUser = userList[userList.size - 1]
            assertThat(testUser.login).isEqualTo(DEFAULT_LOGIN)
            assertThat(testUser.firstName).isEqualTo(DEFAULT_FIRSTNAME)
            assertThat(testUser.lastName).isEqualTo(DEFAULT_LASTNAME)
            assertThat(testUser.email).isEqualTo(DEFAULT_EMAIL)
            assertThat(testUser.imageUrl).isEqualTo(DEFAULT_IMAGEURL)
            assertThat(testUser.langKey).isEqualTo(DEFAULT_LANGKEY)
        }
    }

    @Test
    @Throws(Exception::class)
    fun createUserWithExistingId() {
        val databaseSizeBeforeCreate = userRepository.findAll()
            .collectList().block()!!.size

        val managedUserVM = ManagedUserVM().apply {
            id = "1L"
            login = DEFAULT_LOGIN
            password = DEFAULT_PASSWORD
            firstName = DEFAULT_FIRSTNAME
            lastName = DEFAULT_LASTNAME
            email = DEFAULT_EMAIL
            activated = true
            imageUrl = DEFAULT_IMAGEURL
            langKey = DEFAULT_LANGKEY
            authorities = setOf(USER)
        }

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient.post().uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(convertObjectToJsonBytes(managedUserVM))
            .exchange()
            .expectStatus().isBadRequest

        assertPersistedUsers { userList ->
            // Validate the User in the database
            assertThat(userList).hasSize(databaseSizeBeforeCreate)
        }
    }

    @Test
    @Throws(Exception::class)
    fun createUserWithExistingLogin() {
        // Initialize the database
        userRepository.save(user).block()
        val databaseSizeBeforeCreate = userRepository.findAll()
            .collectList().block()!!.size

        val managedUserVM = ManagedUserVM().apply {
            login = DEFAULT_LOGIN // this login should already be used
            password = DEFAULT_PASSWORD
            firstName = DEFAULT_FIRSTNAME
            lastName = DEFAULT_LASTNAME
            email = "anothermail@localhost"
            activated = true
            imageUrl = DEFAULT_IMAGEURL
            langKey = DEFAULT_LANGKEY
            authorities = setOf(USER)
        }

        // Create the User
        webTestClient.post().uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(convertObjectToJsonBytes(managedUserVM))
            .exchange()
            .expectStatus().isBadRequest

        assertPersistedUsers { userList -> assertThat(userList).hasSize(databaseSizeBeforeCreate) }
    }

    @Test
    @Throws(Exception::class)
    fun createUserWithExistingEmail() {
        // Initialize the database
        userRepository.save(user).block()
        val databaseSizeBeforeCreate = userRepository.findAll()
            .collectList().block()!!.size

        val managedUserVM = ManagedUserVM().apply {
            login = "anotherlogin"
            password = DEFAULT_PASSWORD
            firstName = DEFAULT_FIRSTNAME
            lastName = DEFAULT_LASTNAME
            email = DEFAULT_EMAIL // this email should already be used
            activated = true
            imageUrl = DEFAULT_IMAGEURL
            langKey = DEFAULT_LANGKEY
            authorities = setOf(USER)
        }

        // Create the User
        webTestClient.post().uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(convertObjectToJsonBytes(managedUserVM))
            .exchange()
            .expectStatus().isBadRequest

        assertPersistedUsers { userList -> assertThat(userList).hasSize(databaseSizeBeforeCreate) }
    }

    @Test
    fun getAllUsers() {
        // Initialize the database
        userRepository.save(user).block()

        // Get all the users
        val foundUser = webTestClient.get().uri("/api/users?sort=createdDate,DESC")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .returnResult(UserDTO::class.java).responseBody.blockFirst()

        assertNotNull(foundUser)
        assertThat(foundUser.login).isEqualTo(DEFAULT_LOGIN)
        assertThat(foundUser.firstName).isEqualTo(DEFAULT_FIRSTNAME)
        assertThat(foundUser.lastName).isEqualTo(DEFAULT_LASTNAME)
        assertThat(foundUser.email).isEqualTo(DEFAULT_EMAIL)
        assertThat(foundUser.imageUrl).isEqualTo(DEFAULT_IMAGEURL)
        assertThat(foundUser.langKey).isEqualTo(DEFAULT_LANGKEY)
    }

    @Test
    fun getUser() {
        // Initialize the database
        userRepository.save(user).block()

        // Get the user
        webTestClient.get().uri("/api/users/{login}", user.login)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("\$.login").isEqualTo(user.login)
            .jsonPath("\$.firstName").isEqualTo(DEFAULT_FIRSTNAME)
            .jsonPath("\$.lastName").isEqualTo(DEFAULT_LASTNAME)
            .jsonPath("\$.email").isEqualTo(DEFAULT_EMAIL)
            .jsonPath("\$.imageUrl").isEqualTo(DEFAULT_IMAGEURL)
            .jsonPath("\$.langKey").isEqualTo(DEFAULT_LANGKEY)
    }

    @Test
    fun getNonExistingUser() {
        webTestClient.get().uri("/api/users/unknown")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @Throws(Exception::class)
    fun updateUser() {
        // Initialize the database
        userRepository.save(user).block()
        val databaseSizeBeforeUpdate = userRepository.findAll()
            .collectList().block()!!.size

        // Update the user
        val updatedUser = userRepository.findById(user.id!!).block()
        assertNotNull(updatedUser)

        val managedUserVM = ManagedUserVM().apply {
            id = updatedUser.id
            login = updatedUser.login
            password = UPDATED_PASSWORD
            firstName = UPDATED_FIRSTNAME
            lastName = UPDATED_LASTNAME
            email = UPDATED_EMAIL
            activated = updatedUser.activated
            imageUrl = UPDATED_IMAGEURL
            langKey = UPDATED_LANGKEY
            createdBy = updatedUser.createdBy
            createdDate = updatedUser.createdDate
            lastModifiedBy = updatedUser.lastModifiedBy
            lastModifiedDate = updatedUser.lastModifiedDate
            authorities = setOf(USER)
        }

        webTestClient.put().uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(convertObjectToJsonBytes(managedUserVM))
            .exchange()
            .expectStatus().isOk

        assertPersistedUsers { userList ->
            assertThat(userList).hasSize(databaseSizeBeforeUpdate)
            val testUser = userList[userList.size - 1]
            assertThat(testUser.firstName).isEqualTo(UPDATED_FIRSTNAME)
            assertThat(testUser.lastName).isEqualTo(UPDATED_LASTNAME)
            assertThat(testUser.email).isEqualTo(UPDATED_EMAIL)
            assertThat(testUser.imageUrl).isEqualTo(UPDATED_IMAGEURL)
            assertThat(testUser.langKey).isEqualTo(UPDATED_LANGKEY)
        }
    }

    @Test
    @Throws(Exception::class)
    fun updateUserLogin() {
        // Initialize the database
        userRepository.save(user).block()
        val databaseSizeBeforeUpdate = userRepository.findAll()
            .collectList().block()!!.size

        // Update the user
        val updatedUser = userRepository.findById(user.id!!).block()
        assertNotNull(updatedUser)

        val managedUserVM = ManagedUserVM().apply {
            id = updatedUser.id
            login = UPDATED_LOGIN
            password = UPDATED_PASSWORD
            firstName = UPDATED_FIRSTNAME
            lastName = UPDATED_LASTNAME
            email = UPDATED_EMAIL
            activated = updatedUser.activated
            imageUrl = UPDATED_IMAGEURL
            langKey = UPDATED_LANGKEY
            createdBy = updatedUser.createdBy
            createdDate = updatedUser.createdDate
            lastModifiedBy = updatedUser.lastModifiedBy
            lastModifiedDate = updatedUser.lastModifiedDate
            authorities = setOf(USER)
        }

        webTestClient.put().uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(convertObjectToJsonBytes(managedUserVM))
            .exchange()
            .expectStatus().isOk

        assertPersistedUsers { userList ->
            assertThat(userList).hasSize(databaseSizeBeforeUpdate)
            val testUser = userList[userList.size - 1]
            assertThat(testUser.login).isEqualTo(UPDATED_LOGIN)
            assertThat(testUser.firstName).isEqualTo(UPDATED_FIRSTNAME)
            assertThat(testUser.lastName).isEqualTo(UPDATED_LASTNAME)
            assertThat(testUser.email).isEqualTo(UPDATED_EMAIL)
            assertThat(testUser.imageUrl).isEqualTo(UPDATED_IMAGEURL)
            assertThat(testUser.langKey).isEqualTo(UPDATED_LANGKEY)
        }
    }

    @Test
    @Throws(Exception::class)
    fun updateUserExistingEmail() {
        // Initialize the database with 2 users
        userRepository.save(user).block()

        val anotherUser = User(
            login = "jhipster",
            password = RandomStringUtils.random(60),
            activated = true,
            email = "jhipster@localhost",
            firstName = "java",
            lastName = "hipster",
            imageUrl = "",
            langKey = "en"
        )
        userRepository.save(anotherUser).block()

        // Update the user
        val updatedUser = userRepository.findById(user.id!!).block()
        assertNotNull(updatedUser)

        val managedUserVM = ManagedUserVM().apply {
            id = updatedUser.id
            login = updatedUser.login
            password = updatedUser.password
            firstName = updatedUser.firstName
            lastName = updatedUser.lastName
            email = "jhipster@localhost" // this email should already be used by anotherUser
            activated = updatedUser.activated
            imageUrl = updatedUser.imageUrl
            langKey = updatedUser.langKey
            createdBy = updatedUser.createdBy
            createdDate = updatedUser.createdDate
            lastModifiedBy = updatedUser.lastModifiedBy
            lastModifiedDate = updatedUser.lastModifiedDate
            authorities = setOf(USER)
        }

        webTestClient.put().uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(convertObjectToJsonBytes(managedUserVM))
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @Throws(Exception::class)
    fun updateUserExistingLogin() {
        // Initialize the database
        userRepository.save(user).block()

        val anotherUser = User(
            login = "jhipster",
            password = RandomStringUtils.random(60),
            activated = true,
            email = "jhipster@localhost",
            firstName = "java",
            lastName = "hipster",
            imageUrl = "",
            langKey = "en"
        )
        userRepository.save(anotherUser).block()

        // Update the user
        val updatedUser = userRepository.findById(user.id!!).block()
        assertNotNull(updatedUser)

        val managedUserVM = ManagedUserVM().apply {
            id = updatedUser.id
            login = "jhipster" // this login should already be used by anotherUser
            password = updatedUser.password
            firstName = updatedUser.firstName
            lastName = updatedUser.lastName
            email = updatedUser.email
            activated = updatedUser.activated
            imageUrl = updatedUser.imageUrl
            langKey = updatedUser.langKey
            createdBy = updatedUser.createdBy
            createdDate = updatedUser.createdDate
            lastModifiedBy = updatedUser.lastModifiedBy
            lastModifiedDate = updatedUser.lastModifiedDate
            authorities = setOf(USER)
        }

        webTestClient.put().uri("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(convertObjectToJsonBytes(managedUserVM))
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun deleteUser() {
        // Initialize the database
        userRepository.save(user).block()
        val databaseSizeBeforeDelete = userRepository.findAll()
            .collectList().block()!!.size

        // Delete the user
        webTestClient.delete().uri("/api/users/{login}", user.login)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNoContent

        assertPersistedUsers { userList -> assertThat(userList).hasSize(databaseSizeBeforeDelete - 1) }
    }

    @Test
    fun getAllAuthorities() {
        webTestClient.get().uri("/api/users/authorities")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
            .expectBody()
            .jsonPath("\$").isArray
            .jsonPath("\$[?(@=='" + ADMIN + "')]").hasJsonPath()
            .jsonPath("\$[?(@=='" + USER + "')]").hasJsonPath()
    }

    @Test
    @Throws(Exception::class)
    fun testUserEquals() {
        equalsVerifier(User::class)
        val user1 = User(id = "id1")
        val user2 = User(id = user1.id)
        assertThat(user1).isEqualTo(user2)
        user2.id = "id2"
        assertThat(user1).isNotEqualTo(user2)
        user1.id = null
        assertThat(user1).isNotEqualTo(user2)
    }

    @Test
    fun testUserDTOtoUser() {
        val userDTO = UserDTO(
            id = DEFAULT_ID,
            login = DEFAULT_LOGIN,
            firstName = DEFAULT_FIRSTNAME,
            lastName = DEFAULT_LASTNAME,
            email = DEFAULT_EMAIL,
            activated = true,
            imageUrl = DEFAULT_IMAGEURL,
            langKey = DEFAULT_LANGKEY,
            createdBy = DEFAULT_LOGIN,
            lastModifiedBy = DEFAULT_LOGIN,
            authorities = setOf(USER)
        )

        val user = userMapper.userDTOToUser(userDTO)
        assertNotNull(user)
        assertThat(user.id).isEqualTo(DEFAULT_ID)
        assertThat(user.login).isEqualTo(DEFAULT_LOGIN)
        assertThat(user.firstName).isEqualTo(DEFAULT_FIRSTNAME)
        assertThat(user.lastName).isEqualTo(DEFAULT_LASTNAME)
        assertThat(user.email).isEqualTo(DEFAULT_EMAIL)
        assertThat(user.activated).isEqualTo(true)
        assertThat(user.imageUrl).isEqualTo(DEFAULT_IMAGEURL)
        assertThat(user.langKey).isEqualTo(DEFAULT_LANGKEY)
        assertThat(user.createdBy).isNull()
        assertThat(user.createdDate).isNotNull()
        assertThat(user.lastModifiedBy).isNull()
        assertThat(user.lastModifiedDate).isNotNull()
        assertThat(user.authorities).extracting("name").containsExactly(USER)
    }

    @Test
    fun testUserToUserDTO() {
        user.id = DEFAULT_ID
        user.createdBy = DEFAULT_LOGIN
        user.createdDate = Instant.now()
        user.lastModifiedBy = DEFAULT_LOGIN
        user.lastModifiedDate = Instant.now()
        user.authorities = mutableSetOf(Authority(name = USER))

        val userDTO = userMapper.userToUserDTO(user)

        assertThat(userDTO.id).isEqualTo(DEFAULT_ID)
        assertThat(userDTO.login).isEqualTo(DEFAULT_LOGIN)
        assertThat(userDTO.firstName).isEqualTo(DEFAULT_FIRSTNAME)
        assertThat(userDTO.lastName).isEqualTo(DEFAULT_LASTNAME)
        assertThat(userDTO.email).isEqualTo(DEFAULT_EMAIL)
        assertThat(userDTO.isActivated()).isEqualTo(true)
        assertThat(userDTO.imageUrl).isEqualTo(DEFAULT_IMAGEURL)
        assertThat(userDTO.langKey).isEqualTo(DEFAULT_LANGKEY)
        assertThat(userDTO.createdBy).isEqualTo(DEFAULT_LOGIN)
        assertThat(userDTO.createdDate).isEqualTo(user.createdDate)
        assertThat(userDTO.lastModifiedBy).isEqualTo(DEFAULT_LOGIN)
        assertThat(userDTO.lastModifiedDate).isEqualTo(user.lastModifiedDate)
        assertThat(userDTO.authorities).containsExactly(USER)
        assertThat(userDTO.toString()).isNotNull()
    }

    @Test
    fun testAuthorityEquals() {
        val authorityA = Authority()
        assertThat(authorityA).isEqualTo(authorityA)
        assertThat(authorityA).isNotEqualTo(null)
        assertThat(authorityA).isNotEqualTo(Any())
        assertThat(authorityA.hashCode()).isEqualTo(31)
        assertThat(authorityA.toString()).isNotNull()

        val authorityB = Authority()
        assertThat(authorityA.name).isEqualTo(authorityB.name)

        authorityB.name = ADMIN
        assertThat(authorityA).isNotEqualTo(authorityB)

        authorityA.name = USER
        assertThat(authorityA).isNotEqualTo(authorityB)

        authorityB.name = USER
        assertThat(authorityA).isEqualTo(authorityB)
        assertThat(authorityA.hashCode()).isEqualTo(authorityB.hashCode())
    }

    companion object {

        private const val DEFAULT_LOGIN = "johndoe"
        private const val UPDATED_LOGIN = "jhipster"

        private const val DEFAULT_ID = "id1"

        private const val DEFAULT_PASSWORD = "passjohndoe"
        private const val UPDATED_PASSWORD = "passjhipster"

        private const val DEFAULT_EMAIL = "johndoe@localhost"
        private const val UPDATED_EMAIL = "jhipster@localhost"

        private const val DEFAULT_FIRSTNAME = "john"
        private const val UPDATED_FIRSTNAME = "jhipsterFirstName"

        private const val DEFAULT_LASTNAME = "doe"
        private const val UPDATED_LASTNAME = "jhipsterLastName"

        private const val DEFAULT_IMAGEURL = "http://placehold.it/50x50"
        private const val UPDATED_IMAGEURL = "http://placehold.it/40x40"

        private const val DEFAULT_LANGKEY = "en"
        private const val UPDATED_LANGKEY = "fr"

        /**
         * Create a User.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which has a required relationship to the User entity.
         */
        @JvmStatic
        fun createEntity(): User {
            return User(
                login = DEFAULT_LOGIN,
                password = RandomStringUtils.random(60),
                activated = true,
                email = DEFAULT_EMAIL,
                firstName = DEFAULT_FIRSTNAME,
                lastName = DEFAULT_LASTNAME,
                imageUrl = DEFAULT_IMAGEURL,
                langKey = DEFAULT_LANGKEY
            )
        }
    }

    fun assertPersistedUsers(userAssertion: (List<User>) -> Unit) {
        userAssertion(userRepository.findAll().collectList().block())
    }
}
