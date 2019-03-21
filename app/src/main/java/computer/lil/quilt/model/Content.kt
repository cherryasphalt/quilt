package computer.lil.quilt.model

import com.squareup.moshi.JsonClass

open class Content(
    open val type: String?
) {
    @JsonClass(generateAdapter = true)
    data class Post(
        override val type: String,
        val text: String,
        val root: Identifier?,
        val branch: Identifier?,
        val mentions: List<Mention>?,
        val channel: String?
    ): Content(type) {
        @JsonClass(generateAdapter = true)
        data class Mention(
            val link: Identifier,
            val name: String
        )
    }

    @JsonClass(generateAdapter = true)
    data class Pub(
        override val type: String,
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
    data class Contact(
        override val type: String,
        val contact: Identifier,
        val following: Boolean
    ) : Content(type)

    @JsonClass(generateAdapter = true)
    data class PrivateMessage(
        val box: String
    ) : Content(null)
}