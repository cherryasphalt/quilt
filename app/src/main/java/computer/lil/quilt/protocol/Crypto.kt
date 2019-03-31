package computer.lil.quilt.protocol

import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.Auth
import com.goterl.lazycode.lazysodium.interfaces.SecretBox
import com.goterl.lazycode.lazysodium.interfaces.Sign
import com.goterl.lazycode.lazysodium.utils.Key
import okio.Buffer
import okio.ByteString
import java.nio.charset.StandardCharsets
import java.util.concurrent.ThreadFactory

class Crypto {
    companion object {
        fun derivePublicKey(privateKey: ByteString): ByteString {
            val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)

            val seed = ByteArray(Sign.SEEDBYTES)
            val publicKey = ByteArray(Sign.PUBLICKEYBYTES)
            val newSecretKey = ByteArray(Sign.SECRETKEYBYTES)
            ls.cryptoSignEd25519SkToSeed(seed, privateKey.toByteArray())
            ls.cryptoBoxSeedKeypair(publicKey, newSecretKey, seed)
            return publicKey.toByteString()
        }

        fun ByteArray.increment(): ByteArray {
            for (i in size - 1 downTo 0) {
                if (this[i] == 0xFF.toByte()) {
                    this[i] = 0x00.toByte()
                } else {
                    ++this[i]
                    break
                }
            }
            return this
        }

        fun ByteString.increment(): ByteString {
            return ByteString.of(*this.toByteArray().increment())
        }

        fun Buffer.increment(): Buffer {
            val incremented = this.snapshot().increment()
            this.skip(this.size)
            this.write(incremented)
            return this
        }

        fun ByteArray.toByteString(): ByteString {
            return ByteString.of(*this)
        }

        fun ByteString.toKey(): Key {
            return Key.fromBytes(this.toByteArray())
        }

        fun secretBoxSeal(message: ByteString, key: ByteString, nonce: ByteString): ByteString {
            val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)

            val encrypted = ByteArray(message.size + SecretBox.MACBYTES)
            ls.cryptoSecretBoxEasy(encrypted, message.toByteArray(), message.size.toLong(), nonce.toByteArray(), key.toByteArray())
            return encrypted.toByteString()
        }

        fun secretBoxOpen(encrypted: ByteString, key: ByteString, nonce: ByteString): ByteString? {
            val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)

            val decrypted = ByteArray(encrypted.size - SecretBox.MACBYTES)
            val valid = ls.cryptoSecretBoxOpenEasy(
                decrypted,
                encrypted.toByteArray(),
                encrypted.size.toLong(),
                nonce.toByteArray(),
                key.toByteArray()
            )

            return if (valid) decrypted.toByteString() else null
        }

        fun verifySignDetached(signature: ByteString, message: ByteString, key: ByteString): Boolean {
            val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)

            return ls.cryptoSignVerifyDetached(signature.toByteArray(), message.toByteArray(), message.size.toLong(), key.toByteArray())
        }

        fun createHmac(key: ByteString, message: ByteString): ByteString {
            val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)

            val hmac = ByteArray(Auth.BYTES)
            ls.cryptoAuth(hmac, message.toByteArray(), message.size.toLong(), key.toByteArray())
            return hmac.toByteString()
        }
    }
}

class Util {
    companion object {
        @JvmStatic
        fun threadFactory(name: String, daemon: Boolean): ThreadFactory {
            return ThreadFactory { runnable ->
                val result = Thread(runnable, name)
                result.isDaemon = daemon
                result
            }
        }
    }
}