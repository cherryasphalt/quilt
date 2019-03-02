package computer.lil.batchwork.protocol

import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.Auth
import com.goterl.lazycode.lazysodium.interfaces.Hash
import com.goterl.lazycode.lazysodium.interfaces.SecretBox
import com.goterl.lazycode.lazysodium.utils.Key
import com.goterl.lazycode.lazysodium.utils.KeyPair
import computer.lil.batchwork.identity.IdentityHandler
import java.nio.charset.StandardCharsets
import java.util.*

abstract class Handshake(
        val identityHandler: IdentityHandler,
        val networkId: ByteArray = Key.fromHexString("d4a1cb88a66f02f8db635ce26441cc5dac1b08420ceaac230839b755845a9ffb").asBytes
    ) {
    enum class State {
        PHASE1, PHASE2, PHASE3, PHASE4, ERROR
    }

    var state = State.PHASE1

    protected val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)

    var completed = false
    var remoteKey: ByteArray? = null
    val localEphemeralKeyPair: KeyPair = ls.cryptoKxKeypair()
    var remoteEphemeralKey: ByteArray? = null

    protected var sharedSecretab: Key? = null
    protected var sharedSecretaB: Key? = null
    protected var sharedSecretAb: Key? = null

    protected fun ByteArray.getLongSize(): Long { return this.size.toLong() }

    private fun createHmac(key: ByteArray, message: ByteArray): ByteArray {
        val hmac = ByteArray(Auth.BYTES)
        ls.cryptoAuth(hmac, message, message.getLongSize(), key)
        return hmac
    }

    private fun hash256(message: ByteArray): ByteArray {
        val hash = ByteArray(Hash.SHA256_BYTES)
        ls.cryptoHashSha256(hash, message, message.getLongSize())
        return hash
    }


    fun createHelloMessage(): ByteArray {
        val hmacMessage = createHmac(networkId, localEphemeralKeyPair.publicKey.asBytes)
        return byteArrayOf(*hmacMessage, *localEphemeralKeyPair.publicKey.asBytes)
    }

    fun verifyHelloMessage(data: ByteArray): Boolean {
        if (data.size != 64)
            return false

        val mac = data.sliceArray(0..31)
        val remoteEphemeralKey = data.sliceArray(32..63)
        val expectedMac = createHmac(networkId, remoteEphemeralKey)

        if (Arrays.equals(mac, expectedMac)) {
            this.remoteEphemeralKey = remoteEphemeralKey
            computeSharedKeys()
            return true
        }
        return false
    }

    fun createBoxStream(): BoxStream {
        val localToRemoteKey =
            hash256(
                byteArrayOf(
                    *hash256(
                        hash256(
                            byteArrayOf(*networkId, *sharedSecretab!!.asBytes, *sharedSecretaB!!.asBytes, *sharedSecretAb!!.asBytes)
                        )
                    ),
                    *remoteKey!!
                )
            )

        val remoteToLocalKey =
            hash256(
                byteArrayOf(
                    *hash256(
                        hash256(
                            byteArrayOf(*networkId, *sharedSecretab!!.asBytes, *sharedSecretaB!!.asBytes, *sharedSecretAb!!.asBytes)
                        )
                    ),
                    *identityHandler.getIdentityPublicKey()
                )
            )

        val localToRemoteNonce = remoteEphemeralKey!!.sliceArray(0 until SecretBox.NONCEBYTES)
        val remoteToLocalNonce = localEphemeralKeyPair.publicKey.asBytes.sliceArray(0 until SecretBox.NONCEBYTES)

        return BoxStream(localToRemoteKey, remoteToLocalKey, localToRemoteNonce, remoteToLocalNonce)
    }

    protected abstract fun computeSharedKeys()
}