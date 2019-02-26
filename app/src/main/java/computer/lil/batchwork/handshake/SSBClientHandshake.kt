package computer.lil.batchwork.handshake

import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.Auth
import com.goterl.lazycode.lazysodium.interfaces.Hash
import com.goterl.lazycode.lazysodium.interfaces.SecretBox
import com.goterl.lazycode.lazysodium.interfaces.Sign
import com.goterl.lazycode.lazysodium.utils.Key
import com.goterl.lazycode.lazysodium.utils.KeyPair
import java.nio.charset.StandardCharsets
import java.util.*

class SSBClientHandshake(val clientLongTermKeyPair: KeyPair, val serverLongTermKey: ByteArray) {
    enum class State {
        STEP1, STEP2, STEP3
    }

    var state = State.STEP1
    val sodium = SodiumAndroid()
    val lazySodium = LazySodiumAndroid(sodium, StandardCharsets.UTF_8)

    val clientEphemeralKeyPair = lazySodium.cryptoKxKeypair()
    val networkId = Key.fromHexString("d4a1cb88a66f02f8db635ce26441cc5dac1b08420ceaac230839b755845a9ffb").asBytes
    var serverEphemeralKey: ByteArray? = null
    var sharedSecretab: Key? = null
    var sharedSecretaB: Key? = null
    var sharedSecretAb: Key? = null
    var detachedSignatureA: ByteArray? = null

    private fun ByteArray.getLongSize(): Long { return this.size.toLong() }

    private fun createHmac(key: ByteArray, text: ByteArray): ByteArray {
        val hmac = ByteArray(Auth.BYTES)
        lazySodium.cryptoAuth(hmac, text, text.getLongSize(), key)

        return hmac
    }

    fun createHello(): ByteArray {
        val hmacMessage = createHmac(networkId, clientEphemeralKeyPair.publicKey.asBytes)
        return byteArrayOf(*hmacMessage, *clientEphemeralKeyPair.publicKey.asBytes)
    }

    fun validateHelloResponse(data: ByteArray): Boolean {
        if (data.size != 64)
            return false

        val mac = data.sliceArray(0..31)
        val serverKey = data.sliceArray(32..63)
        val expectedMac = createHmac(networkId, serverKey)

        if (Arrays.equals(mac, expectedMac)) {
            this.serverEphemeralKey = serverKey
            computeSharedKeys()
            return true
        }
        return false
    }

    private fun computeSharedKeys() {
        val curve25519ServerKey = ByteArray(Sign.CURVE25519_PUBLICKEYBYTES)
        lazySodium.convertPublicKeyEd25519ToCurve25519(curve25519ServerKey, serverLongTermKey)

        val curve25519ClientSecretKey = ByteArray(Sign.CURVE25519_SECRETKEYBYTES)
        lazySodium.convertSecretKeyEd25519ToCurve25519(curve25519ClientSecretKey, clientLongTermKeyPair.secretKey.asBytes)

        sharedSecretab = lazySodium.cryptoScalarMult(clientEphemeralKeyPair.secretKey, Key.fromBytes(serverEphemeralKey))
        sharedSecretaB = lazySodium.cryptoScalarMult(clientEphemeralKeyPair.secretKey, Key.fromBytes(curve25519ServerKey))
        sharedSecretAb = lazySodium.cryptoScalarMult(Key.fromBytes(curve25519ClientSecretKey), Key.fromBytes(serverEphemeralKey))
    }

    fun createAuthenticateMessage(): ByteArray {
        val hash = ByteArray(Hash.SHA256_BYTES)
        lazySodium.cryptoHashSha256(hash, sharedSecretab?.asBytes, sharedSecretab?.asBytes?.size!!.toLong())

        val message = byteArrayOf(*networkId, *serverLongTermKey, *hash)
        val detachedSignature = ByteArray(Sign.BYTES)
        val sigLength = LongArray(1)
        lazySodium.cryptoSignDetached(detachedSignature, sigLength, message, message.getLongSize(), clientLongTermKeyPair.secretKey.asBytes)
        detachedSignatureA = detachedSignature.sliceArray(0..(sigLength[0].toInt() - 1))

        val finalMessage = byteArrayOf(*detachedSignatureA!!, *clientLongTermKeyPair.publicKey.asBytes)
        val zeroNonce = ByteArray(SecretBox.NONCEBYTES)
        val payload = ByteArray(112)
        //val payload = ByteArray(finalMessage.size - SecretBox.MACBYTES)
        val boxKey = ByteArray(Hash.SHA256_BYTES)
        val preKey = byteArrayOf(*networkId, *sharedSecretab!!.asBytes, *sharedSecretaB!!.asBytes)

        lazySodium.cryptoHashSha256(boxKey, preKey, preKey.getLongSize())
        lazySodium.cryptoSecretBoxEasy(payload, finalMessage, finalMessage.getLongSize(), zeroNonce, boxKey)

        return payload
    }

    fun validateServerAcceptResponse(data: ByteArray): Boolean {
        val zeroNonce = ByteArray(SecretBox.NONCEBYTES)
        val responseKey = ByteArray(Hash.SHA256_BYTES)
        val preKey = byteArrayOf(*networkId, *sharedSecretab!!.asBytes, *sharedSecretaB!!.asBytes, *sharedSecretAb!!.asBytes)
        lazySodium.cryptoHashSha256(responseKey, preKey, preKey.getLongSize())
        val hashab = ByteArray(Hash.SHA256_BYTES)
        lazySodium.cryptoHashSha256(hashab, sharedSecretab?.asBytes, sharedSecretab?.asBytes?.size!!.toLong())

        val messageSize = networkId.size + (detachedSignatureA?.size ?: 0) + clientLongTermKeyPair.publicKey.asBytes.size + hashab.size
        val expectedMessage = byteArrayOf(*networkId, *detachedSignatureA!!, *clientLongTermKeyPair.publicKey.asBytes, *hashab)
        val detachedSignatureB = ByteArray(messageSize - SecretBox.MACBYTES)

        return lazySodium.cryptoSecretBoxOpenEasy(detachedSignatureB, data, data.getLongSize(), zeroNonce, responseKey)
                && lazySodium.cryptoSignVerifyDetached(detachedSignatureB, expectedMessage, expectedMessage.getLongSize(), serverLongTermKey)
    }
}