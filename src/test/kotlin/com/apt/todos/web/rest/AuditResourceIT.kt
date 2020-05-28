package com.apt.todos.web.rest

import com.apt.todos.TodosApp
import com.apt.todos.domain.PersistentAuditEvent
import com.apt.todos.repository.PersistenceAuditEventRepository
import com.apt.todos.security.ADMIN
import java.time.Instant
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient

private const val SAMPLE_PRINCIPAL = "SAMPLE_PRINCIPAL"
private const val SAMPLE_TYPE = "SAMPLE_TYPE"
private val SAMPLE_TIMESTAMP = Instant.parse("2015-08-04T10:11:30Z")
private const val SECONDS_PER_DAY = (60 * 60 * 24).toLong()

/**
 * Integration tests for the [AuditResource] REST controller.
 */
@AutoConfigureWebTestClient
@WithMockUser(authorities = [ADMIN])
@SpringBootTest(classes = [TodosApp::class])
class AuditResourceIT {

    @Autowired
    private lateinit var auditEventRepository: PersistenceAuditEventRepository

    private lateinit var auditEvent: PersistentAuditEvent

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun initTest() {
        auditEventRepository.deleteAll().block()
        auditEvent = PersistentAuditEvent(
            auditEventType = SAMPLE_TYPE,
            principal = SAMPLE_PRINCIPAL,
            auditEventDate = SAMPLE_TIMESTAMP
        )
    }

    @Test

    fun getAllAudits() {
        // Initialize the database
        auditEventRepository.save(auditEvent).block()

        // Get all the audits
        webTestClient.get().uri("/management/audits")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().jsonPath("$.[*].principal").value(hasItem(SAMPLE_PRINCIPAL))
    }

    @Test

    fun getAudit() {
        // Initialize the database
        auditEventRepository.save(auditEvent).block()

        // Get the audit
        webTestClient.get().uri("/management/audits/{id}", auditEvent.id)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().jsonPath("$.principal").isEqualTo(SAMPLE_PRINCIPAL)
    }

    @Test

    fun getAuditsByDate() {
        // Initialize the database
        auditEventRepository.save(auditEvent).block()

        // Generate dates for selecting audits by date, making sure the period will contain the audit
        val fromDate = SAMPLE_TIMESTAMP.minusSeconds(SECONDS_PER_DAY).toString().substring(0, 10)
        val toDate = SAMPLE_TIMESTAMP.plusSeconds(SECONDS_PER_DAY).toString().substring(0, 10)

        // Get the audit
        webTestClient.get().uri("/management/audits?fromDate=" + fromDate + "&toDate=" + toDate)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().jsonPath("$.[*].principal").value(hasItem(SAMPLE_PRINCIPAL))
    }

    @Test

    fun getNonExistingAuditsByDate() {
        // Initialize the database
        auditEventRepository.save(auditEvent).block()

        // Generate dates for selecting audits by date, making sure the period will not contain the sample audit
        val fromDate = SAMPLE_TIMESTAMP.minusSeconds(2 * SECONDS_PER_DAY).toString().substring(0, 10)
        val toDate = SAMPLE_TIMESTAMP.minusSeconds(SECONDS_PER_DAY).toString().substring(0, 10)

        // Query audits but expect no results
        webTestClient.get().uri("/management/audits?fromDate=" + fromDate + "&toDate=" + toDate)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectHeader().valueEquals("X-Total-Count", "0")
    }

    @Test

    fun getNonExistingAudit() {
        // Get the audit
        webTestClient.get().uri("/management/audits/{id}", Long.MAX_VALUE)
            .exchange()
            .expectStatus().isNotFound()
    }

    @Test

    fun testPersistentAuditEventEquals() {
        equalsVerifier(PersistentAuditEvent::class)
        val auditEvent1 = PersistentAuditEvent(id = "id1")
        val auditEvent2 = PersistentAuditEvent(id = auditEvent1.id)
        assertThat(auditEvent1).isEqualTo(auditEvent2)
        auditEvent2.id = "id2"
        assertThat(auditEvent1).isNotEqualTo(auditEvent2)
        auditEvent1.id = null
        assertThat(auditEvent1).isNotEqualTo(auditEvent2)
    }
}
