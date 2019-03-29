package computer.lil.quilt.protocol

import com.goterl.lazycode.lazysodium.interfaces.Hash
import com.goterl.lazycode.lazysodium.interfaces.SecretBox
import com.goterl.lazycode.lazysodium.interfaces.Sign
import com.goterl.lazycode.lazysodium.utils.Key
import computer.lil.quilt.identity.IdentityHandler
import okio.ByteString
import okio.ByteString.Companion.decodeHex

class ServerHandshake(identityHandler: IdentityHandler, networkId: ByteString = "d4a1cb88a66f02f8db635ce26441cc5dac1b08420ceaac230839b755845a9ffb".decodeHex()): Handshake(identityHandler, networkId) {
    var detachedSignatureA: ByteString? = null

    override fun computeSharedKeys() {
        sharedSecretab = ls.cryptoScalarMult(localEphemeralKeyPair.secretKey, Key.fromBytes(remoteEphemeralKey?.toByteArray()))
        sharedSecretaB = Key.fromBytes(identityHandler.keyExchangeUsingIdentitySecret(remoteEphemeralKey!!).toByteArray())
    }

    fun verifyClientAuthentication(data: ByteString): Boolean {
        val dataPlainText = ByteArray(96)
        val zeroNonce = ByteArray(SecretBox.NONCEBYTES)
        val hashKey = ByteString.of(*networkId.toByteArray(), *sharedSecretab!!.asBytes, *sharedSecretaB!!.asBytes).sha256()
        ls.cryptoSecretBoxOpenEasy(dataPlainText, data.toByteArray(), data.size.toLong(), zeroNonce, hashKey.toByteArray())

        val dataPlainTextString = ByteString.of(*dataPlainText)
        val detachedSignatureA = dataPlainTextString.substring(0, 64)
        val clientLongTermPublicKey = dataPlainTextString.substring(64, 96)
        val hashab = ByteString.of(*sharedSecretab!!.asBytes).sha256()
        val expectedMessage = ByteString.of(*networkId.toByteArray(), *identityHandler.getIdentityPublicKey().toByteArray(), *hashab.toByteArray())
        if (ls.cryptoSignVerifyDetached(detachedSignatureA.toByteArray(), expectedMessage.toByteArray(), expectedMessage.size.toLong(), clientLongTermPublicKey.toByteArray())) {
            this.remoteKey = clientLongTermPublicKey
            this.detachedSignatureA = detachedSignatureA

            val curve25519ClientKey = ByteArray(Sign.CURVE25519_PUBLICKEYBYTES)
            ls.convertPublicKeyEd25519ToCurve25519(curve25519ClientKey, remoteKey!!.toByteArray())
            this.sharedSecretAb = ls.cryptoScalarMult(localEphemeralKeyPair.secretKey, Key.fromBytes(curve25519ClientKey))
            return true
        }
        return false
    }

    fun createAcceptMessage(): ByteString {
        val hashab = ByteArray(Hash.SHA256_BYTES)
        ls.cryptoHashSha256(hashab, sharedSecretab?.asBytes, sharedSecretab?.asBytes!!.getLongSize())
        val message = ByteString.of(*networkId.toByteArray(), *detachedSignatureA!!.toByteArray(), *remoteKey!!.toByteArray(), *hashab)
        val detachedSignatureB = identityHandler.signUsingIdentity(message)

        val zeroNonce = ByteArray(SecretBox.NONCEBYTES)
        val key = ByteString.of(*networkId.toByteArray(), *sharedSecretab!!.asBytes, *sharedSecretaB!!.asBytes, *sharedSecretAb!!.asBytes).sha256()

        val payload = ByteArray(SecretBox.MACBYTES + detachedSignatureB.size)
        ls.cryptoSecretBoxEasy(payload, detachedSignatureB.toByteArray(), detachedSignatureB.size.toLong(), zeroNonce, key.toByteArray())

        completed = true
        return ByteString.of(*payload)
    }
}
