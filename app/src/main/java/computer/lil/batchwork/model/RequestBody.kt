package computer.lil.batchwork.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RequestBody(
    val method: List<String>,
    val type: String,
    val args: List<Any>
)