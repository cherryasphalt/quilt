package computer.lil.quilt.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RequestModel(
    val method: List<String>,
    val type: RequestType,
    val args: List<Any>
)

enum class RequestType(val type: String) {
    SOURCE("source"),
    ASYNC("async")
}
