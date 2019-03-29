package computer.lil.quilt.identity

import computer.lil.quilt.model.Identifier
import okio.ByteString
import java.lang.Exception
import java.nio.charset.Charset

interface IdentityHandler {
    class IdentityException(message:String): Exception(message)

    fun generateIdentityKeyPair(): Boolean
    fun getIdentityPublicKey(): ByteString
    fun getIdentityString(): String
    fun signUsingIdentity(message: ByteString): ByteString
    fun signUsingIdentity(message: String, charset: Charset = Charsets.UTF_8): ByteString
    fun keyExchangeUsingIdentitySecret(exchangePublicKey: ByteString): ByteString
    fun getIdentifier(): Identifier
}