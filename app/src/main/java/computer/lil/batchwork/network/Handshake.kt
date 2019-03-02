package computer.lil.batchwork.network

import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.Auth
import com.goterl.lazycode.lazysodium.utils.Key
import com.goterl.lazycode.lazysodium.utils.KeyPair
import computer.lil.batchwork.identity.IdentityHandler
import java.nio.charset.StandardCharsets
import java.util.*

abstract class Handshake(val identityHandler: IdentityHandler) {
    protected val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)

    val networkId = Key.fromHexString("d4a1cb88a66f02f8db635ce26441cc5dac1b08420ceaac230839b755845a9ffb").asBytes
    val localEphemeralKeyPair: KeyPair = ls.cryptoKxKeypair()
    var remoteEphemeralKey: ByteArray? = null
    protected var sharedSecretab: Key? = null
    protected var sharedSecretaB: Key? = null
    protected var sharedSecretAb: Key? = null

    protected fun ByteArray.getLongSize(): Long { return this.size.toLong() }

    private fun createHmac(key: ByteArray, text: ByteArray): ByteArray {
        val hmac = ByteArray(Auth.BYTES)
        ls.cryptoAuth(hmac, text, text.getLongSize(), key)

        return hmac
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

    protected abstract fun computeSharedKeys()
}