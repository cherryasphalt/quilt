package computer.lil.quilt.identity

import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.Sign
import com.goterl.lazycode.lazysodium.utils.Key
import computer.lil.quilt.BuildConfig
import computer.lil.quilt.model.Identifier
import java.lang.ref.WeakReference
import java.math.BigInteger
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.*
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.security.auth.x500.X500Principal

class AndroidKeyStoreIdentityHandler(context: Context): IdentityHandler {
    companion object {
        private const val SHARED_PREF_NAME = BuildConfig.APPLICATION_ID + ".PREFERENCE"

        private const val PREF_IDENTITY_PUBLIC_KEY = "PREF_IDENTITY_PUBLIC_KEY"
        private const val PREF_IDENTITY_ENCRYPTED_PRIVATE_KEY = "PREF_IDENTITY_ENCRYPTED_PRIVATE_KEY"
        private const val PREF_IDENTITY_ALGORITHM = "PREF_IDENTITY_ALGORITHM"
        private const val PREF_IDENTITY_KEYSTORE_ALGO = "PREF_IDENTITY_KEYSTORE_ALGO"
        private const val PREF_IDENTITY_KEYSTORE_IV = "PREF_IDENTITY_KEYSTORE_IV"

        private const val ALIAS_IDENTITY_KEYSTORE = "ALIAS_IDENTITY_KEYSTORE"
        private const val PROVIDER_KEYSTORE = "AndroidKeyStore"
        private const val RSA_MODE = "RSA/ECB/PKCS1Padding"
        private const val AES_MODE = "AES/GCM/NoPadding"

        fun getInstance(context: Context): AndroidKeyStoreIdentityHandler {
            val handler = AndroidKeyStoreIdentityHandler(context)
            if (!checkIdentityKeyPairExists(context))
                handler.generateIdentityKeyPair()
            return handler
        }

        fun createWithGeneratedKeys(context: Context): AndroidKeyStoreIdentityHandler {
            val handler = AndroidKeyStoreIdentityHandler(context)
            if (handler.generateIdentityKeyPair())
                return handler
            throw IdentityHandler.IdentityException("Failed generating keys.")
        }

        fun checkIdentityKeyPairExists(context: Context): Boolean {
            val pref= context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            return pref.contains(PREF_IDENTITY_PUBLIC_KEY) && pref.contains(PREF_IDENTITY_ENCRYPTED_PRIVATE_KEY)
        }
    }

