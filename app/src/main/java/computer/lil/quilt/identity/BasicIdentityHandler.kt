package computer.lil.quilt.identity

import android.util.Base64
import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.exceptions.SodiumException
import com.goterl.lazycode.lazysodium.interfaces.Sign
import com.goterl.lazycode.lazysodium.utils.Key
import com.goterl.lazycode.lazysodium.utils.KeyPair
import computer.lil.quilt.model.Identifier
import computer.lil.quilt.util.Crypto
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

    constructor(publicKey: ByteArray, privateKey: ByteArray) : this() {
        keyPair = KeyPair(Key.fromBytes(publicKey), Key.fromBytes(privateKey))
    }

    constructor(privateKey: ByteArray) : this(Crypto.derivePublicKey(privateKey), privateKey)

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

    override fun getIdentityPublicKey(): ByteArray {
        keyPair?.publicKey?.asBytes?.let { return it }
        throw IdentityHandler.IdentityException("Identity not found.")
    }

    override fun getIdentifier(): Identifier {
        return Identifier(Base64.encodeToString(getIdentityPublicKey(), Base64.NO_WRAP), Identifier.AlgoType.ED25519, Identifier.IdentityType.IDENTITY)
    }

    override fun getIdentityString(): String {
        return "@${Base64.encodeToString(getIdentityPublicKey(), Base64.NO_WRAP)}.$KEY_ALGO"
    }

    override fun signUsingIdentity(message: ByteArray): ByteArray {
        val signature = ByteArray(Sign.BYTES)
        val signatureLength = LongArray(1)
        ls.cryptoSignDetached(signature, signatureLength, message, message.size.toLong(), keyPair?.secretKey?.asBytes)

        return signature.sliceArray(0 until signatureLength[0].toInt())
    }

    override fun signUsingIdentity(message: String, charset: Charset): ByteArray {
        return signUsingIdentity(message.toByteArray(charset))
    }

    override fun keyExchangeUsingIdentitySecret(exchangePublicKey: ByteArray): ByteArray {
        val curve25519ClientSecretKey = ByteArray(Sign.CURVE25519_SECRETKEYBYTES)
        ls.convertSecretKeyEd25519ToCurve25519(curve25519ClientSecretKey, keyPair?.secretKey?.asBytes)
        return ls.cryptoScalarMult(Key.fromBytes(curve25519ClientSecretKey), Key.fromBytes(exchangePublicKey)).asBytes
    }
}