package com.apt.todos.repository

import com.apt.todos.domain.Authority
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

/**
 * Spring Data MongoDB repository for the [Authority] entity.
 */

interface AuthorityRepository : ReactiveMongoRepository<Authority, String>
