package computer.lil.quilt.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.protocol.Constants
import computer.lil.quilt.protocol.Crypto.Companion.toByteString
import java.util.*

@JsonClass(generateAdapter = true)
class MessageModel(
    var previous: Identifier?,
    var sequence: Int,
    var author: Identifier,
    var timestamp: Date,
    var hash: String = Identifier.AlgoType.SHA256.algo,
    val content: Content,
    var signature: String? = null
) {
    constructor(
        previous: Identifier?,
        sequence: Int,
        author: Identifier,
        timestamp: Date,
        hash: String = Identifier.AlgoType.SHA256.algo,
        content: Content,
        moshi: Moshi,
        identityHandler: IdentityHandler
    ) : this(previous, sequence, author, timestamp, hash, content, null) {
        val signatureRegex = Regex(",\\s*\"signature\":\\s*((\"[a-zA-Z0-9+/]*={0,3}.sig.[a-zA-Z0-9+/]+\")|null)")

        val json = moshi.adapter(MessageModel::class.java).indent("  ").toJson(this)
        val removedSignature = signatureRegex.replace(json, "")

        val newSig = identityHandler.signUsingIdentity(removedSignature)
        val encodedSig = newSig.base64()
        signature = "$encodedSig.sig.ed25519"
    }

    val moshi = Constants.getMoshiInstance()

    fun createMessageId(): Identifier {
        val encodedId = moshi.adapter(MessageModel::class.java)
                                .indent("  ")
                                .toJson(this)
                                .toByteArray()
                                .toByteString()
                                .sha256()
                                .base64()
        return Identifier(encodedId, Identifier.AlgoType.SHA256, Identifier.IdentityType.MESSAGE)
    }

    fun toJson(): String {
        return moshi.adapter(MessageModel::class.java).toJson(this)
    }
}