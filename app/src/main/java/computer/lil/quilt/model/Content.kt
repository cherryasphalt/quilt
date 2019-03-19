package computer.lil.quilt.model

import com.squareup.moshi.JsonClass

interface Content {
    @JsonClass(generateAdapter = true)
    data class Post(
        val text: String,
        val root: Identifier?,
        val branch: Identifier?,
        val mentions: List<Mention>?,
        val channel: String?
    ): Content {
        @JsonClass(generateAdapter = true)
        data class Mention(
            val link: Identifier,
            val name: String
        )
    }

    @JsonClass(generateAdapter = true)
    data class Pub(
        val address: Address
    ): Content {
        @JsonClass(generateAdapter = true)
        data class Address(
            val host: String,
            val port: Int,
            val key: String
        )
    }

    @JsonClass(generateAdapter = true)
    data class Contact(
        val contact: Identifier,
        val following: Boolean
    ) : Content

    @JsonClass(generateAdapter = true)
    data class PrivateMessage(
        val box: String
    ) : Content
}