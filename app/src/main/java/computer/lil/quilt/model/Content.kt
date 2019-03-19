package computer.lil.quilt.model

import com.squareup.moshi.JsonClass

interface Content {
    @JsonClass(generateAdapter = true)
    data class Post(
        val text: String,
        val root: String?,
        val branch: List<String>?,
        val mentions: List<Mention>?
    ): Content {
        @JsonClass(generateAdapter = true)
        data class Mention(
            val link: String,
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
        val contact: String,
        val following: Boolean
    ) : Content

    @JsonClass(generateAdapter = true)
    data class PrivateMessage(
        val box: String
    ) : Content
}