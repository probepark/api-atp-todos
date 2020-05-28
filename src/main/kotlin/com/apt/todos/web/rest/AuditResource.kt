package com.apt.todos.web.rest

import com.apt.todos.service.AuditEventService
import io.github.jhipster.web.util.PaginationUtil
import java.time.LocalDate
import java.time.ZoneId
import org.springframework.boot.actuate.audit.AuditEvent
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.switchIfEmpty

/**
 * REST controller for getting the `AuditEvent`s.
 */
@RestController
@RequestMapping("/management/audits")
class AuditResource(private val auditEventService: AuditEventService) {

    /**
     * `GET /audits` : get a page of `AuditEvent`s.
     *
     * @param request a [ServerHttpRequest] request.
     * @param pageable the pagination information.
     * @return the `ResponseEntity` with status `200 (OK)` and the list of `AuditEvent`s in body.
     */
    @GetMapping
    fun getAll(request: ServerHttpRequest, pageable: Pageable): Mono<ResponseEntity<Flux<AuditEvent>>> {
        return auditEventService.count()
            .map { PageImpl<AuditEvent>(listOf(), pageable, it) }
            .map { PaginationUtil.generatePaginationHttpHeaders(UriComponentsBuilder.fromHttpRequest(request), it) }
            .map { ResponseEntity.ok().headers(it).body(auditEventService.findAll(pageable)) }
    }

    /**
     * `GET  /audits` : get a page of `AuditEvent`s between the `fromDate` and `toDate`.
     *
     * @param fromDate the start of the time period of `AuditEvent`s to get.
     * @param toDate the end of the time period of `AuditEvent`s to get.
     * @param request a [ServerHttpRequest] request.
     * @param pageable the pagination information.
     * @return the `ResponseEntity` with status `200 (OK)` and the list of `AuditEvent`s in body.
     */
    @GetMapping(params = ["fromDate", "toDate"])
    fun getByDates(
        @RequestParam(value = "fromDate") fromDate: LocalDate,
        @RequestParam(value = "toDate") toDate: LocalDate,
        request: ServerHttpRequest,
        pageable: Pageable
    ): Mono<ResponseEntity<Flux<AuditEvent>>> {

        val from = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val to = toDate.atStartOfDay(ZoneId.systemDefault()).plusDays(1).toInstant()

        val events = auditEventService.findByDates(from, to, pageable)
        return auditEventService.countByDates(from, to)
            .map { PageImpl<AuditEvent>(listOf(), pageable, it) }
            .map { PaginationUtil.generatePaginationHttpHeaders(UriComponentsBuilder.fromHttpRequest(request), it) }
            .map { ResponseEntity.ok().headers(it).body(events) }
    }

    /**
     * `GET  /audits/:id` : get an `AuditEvent` by id.
     *
     * @param id the id of the entity to get.
     * @return the `ResponseEntity` with status `200 (OK)` and the AuditEvent in body, or status `404 (Not Found)`.
     */
    @GetMapping("/{id:.+}")
    fun get(@PathVariable id: String?) = auditEventService.find(id!!)
        .switchIfEmpty { Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND)) }
}
