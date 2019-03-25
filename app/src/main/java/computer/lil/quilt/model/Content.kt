package computer.lil.quilt.model

import com.squareup.moshi.JsonClass

open class Content(
    open val type: String?
) {
    @JsonClass(generateAdapter = true)
    data class Post(
        override val type: String = "post",
        val text: String,
        val root: Identifier? = null,
        val branch: Identifier? = null,
        val mentions: List<Mention>? = null,
        val channel: String? = null
    ): Content(type) {
        @JsonClass(generateAdapter = true)
        data class Mention(
            val link: Identifier,
            val name: String
        )
    }

    @JsonClass(generateAdapter = true)
    data class Pub(
        override val type: String = "pub",
        val address: Address
    ): Content(type) {
        @JsonClass(generateAdapter = true)
        data class Address(
            val host: String,
            val port: Int,
            val key: String
        )
    }

    @JsonClass(generateAdapter = true)
    data class About(
        override val type: String = "about",
        val about: Identifier,
        val image: Identifier,
        val name: String
    ) : Content(type)

    @JsonClass(generateAdapter = true)
    data class Contact(
        override val type: String = "contact",
        val contact: Identifier,
        val following: Boolean
    ) : Content(type)

    @JsonClass(generateAdapter = true)
    data class PrivateMessage(
        val box: String
    ) : Content(null)
}