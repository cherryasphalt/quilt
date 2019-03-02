package computer.lil.batchwork.model

import android.content.Context
import android.util.Base64
import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.Hash
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import computer.lil.batchwork.database.Message
import computer.lil.batchwork.database.SSBDatabase
import computer.lil.batchwork.identity.AndroidKeyStoreIdentityHandler
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class SSBClient {
    companion object {
        private val moshi = Moshi.Builder().build()

        fun createMessage(context: Context, content: Map<String, String>): Message {
            val database = SSBDatabase.getInstance(context)

            val author = AndroidKeyStoreIdentityHandler(context).getIdentityString()

            val lastMessage = database.messageDao().getRecentMessageFromAuthor(author).value
            val previous = lastMessage?.id
            val sequence = (lastMessage?.sequence ?: 0) + 1

            val hash = "sha256"
            val timestamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())

            val preSignMessage = PreSignatureMessage(previous, author, sequence, timestamp, hash, content)
            val signature = signMessage(context, preSignMessage)

            val message = Message("", previous!!, author, sequence, timestamp, hash, content.toString(), signature)
            message.id = createMessageId(message)

            return message
        }

        fun signMessage(context: Context, preSignatureMessage: PreSignatureMessage): String {
            val adapter: JsonAdapter<PreSignatureMessage> = moshi.adapter(
                SSBClient.PreSignatureMessage::class.java)
            val json = adapter.toJson(preSignatureMessage)
            val signature = Base64.encodeToString(AndroidKeyStoreIdentityHandler(context).signUsingIdentity(json), Base64.DEFAULT)
            return "$signature.sig.ed25519"
        }

        fun createMessageId(message: Message): String {
            val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)
            val moshi = Moshi.Builder().build()
            val adapter: JsonAdapter<Message> = moshi.adapter(Message::class.java)
            val idByteArray = ByteArray(Hash.SHA256_BYTES)
            val jsonMessage = adapter.toJson(message).toByteArray()
            ls.cryptoHashSha256(idByteArray, jsonMessage, jsonMessage.size.toLong())
            val id = Base64.encodeToString(idByteArray, Base64.DEFAULT)
            return "%$id.sha256"
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