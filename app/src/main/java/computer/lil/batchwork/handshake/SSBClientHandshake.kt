package computer.lil.batchwork.handshake

import android.util.Log
import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.Auth
import com.goterl.lazycode.lazysodium.interfaces.Hash
import com.goterl.lazycode.lazysodium.interfaces.SecretBox
import com.goterl.lazycode.lazysodium.interfaces.Sign
import com.goterl.lazycode.lazysodium.utils.Key
import com.goterl.lazycode.lazysodium.utils.KeyPair
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*

class SSBClientHandshake(val longTermKeyPair: KeyPair, val serverLongTermKey: ByteArray) {
    enum class State {
        STEP1, STEP2
    }

    var state = State.STEP1
    val sodium = SodiumAndroid()
    val lazySodium = LazySodiumAndroid(sodium, StandardCharsets.UTF_8)

    val clientEphemeralKeyPair = generateKeys()
    val networkId = Key.fromHexString("d4a1cb88a66f02f8db635ce26441cc5dac1b08420ceaac230839b755845a9ffb").asBytes
    var serverEphemeralKey: ByteArray? = null
    var sharedSecretab: Key? = null
    var sharedSecretaB: Key? = null
    var sharedSecretAb: Key? = null
    var detachedSignatureA: ByteArray? = null

    private fun ByteArray.getLongSize(): Long { return this.size.toLong() }

    private fun generateKeys(): KeyPair {
        return lazySodium.cryptoKxKeypair()
    }

    private fun createHmac(key: ByteArray, text: ByteArray): ByteArray {
        val hmac = ByteArray(Auth.BYTES)
        lazySodium.cryptoAuth(hmac, text, text.getLongSize(), key)

        return hmac
    }

    fun createHello(): ByteArray {
        val hmacMessage = createHmac(networkId, clientEphemeralKeyPair.publicKey.asBytes)
        return ByteBuffer.allocate(hmacMessage.size + clientEphemeralKeyPair.publicKey.asBytes.size)
            .put(hmacMessage)
            .put(clientEphemeralKeyPair.publicKey.asBytes)
            .array()
    }

    fun validateHelloResponse(response: ByteArray): Boolean {
        if (response.size != 64)
            return false

        val mac = response.sliceArray(0..31)
        val serverKey = response.sliceArray(32..63)
        val expectedMac = createHmac(networkId, serverKey)

        if (Arrays.equals(mac, expectedMac)) {
            this.serverEphemeralKey = serverKey
            computeSharedKeys()
            return true
        }
        return false
    }

    fun validateServerAcceptResponse(response: ByteArray): Boolean {
        val zeroNonce = ByteArray(SecretBox.NONCEBYTES)
        val responseKey = ByteArray(Hash.SHA256_BYTES)
        val prekey = ByteBuffer.allocate(networkId.size + Sign.CURVE25519_SECRETKEYBYTES + Sign.CURVE25519_SECRETKEYBYTES)
            .put(networkId)
            .put(sharedSecretab?.asBytes)
            .put(sharedSecretaB?.asBytes)
            .put(sharedSecretAb?.asBytes)
            .array()
        lazySodium.cryptoHashSha256(responseKey, prekey, prekey.getLongSize())

        val hashab = ByteArray(Hash.SHA256_BYTES)
        val messageSize = networkId.size + (detachedSignatureA?.size ?: 0) + longTermKeyPair.publicKey.asBytes.size + hashab.size

        val expectedMessage = ByteBuffer.allocate(messageSize)
            .put(networkId)
            .put(detachedSignatureA)
            .put(longTermKeyPair.publicKey.asBytes)
            .put(hashab)
            .array()

        val detachedSignatureB = ByteArray(messageSize)
        return lazySodium.cryptoSecretBoxOpenEasy(detachedSignatureB, response, response.getLongSize(), zeroNonce, responseKey)
                && lazySodium.cryptoSignVerifyDetached(detachedSignatureB, expectedMessage, expectedMessage.getLongSize(), serverLongTermKey)
    }

