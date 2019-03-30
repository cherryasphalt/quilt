package computer.lil.quilt.identity

import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.exceptions.SodiumException
import com.goterl.lazycode.lazysodium.interfaces.Sign
import com.goterl.lazycode.lazysodium.utils.Key
import com.goterl.lazycode.lazysodium.utils.KeyPair
import computer.lil.quilt.model.Identifier
import computer.lil.quilt.protocol.Crypto
import okio.ByteString
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

class BasicIdentityHandler(): IdentityHandler {
    companion object {
        const val KEY_ALGO = "ed25519"

        fun createWithGeneratedKeys(): BasicIdentityHandler {
            val handler = BasicIdentityHandler()
            if (handler.generateIdentityKeyPair())
                return handler
            throw IdentityHandler.IdentityException("Failed generating keys.")
        }
    }

    private val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)
    private var keyPair: KeyPair? = null

    constructor(publicKey: ByteString, privateKey: ByteString) : this() {
        keyPair = KeyPair(Key.fromBytes(publicKey.toByteArray()), Key.fromBytes(privateKey.toByteArray()))
    }

    constructor(privateKey: ByteString) : this(Crypto.derivePublicKey(privateKey), privateKey)

    override fun generateIdentityKeyPair(): Boolean {
        if (keyPair != null)
            return false
        return try {
            keyPair = ls.cryptoSignSeedKeypair(SecureRandom().generateSeed(Sign.SEEDBYTES))
            true
        } catch (e: SodiumException) {
            false
        }
    }

    override fun getIdentityPublicKey(): ByteString {
        keyPair?.publicKey?.asBytes?.let { return ByteString.of(*it) }
        throw IdentityHandler.IdentityException("Identity not found.")
    }

    override fun getIdentifier(): Identifier {
        return Identifier(getIdentityPublicKey().base64(), Identifier.AlgoType.ED25519, Identifier.IdentityType.IDENTITY)
    }

    override fun getIdentityString(): String {
        return "@${getIdentityPublicKey().base64()}.$KEY_ALGO"
    }

    override fun signUsingIdentity(message: ByteString): ByteString {
        val signature = ByteArray(Sign.BYTES)
        val signatureLength = LongArray(1)
        ls.cryptoSignDetached(signature, signatureLength, message.toByteArray(), message.size.toLong(), keyPair?.secretKey?.asBytes)

        return ByteString.of(*signature.sliceArray(0 until signatureLength[0].toInt()))
    }

    override fun signUsingIdentity(message: String, charset: Charset): ByteString {
        return signUsingIdentity(ByteString.of(*message.toByteArray(charset)))
    }

    override fun keyExchangeUsingIdentitySecret(exchangePublicKey: ByteString): ByteString {
        val curve25519ClientSecretKey = ByteArray(Sign.CURVE25519_SECRETKEYBYTES)
        ls.convertSecretKeyEd25519ToCurve25519(curve25519ClientSecretKey, keyPair?.secretKey?.asBytes)
        return ByteString.of(*ls.cryptoScalarMult(Key.fromBytes(curve25519ClientSecretKey), Key.fromBytes(exchangePublicKey.toByteArray())).asBytes)
    }
}