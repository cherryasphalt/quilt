package computer.lil.batchwork.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.sun.jna.StringArray

class SSBClient() {
    enum class RequestType() {
        @Json(name="async") ASYNC(),
        @Json(name="source") SOURCE()
    }

    @JsonClass(generateAdapter = true)
    data class Request(
        val name: List<String>,
        val type: RequestType,
        val args: Map<String, Any>
    )

    @JsonClass(generateAdapter = true)
    data class Response(
        val key: String,
        val value: Message,
        val timestamp: Long
    )

    @JsonClass(generateAdapter = true)
    data class Message(
        val previous: String,
        val author: String,
        val sequence: Int,
        val timestamp: Long,
        val hash: String,
        val content: List<Map<String, Any>>,
        val signature: String
    )

    @JsonClass(generateAdapter = true)
    data class Error(
        val name: String,
        val message: String,
        val stack: String
    )
}