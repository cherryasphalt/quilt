package computer.lil.quilt.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessageModel(
    var previous: String,
    var author: String,
    var sequence: Int,
    var timestamp: Long,
    var hash: String,
    val content: Map<String, Any>,
    val signature: String
)