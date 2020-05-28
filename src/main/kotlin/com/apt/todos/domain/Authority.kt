package com.apt.todos.domain

import java.io.Serializable
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * An authority (a security role) used by Spring Security.
 */
@Document(collection = "jhi_authority")
data class Authority(

    @field:NotNull
    @field:Size(max = 50)
    @Id
    var name: String? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Authority) return false
        if (other.name == null || name == null) return false

        return name == other.name
    }

    override fun hashCode() = 31

    companion object {
        private const val serialVersionUID = 1L
    }
}