    private val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)
    private val contextRef = WeakReference<Context>(context)

    /*
        Generates an identity key pair and stores it encrypted in the shared preferences.
        Returns true if successful.
     */
    override fun generateIdentityKeyPair(): Boolean {
        contextRef.get()?.let {context ->
            if (!checkIdentityKeyPairExists(context)) {
                ls.cryptoSignSeedKeypair(SecureRandom().generateSeed(Sign.SEEDBYTES))?.let { keyPair ->
                    val editor = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE).edit()

                    //Encrypt private key
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        createAESKeystoreKey()
                        val cipher = Cipher.getInstance(AES_MODE)
                        cipher.init(Cipher.ENCRYPT_MODE, getAesKey())
                        val encryptedKey = cipher.doFinal(keyPair.secretKey.asBytes)
                        editor.putString(
                            PREF_IDENTITY_ENCRYPTED_PRIVATE_KEY,
                            Base64.encodeToString(encryptedKey, Base64.NO_WRAP)
                        )
                        editor.putString(PREF_IDENTITY_KEYSTORE_ALGO, AES_MODE)
                        editor.putString(
                            PREF_IDENTITY_KEYSTORE_IV,
                            Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
                        )

                    } else {
                        createRSAKeystoreKeys(context)
                        val encryptedKey = rsaEncrypt(keyPair.secretKey.asBytes)
                        editor.putString(
                            PREF_IDENTITY_ENCRYPTED_PRIVATE_KEY,
                            Base64.encodeToString(encryptedKey, Base64.NO_WRAP)
                        )
                        editor.putString(PREF_IDENTITY_KEYSTORE_ALGO, RSA_MODE)
                    }
                    editor.putString(
                        PREF_IDENTITY_PUBLIC_KEY,
                        Base64.encodeToString(keyPair.publicKey.asBytes, Base64.NO_WRAP)
                    )
                    editor.putString(PREF_IDENTITY_ALGORITHM, "ed25519")
                    editor.apply()
                    return true
                }
            }
        }
        return false
    }

    override fun signUsingIdentity(message: String, charset: Charset): ByteArray {
        return signUsingIdentity(message.toByteArray(charset))
    }

    override fun signUsingIdentity(message: ByteArray): ByteArray {
        val signature = ByteArray(Sign.BYTES)
        val signatureLength = LongArray(1)
        ls.cryptoSignDetached(signature, signatureLength, message, message.size.toLong(), decryptPrivateKey())

        return signature.sliceArray(0 until signatureLength[0].toInt())
    }

    override fun getIdentifier(): Identifier {
        contextRef.get()?.run {
            val pref = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            pref.getString(PREF_IDENTITY_PUBLIC_KEY, null)?.let { publicKeyString ->
                return Identifier(publicKeyString, Identifier.AlgoType.ED25519, Identifier.IdentityType.IDENTITY)
            }
        }
        throw IdentityHandler.IdentityException("Identity not found.")
    }

    override fun getIdentityString(): String {
        contextRef.get()?.run {
            val pref = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            pref.getString(PREF_IDENTITY_PUBLIC_KEY, null)?.let { publicKeyString ->
                pref.getString(PREF_IDENTITY_ALGORITHM, null)?.let { algorithmName ->
                    return "@$publicKeyString.$algorithmName"
                }
            }
        }
        throw IdentityHandler.IdentityException("Identity not found.")
    }

    override fun getIdentityPublicKey(): ByteArray {
        contextRef.get()?.run {
            val pref = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            pref.getString(PREF_IDENTITY_PUBLIC_KEY, null)?.let {
                return Base64.decode(it, Base64.NO_WRAP)
            }
        }
        throw IdentityHandler.IdentityException("Identity not found.")
    }

    override fun keyExchangeUsingIdentitySecret(exchangePublicKey: ByteArray): ByteArray {
        val curve25519ClientSecretKey = ByteArray(Sign.CURVE25519_SECRETKEYBYTES)
        ls.convertSecretKeyEd25519ToCurve25519(curve25519ClientSecretKey, decryptPrivateKey())
        return ls.cryptoScalarMult(Key.fromBytes(curve25519ClientSecretKey), Key.fromBytes(exchangePublicKey)).asBytes
    }

    private fun decryptPrivateKey(): ByteArray {
        contextRef.get()?.run {
            val pref = getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)


        pref.getString(PREF_IDENTITY_ENCRYPTED_PRIVATE_KEY, null)?.let { encryptedKey ->
                pref.getString(PREF_IDENTITY_KEYSTORE_ALGO, null)?.let { algo ->
                    return if (algo == RSA_MODE) {
                        rsaDecrypt(Base64.decode(encryptedKey, Base64.NO_WRAP))
                    } else {
                        val iv = Base64.decode(pref.getString(PREF_IDENTITY_KEYSTORE_IV, null), Base64.NO_WRAP)
                        aesDecrypt(Base64.decode(encryptedKey, Base64.NO_WRAP), iv)
                    }
                }
            }
        }
        throw IdentityHandler.IdentityException("Identity not found.")
    }

    private fun getAesKey(): java.security.Key {
        val keyStore = KeyStore.getInstance(PROVIDER_KEYSTORE)
        keyStore.load(null)
        val secretKeyEntry = keyStore.getEntry(ALIAS_IDENTITY_KEYSTORE, null) as KeyStore.SecretKeyEntry
        return secretKeyEntry.secretKey
    }

    private fun getRSAKey(): KeyPair {
        val keyStore = KeyStore.getInstance(PROVIDER_KEYSTORE)
        keyStore.load(null)
        val privateKey = keyStore.getKey(ALIAS_IDENTITY_KEYSTORE, null) as PrivateKey
        val publicKey = keyStore.getCertificate(ALIAS_IDENTITY_KEYSTORE).publicKey;
        return KeyPair(publicKey, privateKey)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun createAESKeystoreKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(ALIAS_IDENTITY_KEYSTORE, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        keyGenerator.generateKey()
    }

    fun createRSAKeystoreKeys(context: Context) {
        val start = GregorianCalendar()
        val end = GregorianCalendar()
        end.add(Calendar.YEAR, 25)

        val kpGenerator = KeyPairGenerator.getInstance("RSA", PROVIDER_KEYSTORE)
        val spec = KeyPairGeneratorSpec.Builder(context)
            .setAlias(ALIAS_IDENTITY_KEYSTORE)
            .setSerialNumber(BigInteger.TEN)
            .setSubject(X500Principal("CN=$ALIAS_IDENTITY_KEYSTORE CA Certificate"))
            .setStartDate(start.time)
            .setEndDate(end.time)
            .build()
        kpGenerator.initialize(spec)
        kpGenerator.generateKeyPair()
    }

    @Throws(Exception::class)
    private fun rsaEncrypt(data: ByteArray): ByteArray {
        val keyPair = getRSAKey()
        // Encrypt the text
        val cipher = Cipher.getInstance(RSA_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.public)
        return cipher.doFinal(data)
    }

    @Throws(Exception::class)
    private fun rsaDecrypt(encrypted: ByteArray): ByteArray {
        val keyStore = KeyStore.getInstance(PROVIDER_KEYSTORE)
        keyStore.load(null)
        val keyPair = getRSAKey()
        val cipher = Cipher.getInstance(RSA_MODE)
        cipher.init(Cipher.DECRYPT_MODE, keyPair.private)
        return cipher.doFinal(encrypted)
    }

    private fun aesDecrypt(encrypted: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.DECRYPT_MODE, getAesKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }
}