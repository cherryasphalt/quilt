package computer.lil.quilt.protocol

import com.goterl.lazycode.lazysodium.interfaces.SecretBox
import com.goterl.lazycode.lazysodium.interfaces.Sign
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.protocol.Constants.Companion.SSB_NETWORK_ID
import computer.lil.quilt.protocol.Crypto.Companion.toByteString
import computer.lil.quilt.protocol.Crypto.Companion.toKey
import okio.ByteString

class ServerHandshake(
    identityHandler: IdentityHandler,
    networkId: ByteString = SSB_NETWORK_ID
) : Handshake(identityHandler, networkId) {
    private var detachedSignatureA: ByteString? = null

    override fun computeSharedKeys() {
        sharedSecretab = ls.cryptoScalarMult(localEphemeralKeyPair.secretKey, remoteEphemeralKey?.toKey())
        sharedSecretaB = identityHandler.keyExchangeUsingIdentitySecret(remoteEphemeralKey!!).toKey()
    }

    fun verifyClientAuthentication(data: ByteString): Boolean {
        val zeroNonce = ByteArray(SecretBox.NONCEBYTES).toByteString()
        val hashKey = ByteString.of(*networkId.toByteArray(), *sharedSecretab!!.asBytes, *sharedSecretaB!!.asBytes).sha256()
        val dataPlainText = Crypto.secretBoxOpen(data, hashKey, zeroNonce)

        val detachedSignatureA = dataPlainText?.substring(0, 64)
        val clientLongTermPublicKey = dataPlainText?.substring(64, 96)
        val hashab = sharedSecretab!!.asBytes.toByteString().sha256()
        val expectedMessage = ByteString.of(*networkId.toByteArray(), *identityHandler.getIdentityPublicKey().toByteArray(), *hashab.toByteArray())

        if (Crypto.verifySignDetached(detachedSignatureA!!, expectedMessage, clientLongTermPublicKey!!)) {
            this.remoteKey = clientLongTermPublicKey
            this.detachedSignatureA = detachedSignatureA

            val curve25519ClientKey = ByteArray(Sign.CURVE25519_PUBLICKEYBYTES)
            ls.convertPublicKeyEd25519ToCurve25519(curve25519ClientKey, remoteKey!!.toByteArray())
            this.sharedSecretAb = ls.cryptoScalarMult(localEphemeralKeyPair.secretKey, curve25519ClientKey.toByteString().toKey())
            return true
        }
        return false
    }

    fun createAcceptMessage(): ByteString {
        val hashab = sharedSecretab?.asBytes?.toByteString()?.sha256()
        val message = ByteString.of(*networkId.toByteArray(), *detachedSignatureA!!.toByteArray(), *remoteKey!!.toByteArray(), *hashab!!.toByteArray())
        val detachedSignatureB = identityHandler.signUsingIdentity(message)

        val zeroNonce = ByteArray(SecretBox.NONCEBYTES).toByteString()
        val key = ByteString.of(*networkId.toByteArray(), *sharedSecretab!!.asBytes, *sharedSecretaB!!.asBytes, *sharedSecretAb!!.asBytes).sha256()
        val payload = Crypto.secretBoxSeal(detachedSignatureB, key, zeroNonce)

        completed = true
        return payload
    }
}
