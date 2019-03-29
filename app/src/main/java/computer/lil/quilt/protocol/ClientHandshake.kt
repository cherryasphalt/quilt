package computer.lil.quilt.protocol

import com.goterl.lazycode.lazysodium.interfaces.Hash
import com.goterl.lazycode.lazysodium.interfaces.SecretBox
import com.goterl.lazycode.lazysodium.interfaces.Sign
import com.goterl.lazycode.lazysodium.utils.Key
import computer.lil.quilt.identity.IdentityHandler
import okio.ByteString
import okio.ByteString.Companion.decodeHex

class ClientHandshake(identityHandler: IdentityHandler, serverKey: ByteString, networkId: ByteString = "d4a1cb88a66f02f8db635ce26441cc5dac1b08420ceaac230839b755845a9ffb".decodeHex()): Handshake(identityHandler, networkId) {
    init {
        remoteKey = serverKey
    }

    private var detachedSignatureA: ByteString? = null

    fun createAuthenticateMessage(): ByteString {
        val hash = ByteString.of(*sharedSecretab!!.asBytes).sha256()
        val message = ByteString.of(*networkId.toByteArray(), *remoteKey!!.toByteArray(), *hash.toByteArray())
        detachedSignatureA = identityHandler.signUsingIdentity(message)

        val finalMessage = ByteString.of(*detachedSignatureA!!.toByteArray(), *identityHandler.getIdentityPublicKey().toByteArray())
        val zeroNonce = ByteArray(SecretBox.NONCEBYTES)
        val payload = ByteArray(112)
        val boxKey = ByteString.of(*networkId.toByteArray(), *sharedSecretab!!.asBytes, *sharedSecretaB!!.asBytes).sha256()

        ls.cryptoSecretBoxEasy(payload, finalMessage.toByteArray(), finalMessage.size.toLong(), zeroNonce, boxKey.toByteArray())

        return ByteString.of(*payload)
    }

    fun verifyServerAcceptResponse(data: ByteString): Boolean {
        val zeroNonce = ByteArray(SecretBox.NONCEBYTES)
        val responseKey = ByteString.of(*networkId.toByteArray(), *sharedSecretab!!.asBytes, *sharedSecretaB!!.asBytes, *sharedSecretAb!!.asBytes).sha256()
        val hashab = ByteString.of(*sharedSecretab!!.asBytes).sha256()

        val messageSize = networkId.size + (detachedSignatureA?.size ?: 0) + identityHandler.getIdentityPublicKey().size + hashab.size
        val expectedMessage = ByteString.of(*networkId.toByteArray(), *detachedSignatureA!!.toByteArray(), *identityHandler.getIdentityPublicKey().toByteArray(), *hashab.toByteArray())
        val detachedSignatureB = ByteArray(messageSize - SecretBox.MACBYTES)

        completed = ls.cryptoSecretBoxOpenEasy(detachedSignatureB, data.toByteArray(), data.size.toLong(), zeroNonce, responseKey.toByteArray())
                && ls.cryptoSignVerifyDetached(detachedSignatureB, expectedMessage.toByteArray(), expectedMessage.size.toLong(), remoteKey!!.toByteArray())
        return completed
    }

    override fun computeSharedKeys() {
        val curve25519ServerKey = ByteArray(Sign.CURVE25519_PUBLICKEYBYTES)
        ls.convertPublicKeyEd25519ToCurve25519(curve25519ServerKey, remoteKey!!.toByteArray())

        sharedSecretab = ls.cryptoScalarMult(localEphemeralKeyPair.secretKey, Key.fromBytes(remoteEphemeralKey?.toByteArray()))
        sharedSecretaB = ls.cryptoScalarMult(localEphemeralKeyPair.secretKey, Key.fromBytes(curve25519ServerKey))
        sharedSecretAb = Key.fromBytes(identityHandler.keyExchangeUsingIdentitySecret(ByteString.of(*remoteEphemeralKey!!.toByteArray())).toByteArray())
    }
}