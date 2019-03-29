package computer.lil.quilt.util

import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.Hash
import com.goterl.lazycode.lazysodium.interfaces.Sign
import okio.ByteString
import java.nio.charset.StandardCharsets

class Crypto {
    companion object {
        fun sha256(message: String): ByteArray {
            val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)
            val hash = ByteArray(Hash.SHA256_BYTES)
            val messageByteArray = message.toByteArray()
            ls.cryptoHashSha256(hash, messageByteArray, messageByteArray.size.toLong())
            return hash
        }

        fun derivePublicKey(privateKey: ByteString): ByteString {
            val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)

            val seed = ByteArray(Sign.SEEDBYTES)
            val publicKey = ByteArray(Sign.PUBLICKEYBYTES)
            val newSecretKey = ByteArray(Sign.SECRETKEYBYTES)
            ls.cryptoSignEd25519SkToSeed(seed, privateKey.toByteArray())
            ls.cryptoBoxSeedKeypair(publicKey, newSecretKey, seed)
            return ByteString.of(*publicKey)
        }
    }
}