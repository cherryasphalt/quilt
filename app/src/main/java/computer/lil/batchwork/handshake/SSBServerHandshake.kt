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

class SSBServerHandshake(val serverLongTermKeyPair: KeyPair) {
    enum class State {
        STEP1, STEP2
    }

    var state = State.STEP1
    val sodium = SodiumAndroid()
    val lazySodium = LazySodiumAndroid(sodium, StandardCharsets.UTF_8)

    val serverEphemeralKeyPair = lazySodium.cryptoKxKeypair()
    val networkId = Key.fromHexString("d4a1cb88a66f02f8db635ce26441cc5dac1b08420ceaac230839b755845a9ffb").asBytes
    var clientEphemeralKey: ByteArray? = null
    var sharedSecretab: Key? = null
    var sharedSecretaB: Key? = null
    var sharedSecretAb: Key? = null
    var detachedSignatureA: ByteArray? = null
    var clientLongTermKey: ByteArray? = null

    private fun ByteArray.getLongSize(): Long { return this.size.toLong() }

    fun validateHello(data: ByteArray): Boolean {
        if (data.size != 64)
            return false

        val mac = data.sliceArray(0..31)
        val clientEphemeralKey = data.sliceArray(32..63)

        if (lazySodium.cryptoAuthVerify(mac, clientEphemeralKey, clientEphemeralKey.getLongSize(), networkId)) {
            this.clientEphemeralKey = clientEphemeralKey
            computeSecrets()
            return true
        }
        return false
    }

    fun createHello(): ByteArray {
        val helloMessage = ByteArray(Auth.BYTES)
        lazySodium.cryptoAuth(helloMessage, serverEphemeralKeyPair.publicKey.asBytes, serverEphemeralKeyPair.publicKey.asBytes.getLongSize(), networkId)
        return byteArrayOf(*helloMessage, *serverEphemeralKeyPair.publicKey.asBytes)
    }

    fun computeSecrets() {
        val curve25519ServerSecretKey = ByteArray(Sign.CURVE25519_SECRETKEYBYTES)
        lazySodium.convertSecretKeyEd25519ToCurve25519(curve25519ServerSecretKey, serverLongTermKeyPair.secretKey.asBytes)

        sharedSecretab = lazySodium.cryptoScalarMult(serverEphemeralKeyPair.secretKey, Key.fromBytes(clientEphemeralKey))
        sharedSecretaB = lazySodium.cryptoScalarMult(Key.fromBytes(curve25519ServerSecretKey), Key.fromBytes(clientEphemeralKey))
    }

    fun validateClientAuthentication(data: ByteArray): Boolean {
        val dataPlainText = ByteArray(112)
        val zeroNonce = ByteArray(SecretBox.NONCEBYTES)
        val preKey = byteArrayOf(*networkId, *sharedSecretab!!.asBytes, *sharedSecretaB!!.asBytes)
        val hashKey = ByteArray(Hash.SHA256_BYTES)
        lazySodium.cryptoHashSha256(hashKey, preKey, preKey.getLongSize())

        if(!lazySodium.cryptoSecretBoxOpenEasy(dataPlainText, data, data.getLongSize(), zeroNonce, hashKey))
            return false

        val detachedSignatureA = dataPlainText.sliceArray(0..63)
        val clientLongTermPublicKey = dataPlainText.sliceArray(64..95)
        val hashab = ByteArray(Hash.SHA256_BYTES)
        lazySodium.cryptoHashSha256(hashab, sharedSecretab?.asBytes, sharedSecretab?.asBytes!!.getLongSize())
        val expectedMessage = byteArrayOf(*networkId, *serverLongTermKeyPair.publicKey.asBytes, *hashab)
        if (lazySodium.cryptoSignVerifyDetached(detachedSignatureA, expectedMessage, expectedMessage.getLongSize(), clientLongTermPublicKey)) {
            this.clientLongTermKey = clientLongTermPublicKey
            this.detachedSignatureA = detachedSignatureA

            val curve25519ClientKey = ByteArray(Sign.CURVE25519_PUBLICKEYBYTES)
            lazySodium.convertPublicKeyEd25519ToCurve25519(curve25519ClientKey, clientLongTermKey)
            this.sharedSecretAb = lazySodium.cryptoScalarMult(serverEphemeralKeyPair.secretKey, Key.fromBytes(curve25519ClientKey))
            return true
        }
        return false
    }

    fun createAccept(): ByteArray {
        val detachedSignature = ByteArray(1024)
        val sigLength = LongArray(1)
        val hashab = ByteArray(Hash.SHA256_BYTES)
        lazySodium.cryptoHashSha256(hashab, sharedSecretab?.asBytes, sharedSecretab?.asBytes!!.getLongSize())
        val message = byteArrayOf(*networkId, *detachedSignatureA!!, *clientLongTermKey!!, *hashab)
        lazySodium.cryptoSignDetached(detachedSignature, sigLength, message, message.getLongSize(), serverLongTermKeyPair.secretKey.asBytes)
        val detachedSignatureB = detachedSignature.sliceArray(0..(sigLength[0].toInt() - 1))

        val zeroNonce = ByteArray(SecretBox.NONCEBYTES)
        val preKey = byteArrayOf(*networkId, *sharedSecretab!!.asBytes, *sharedSecretaB!!.asBytes, *sharedSecretAb!!.asBytes)
        val key = ByteArray(Hash.SHA256_BYTES)
        lazySodium.cryptoHashSha256(key, preKey, preKey.getLongSize())

        val payload = ByteArray(SecretBox.MACBYTES + detachedSignatureB.size)
        lazySodium.cryptoSecretBoxEasy(payload, detachedSignatureB, detachedSignatureB.getLongSize(), zeroNonce, key)
        return payload
    }
}
