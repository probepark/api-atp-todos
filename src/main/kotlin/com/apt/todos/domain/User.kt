package com.apt.todos.domain

import com.apt.todos.config.LOGIN_REGEX
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable
import java.time.Instant
import java.util.Locale
import javax.validation.constraints.Email
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Field

/**
 * A user.
 */
@org.springframework.data.mongodb.core.mapping.Document(collection = "jhi_user")
class User @JvmOverloads constructor(

    @Id
    var id: String? = null,

    login: String? = null,

    @JsonIgnore
    @field:NotNull
    @field:Size(min = 60, max = 60)
    var password: String? = null,

    @field:Size(max = 50)
    @Field("first_name")
    var firstName: String? = null,

    @field:Size(max = 50)
    @Field("last_name")
    var lastName: String? = null,

    @field:Email
    @field:Size(min = 5, max = 254)
    @Indexed
    var email: String? = null,

    var activated: Boolean = false,

    @field:Size(min = 2, max = 10)
    @Field("lang_key")
    var langKey: String? = null,

    @field:Size(max = 256)
    @Field("image_url")
    var imageUrl: String? = null,

    @field:Size(max = 20)
    @Field("activation_key")
    @JsonIgnore
    var activationKey: String? = null,

    @field:Size(max = 20)
    @Field("reset_key")
    @JsonIgnore
    var resetKey: String? = null,

    @Field("reset_date")
    var resetDate: Instant? = null,

    @JsonIgnore
    var authorities: MutableSet<Authority> = mutableSetOf(),

    createdBy: String? = null,
    createdDate: Instant? = Instant.now(),
    lastModifiedBy: String? = null,
    lastModifiedDate: Instant? = Instant.now()
) : AbstractAuditingEntity(createdBy, createdDate, lastModifiedBy, lastModifiedDate), Serializable {

    @NotNull
    @field:Pattern(regexp = LOGIN_REGEX)
    @field:Size(min = 1, max = 50)
    @Indexed
    var login: String? = login
        set(value) {
            // Lowercase the login before saving it in database
            field = value?.toLowerCase(Locale.ENGLISH)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        if (other.id == null || id == null) return false

        return id == other.id
    }

    override fun hashCode() = 31

    override fun toString() =
        "User{" +
            "login='" + login + '\'' +
            ", firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", email='" + email + '\'' +
            ", imageUrl='" + imageUrl + '\'' +
            ", activated='" + activated + '\'' +
            ", langKey='" + langKey + '\'' +
            ", activationKey='" + activationKey + '\'' +
            "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}
