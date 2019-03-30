package computer.lil.quilt.protocol

import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.SecretBox
import com.goterl.lazycode.lazysodium.utils.Key
import com.goterl.lazycode.lazysodium.utils.KeyPair
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.protocol.Crypto.Companion.createHmac
import computer.lil.quilt.protocol.Crypto.Companion.toByteString
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import java.nio.charset.StandardCharsets

abstract class Handshake(
        val identityHandler: IdentityHandler,
        val networkId: ByteString = "d4a1cb88a66f02f8db635ce26441cc5dac1b08420ceaac230839b755845a9ffb".decodeHex()
    ) {
    enum class State {
        PHASE1, PHASE2, PHASE3, PHASE4, ERROR
    }

    var state = State.PHASE1

    protected val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)

    var completed = false
    var remoteKey: ByteString? = null
    val localEphemeralKeyPair: KeyPair = ls.cryptoKxKeypair()
    var remoteEphemeralKey: ByteString? = null

    protected var sharedSecretab: Key? = null
    protected var sharedSecretaB: Key? = null
    protected var sharedSecretAb: Key? = null

    protected fun ByteArray.getLongSize(): Long { return this.size.toLong() }

    fun createHelloMessage(): ByteString {
        val localEphemeralKeyPairString = ByteString.of(*localEphemeralKeyPair.publicKey.asBytes)
        val hmacMessage = createHmac(networkId, localEphemeralKeyPairString)
        return ByteString.of(*hmacMessage.toByteArray(), *localEphemeralKeyPairString.toByteArray())
    }

    fun verifyHelloMessage(data: ByteString): Boolean {
        if (data.size != 64)
            return false

        val mac = data.substring(0, 32)
        val remoteEphemeralKey = data.substring(32, 64)
        val expectedMac = createHmac(networkId, remoteEphemeralKey)

        if (mac == expectedMac) {
            this.remoteEphemeralKey = remoteEphemeralKey
            computeSharedKeys()
            return true
        }
        return false
    }

    fun createBoxStream(): BoxStream {
        val localToRemoteKey =
            ByteString.of(
                *ByteString.of(*networkId.toByteArray(), *sharedSecretab!!.asBytes, *sharedSecretaB!!.asBytes, *sharedSecretAb!!.asBytes).sha256().sha256().toByteArray(),
                *remoteKey!!.toByteArray()
            ).sha256()

        val remoteToLocalKey =
            ByteString.of(
                *ByteString.of(*networkId.toByteArray(), *sharedSecretab!!.asBytes, *sharedSecretaB!!.asBytes, *sharedSecretAb!!.asBytes).sha256().sha256().toByteArray(),
                *identityHandler.getIdentityPublicKey().toByteArray()
            ).sha256()

        val localToRemoteNonce = createHmac(networkId, remoteEphemeralKey!!).substring(0, SecretBox.NONCEBYTES)
        val remoteToLocalNonce = createHmac(networkId, localEphemeralKeyPair.publicKey.asBytes.toByteString()).substring(0, SecretBox.NONCEBYTES)

        return BoxStream(localToRemoteKey, remoteToLocalKey, Buffer().write(localToRemoteNonce), Buffer().write(remoteToLocalNonce))
    }

    protected abstract fun computeSharedKeys()
}