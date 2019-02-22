package computer.lil.batchwork.handshake

import com.codahale.xsalsa20poly1305.Keys
import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.KeyParameter
import java.nio.ByteBuffer

public class SSBClientHandshake() {
    private fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }

    val ephemeralKey = generateEphemeralKey()
    val networkId = byteArrayOfInts(0xd4, 0xa1, 0xcb, 0x88, 0xa6, 0x6f, 0x02, 0xf8, 0xdb, 0x63, 0x5c, 0xe2, 0x64,
        0x41, 0xcc, 0x5d, 0xac, 0x1b, 0x08, 0x42, 0x0c, 0xea, 0xac, 0x23, 0x08, 0x39, 0xb7, 0x55, 0x84, 0x5a, 0x9f, 0xfb)

    private fun createHmac(key: ByteArray, text: ByteArray): ByteArray {
        val hmac = HMac(SHA512Digest())
        hmac.init(KeyParameter(key))
        val result = ByteArray(hmac.macSize)

        hmac.update(text, 0, text.size)
        hmac.doFinal(result, 0)
        return result.sliceArray(0..31)
    }

    private fun generateEphemeralKey(): ByteArray {
        val privateKey = Keys.generatePrivateKey()
        val publicKey = Keys.generatePublicKey(privateKey)

        return publicKey
    }

    public fun createHello(): ByteArray? {
        val ephemeralKey = generateEphemeralKey()
        val hmacMessage = createHmac(networkId, ephemeralKey)
        return ByteBuffer.allocate(hmacMessage.size + ephemeralKey.size)
            .put(hmacMessage)
            .put(ephemeralKey)
            .array()
    }
}