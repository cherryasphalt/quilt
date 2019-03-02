package computer.lil.batchwork.network

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
    val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)

    val serverEphemeralKeyPair: KeyPair = ls.cryptoKxKeypair()
    val networkId: ByteArray = Key.fromHexString("5fb17ceeadd1589110a5c9a1d682ad2680e76d93c37e9cdbff7f22f8c829d032").asBytes
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

        if (ls.cryptoAuthVerify(mac, clientEphemeralKey, clientEphemeralKey.getLongSize(), networkId)) {
            this.clientEphemeralKey = clientEphemeralKey
            computeSharedKeys()
            return true
        }
        return false
    }

    fun createHello(): ByteArray {
        val helloMessage = ByteArray(Auth.BYTES)
        ls.cryptoAuth(helloMessage, serverEphemeralKeyPair.publicKey.asBytes, serverEphemeralKeyPair.publicKey.asBytes.getLongSize(), networkId)
        return byteArrayOf(*helloMessage, *serverEphemeralKeyPair.publicKey.asBytes)
    }

    private fun computeSharedKeys() {
        val curve25519ServerSecretKey = ByteArray(Sign.CURVE25519_SECRETKEYBYTES)
        ls.convertSecretKeyEd25519ToCurve25519(curve25519ServerSecretKey, serverLongTermKeyPair.secretKey.asBytes)

        sharedSecretab = ls.cryptoScalarMult(serverEphemeralKeyPair.secretKey, Key.fromBytes(clientEphemeralKey))
        sharedSecretaB = ls.cryptoScalarMult(Key.fromBytes(curve25519ServerSecretKey), Key.fromBytes(clientEphemeralKey))
    }

    fun validateClientAuthentication(data: ByteArray): Boolean {
        val dataPlainText = ByteArray(96)
        val zeroNonce = ByteArray(SecretBox.NONCEBYTES)
        val preKey = byteArrayOf(*networkId, *sharedSecretab!!.asBytes, *sharedSecretaB!!.asBytes)
        val hashKey = ByteArray(Hash.SHA256_BYTES)
        ls.cryptoHashSha256(hashKey, preKey, preKey.getLongSize())
        ls.cryptoSecretBoxOpenEasy(dataPlainText, data, data.getLongSize(), zeroNonce, hashKey)

        val detachedSignatureA = dataPlainText.sliceArray(0..63)
        val clientLongTermPublicKey = dataPlainText.sliceArray(64..95)
        val hashab = ByteArray(Hash.SHA256_BYTES)
        ls.cryptoHashSha256(hashab, sharedSecretab?.asBytes, sharedSecretab?.asBytes!!.getLongSize())
        val expectedMessage = byteArrayOf(*networkId, *serverLongTermKeyPair.publicKey.asBytes, *hashab)
        if (ls.cryptoSignVerifyDetached(detachedSignatureA, expectedMessage, expectedMessage.getLongSize(), clientLongTermPublicKey)) {
            this.clientLongTermKey = clientLongTermPublicKey
            this.detachedSignatureA = detachedSignatureA

            val curve25519ClientKey = ByteArray(Sign.CURVE25519_PUBLICKEYBYTES)
            ls.convertPublicKeyEd25519ToCurve25519(curve25519ClientKey, clientLongTermKey)
            this.sharedSecretAb = ls.cryptoScalarMult(serverEphemeralKeyPair.secretKey, Key.fromBytes(curve25519ClientKey))
            return true
        }
        return false
    }

    fun createAccept(): ByteArray {
        val detachedSignature = ByteArray(1024)
        val sigLength = LongArray(1)
        val hashab = ByteArray(Hash.SHA256_BYTES)
        ls.cryptoHashSha256(hashab, sharedSecretab?.asBytes, sharedSecretab?.asBytes!!.getLongSize())
        val message = byteArrayOf(*networkId, *detachedSignatureA!!, *clientLongTermKey!!, *hashab)
        ls.cryptoSignDetached(detachedSignature, sigLength, message, message.getLongSize(), serverLongTermKeyPair.secretKey.asBytes)
        val detachedSignatureB = detachedSignature.sliceArray(0..(sigLength[0].toInt() - 1))

        val zeroNonce = ByteArray(SecretBox.NONCEBYTES)
        val preKey = byteArrayOf(*networkId, *sharedSecretab!!.asBytes, *sharedSecretaB!!.asBytes, *sharedSecretAb!!.asBytes)
        val key = ByteArray(Hash.SHA256_BYTES)
        ls.cryptoHashSha256(key, preKey, preKey.getLongSize())

        val payload = ByteArray(SecretBox.MACBYTES + detachedSignatureB.size)
        ls.cryptoSecretBoxEasy(payload, detachedSignatureB, detachedSignatureB.getLongSize(), zeroNonce, key)
        return payload
    }
}
