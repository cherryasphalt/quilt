package computer.lil.quilt.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessageModel(
    var previous: String?,
    var author: Identifier,
    var sequence: Int,
    var timestamp: Long,
    var hash: String,
    val content: Content,
    val signature: String
)