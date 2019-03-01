package computer.lil.batchwork.model

import android.content.Context
import android.util.Pair
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import computer.lil.batchwork.database.Message
import computer.lil.batchwork.database.SSBDatabase
import java.util.concurrent.TimeUnit

class SSBClient {
    companion object {
        fun getAuthorId(): String {
            return ""
        }

        fun createMessage(context: Context, content: Map<String, String>) {
            val database = SSBDatabase.getInstance(context)

            //val author =
            val lastMessage = database.messageDao().getRecentMessageFromAuthor("")
            val previous = lastMessage.value?.signature
            val sequence = (lastMessage.value?.sequence ?: 0) + 1
            val hash = "sha256"
            val timestamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())

            //val preSignMessage = PreSignatureMessage(previous, author, sequence, timestamp, hash, content)

            //val signature =
        }

        fun signMessage(preSignatureMessage: PreSignatureMessage): String {
            val moshi = Moshi.Builder().build()
            val adapter: JsonAdapter<PreSignatureMessage> = moshi.adapter(
                SSBClient.PreSignatureMessage::class.java)
            val json = adapter.toJson(preSignatureMessage)
            return ""
        }
    }

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
    data class PreSignatureMessage(
        val previous: String?,
        val author: String,
        val sequence: Int,
        val timestamp: Long,
        val hash: String,
        val content: Map<String, Any>
    )

    @JsonClass(generateAdapter = true)
    data class Error(
        val name: String,
        val message: String,
        val stack: String
    )
}