package com.apt.todos.repository

import com.apt.todos.domain.PersistentAuditEvent
import java.time.Instant
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Spring Data MongoDB repository for the [PersistentAuditEvent] entity.
 */
interface PersistenceAuditEventRepository : ReactiveMongoRepository<PersistentAuditEvent, String> {

    fun findByPrincipal(principal: String): Flux<PersistentAuditEvent>

    fun findAllByAuditEventDateBetween(
        fromDate: Instant,
        toDate: Instant,
        pageable: Pageable
    ): Flux<PersistentAuditEvent>

    fun findByAuditEventDateBefore(before: Instant): Flux<PersistentAuditEvent>

    fun findAllBy(pageable: Pageable): Flux<PersistentAuditEvent>

    fun countByAuditEventDateBetween(fromDate: Instant, toDate: Instant): Mono<Long>
}
