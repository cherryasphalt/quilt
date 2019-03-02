package computer.lil.batchwork.protocol

import com.goterl.lazycode.lazysodium.interfaces.Hash
import com.goterl.lazycode.lazysodium.interfaces.SecretBox
import com.goterl.lazycode.lazysodium.interfaces.Sign
import com.goterl.lazycode.lazysodium.utils.Key
import computer.lil.batchwork.identity.IdentityHandler

class ServerHandshake(identityHandler: IdentityHandler): Handshake(identityHandler) {
    var detachedSignatureA: ByteArray? = null

    override fun computeSharedKeys() {
        sharedSecretab = ls.cryptoScalarMult(localEphemeralKeyPair.secretKey, Key.fromBytes(remoteEphemeralKey))
        sharedSecretaB = Key.fromBytes(identityHandler.keyExchangeUsingIdentitySecret(remoteEphemeralKey!!))
    }

    fun verifyClientAuthentication(data: ByteArray): Boolean {
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
        val expectedMessage = byteArrayOf(*networkId, *identityHandler.getIdentityPublicKey(), *hashab)
        if (ls.cryptoSignVerifyDetached(detachedSignatureA, expectedMessage, expectedMessage.getLongSize(), clientLongTermPublicKey)) {
            this.remoteKey = clientLongTermPublicKey
            this.detachedSignatureA = detachedSignatureA

            val curve25519ClientKey = ByteArray(Sign.CURVE25519_PUBLICKEYBYTES)
            ls.convertPublicKeyEd25519ToCurve25519(curve25519ClientKey, remoteKey)
            this.sharedSecretAb = ls.cryptoScalarMult(localEphemeralKeyPair.secretKey, Key.fromBytes(curve25519ClientKey))
            return true
        }
        return false
    }

    fun createAcceptMessage(): ByteArray {
        val hashab = ByteArray(Hash.SHA256_BYTES)
        ls.cryptoHashSha256(hashab, sharedSecretab?.asBytes, sharedSecretab?.asBytes!!.getLongSize())
        val message = byteArrayOf(*networkId, *detachedSignatureA!!, *remoteKey!!, *hashab)
        val detachedSignatureB = identityHandler.signUsingIdentity(message)

        val zeroNonce = ByteArray(SecretBox.NONCEBYTES)
        val preKey = byteArrayOf(*networkId, *sharedSecretab!!.asBytes, *sharedSecretaB!!.asBytes, *sharedSecretAb!!.asBytes)
        val key = ByteArray(Hash.SHA256_BYTES)
        ls.cryptoHashSha256(key, preKey, preKey.getLongSize())

        val payload = ByteArray(SecretBox.MACBYTES + detachedSignatureB.size)
        ls.cryptoSecretBoxEasy(payload, detachedSignatureB, detachedSignatureB.getLongSize(), zeroNonce, key)

        completed = true
        return payload
    }
}
