package computer.lil.quilt.model

import android.util.Base64
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.injection.DataComponent
import computer.lil.quilt.injection.DataModule
import computer.lil.quilt.util.Crypto
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
        val encodedSig = Base64.encodeToString(newSig, Base64.NO_WRAP)
        signature = "$encodedSig.sig.ed25519"
    }

    fun createMessageId(): Identifier {
        val moshi = Moshi.Builder()
            .add(Identifier.IdentifierJsonAdapter())
            .add(Adapters.DataTypeAdapter())
            .add(RPCJsonAdapterFactory())
            .add(
                PolymorphicJsonAdapterFactory.of(Content::class.java, "type")
                    .withSubtype(Content.Post::class.java, "post")
                    .withSubtype(Content.Pub::class.java, "pub")
                    .withSubtype(Content.Contact::class.java, "contact")
            ).build()
        val json = moshi.adapter(MessageModel::class.java).indent("  ").toJson(this)
        val id = Crypto.sha256(json)
        val encodedId = Base64.encodeToString(id, Base64.NO_WRAP)

        return Identifier(encodedId, Identifier.AlgoType.SHA256, Identifier.IdentityType.MESSAGE)
    }
}