    private fun computeSharedKeys() {
        val curve25519ServerKey = ByteArray(Sign.CURVE25519_PUBLICKEYBYTES)
        lazySodium.convertPublicKeyEd25519ToCurve25519(curve25519ServerKey, serverLongTermKey)

        val curve25519ClientPublicKey = ByteArray(Sign.CURVE25519_PUBLICKEYBYTES)
        lazySodium.convertPublicKeyEd25519ToCurve25519(curve25519ClientPublicKey, longTermKeyPair.publicKey.asBytes)

        sharedSecretab = lazySodium.cryptoScalarMult(clientEphemeralKeyPair.secretKey, Key.fromBytes(serverEphemeralKey))
        sharedSecretaB = lazySodium.cryptoScalarMult(clientEphemeralKeyPair.secretKey, Key.fromBytes(curve25519ServerKey))
        Log.d("server key converted", Key.fromBytes(curve25519ServerKey).asHexString)
        sharedSecretAb = lazySodium.cryptoScalarMult(Key.fromBytes(curve25519ClientPublicKey), Key.fromBytes(serverEphemeralKey))
    }

    fun createAuthenticateMessage(): ByteArray {
        val hash = ByteArray(Hash.SHA256_BYTES)
        lazySodium.cryptoHashSha256(hash, sharedSecretab?.asBytes, sharedSecretab?.asBytes?.size!!.toLong())

        val message = ByteBuffer.allocate(networkId.size + serverLongTermKey.size + hash.size)
            .put(networkId)
            .put(serverLongTermKey)
            .put(hash)
            .array()

        val detachedSignature = ByteArray(Sign.BYTES)
        val sigLength = LongArray(1)
        lazySodium.cryptoSignDetached(detachedSignature, sigLength, message, message.getLongSize(), longTermKeyPair.secretKey.asBytes)
        detachedSignatureA = detachedSignature.sliceArray(0..(sigLength[0].toInt() - 1))

        val finalMessage = ByteBuffer.allocate(sigLength[0].toInt() + longTermKeyPair.publicKey.asBytes.size)
            .put(detachedSignatureA)
            .put(longTermKeyPair.publicKey.asBytes)
            .array()

        Log.d("sig length", finalMessage.size.toString())
        Log.d("networkID", lazySodium.toHexStr(networkId))
        Log.d("server short term key", lazySodium.toHexStr(serverEphemeralKey))
        Log.d("server long term key", lazySodium.toHexStr(serverLongTermKey))
        Log.d("sharedSecretab", lazySodium.toHexStr(sharedSecretab?.asBytes))
        Log.d("sharedSecretaB", lazySodium.toHexStr(sharedSecretaB?.asBytes))
        Log.d("sharedSecretAb", lazySodium.toHexStr(sharedSecretAb?.asBytes))
        Log.d("sigA", lazySodium.toHexStr(detachedSignatureA))
        Log.d("client long term", lazySodium.toHexStr(longTermKeyPair.publicKey.asBytes))
        Log.d("shash", lazySodium.toHexStr(hash))
        Log.d("verify", lazySodium.cryptoSignVerifyDetached(detachedSignatureA, message, message.getLongSize(), longTermKeyPair.publicKey.asBytes).toString())

        val zeroNonce = ByteArray(SecretBox.NONCEBYTES)
        val payload = ByteArray(1024)
        val boxKey = ByteArray(Hash.SHA256_BYTES)
        val prekey = ByteBuffer.allocate(networkId.size + Sign.CURVE25519_SECRETKEYBYTES + Sign.CURVE25519_SECRETKEYBYTES)
            .put(networkId)
            .put(sharedSecretab?.asBytes)
            .put(sharedSecretaB?.asBytes)
            .array()

        lazySodium.cryptoHashSha256(boxKey, prekey, prekey.getLongSize())
        lazySodium.cryptoSecretBoxEasy(payload, finalMessage, finalMessage.getLongSize(), zeroNonce, boxKey)

        return payload
    }
}