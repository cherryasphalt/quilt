package computer.lil.quilt.database

interface Content {
    data class Post(
        val text: String,
        val root: String,
        val branch: List<String>,
        val mentions: List<Mention>
    ): Content {
        data class Mention(
            val link: String,
            val name: String
        )
    }

    data class Pub(
        val address: Address
    ): Content {
        data class Address(
            val host: String,
            val port: Int,
            val key: String
        )
    }

    data class Contact(
        val contact: String,
        val following: Boolean
    )

    data class PrivateMessage(
        val box: String
    )
}