package computer.lil.quilt.model

import android.util.Base64
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import computer.lil.quilt.identity.IdentityHandler
import java.util.*

@JsonClass(generateAdapter = true)
class MessageModel(
    var previous: Identifier?,
    var sequence: Int,
    var author: Identifier,
    var timestamp: Date,
    var hash: String,
    val content: Content,
    var signature: String? = null
) {
    constructor(
        previous: Identifier?,
        sequence: Int,
        author: Identifier,
        timestamp: Date,
        hash: String,
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
}