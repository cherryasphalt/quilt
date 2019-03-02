package computer.lil.batchwork.identity

import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.exceptions.SodiumException
import com.goterl.lazycode.lazysodium.interfaces.Sign
import com.goterl.lazycode.lazysodium.utils.Key
import com.goterl.lazycode.lazysodium.utils.KeyPair
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

class BasicIdentityHandler: IdentityHandler {
    companion object {
        const val KEY_ALGO = "ed25519"
    }
    private val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)
    private var keyPair: KeyPair? = null

    override fun generateIdentityKeyPair(): Boolean {
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

    override fun getIdentityString(): String {
        return "@${getIdentityPublicKey()}.$KEY_ALGO"
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