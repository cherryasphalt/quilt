package computer.lil.quilt.identity

import java.lang.Exception
import java.nio.charset.Charset

interface IdentityHandler {
    class IdentityException(message:String): Exception(message)

    fun generateIdentityKeyPair(): Boolean
    fun getIdentityPublicKey(): ByteArray
    fun getIdentityString(): String
    fun signUsingIdentity(message: ByteArray): ByteArray
    fun signUsingIdentity(message: String, charset: Charset = Charsets.UTF_8): ByteArray
    fun keyExchangeUsingIdentitySecret(exchangePublicKey: ByteArray): ByteArray
}