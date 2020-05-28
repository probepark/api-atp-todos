package com.apt.todos.config.dbmigrations

import com.apt.todos.domain.Authority
import com.apt.todos.domain.User
import com.apt.todos.security.ADMIN
import com.apt.todos.security.USER
import com.github.mongobee.changeset.ChangeLog
import com.github.mongobee.changeset.ChangeSet
import java.time.Instant
import org.springframework.data.mongodb.core.MongoTemplate

/**
 * Creates the initial database setup.
 */
@ChangeLog(order = "001")
@Suppress("unused")
class InitialSetupMigration {

    @ChangeSet(order = "01", author = "initiator", id = "01-addAuthorities")
    fun addAuthorities(mongoTemplate: MongoTemplate) {
        val adminAuthority = Authority(ADMIN)
        val userAuthority = Authority(USER)

        mongoTemplate.save(adminAuthority)
        mongoTemplate.save(userAuthority)
    }

    @ChangeSet(order = "02", author = "initiator", id = "02-addUsers")
    fun addUsers(mongoTemplate: MongoTemplate) {
        val adminAuthority = Authority(ADMIN)
        val userAuthority = Authority(USER)

        val systemUser = User(
            id = "user-0",
            login = "system",
            password = "\$2a\$10\$mE.qmcV0mFU5NcKh73TZx.z4ueI/.bDWbj0T1BYyqP481kGGarKLG",
            firstName = "",
            lastName = "System",
            email = "system@localhost",
            activated = true,
            langKey = "en",
            createdBy = "system",
            createdDate = Instant.now(),
            authorities = mutableSetOf(adminAuthority, userAuthority)
        )
        mongoTemplate.save(systemUser)

        val anonymousUser = User(
            id = "user-1",
            login = "anonymoususer",
            password = "\$2a\$10\$j8S5d7Sr7.8VTOYNviDPOeWX8KcYILUVJBsYV83Y5NtECayypx9lO",
            firstName = "Anonymous",
            lastName = "User",
            email = "anonymous@localhost",
            activated = true,
            langKey = "en",
            createdBy = systemUser.login,
            createdDate = Instant.now()
        )
        mongoTemplate.save(anonymousUser)

        val adminUser = User(
            id = "user-2",
            login = "admin",
            password = "\$2a\$10\$gSAhZrxMllrbgj/kkK9UceBPpChGWJA7SYIb1Mqo.n5aNLq1/oRrC",
            firstName = "admin",
            lastName = "Administrator",
            email = "admin@localhost",
            activated = true,
            langKey = "en",
            createdBy = systemUser.login,
            createdDate = Instant.now(),
            authorities = mutableSetOf(adminAuthority, userAuthority)
        )
        mongoTemplate.save(adminUser)

        val userUser = User(
            id = "user-3",
            login = "user",
            password = "\$2a\$10\$VEjxo0jq2YG9Rbk2HmX9S.k1uZBGYUHdUcid3g/vfiEl7lwWgOH/K",
            firstName = "",
            lastName = "User",
            email = "user@localhost",
            activated = true,
            langKey = "en",
            createdBy = systemUser.login,
            createdDate = Instant.now(),
            authorities = mutableSetOf(userAuthority)
        )
        mongoTemplate.save(userUser)
    }
}
