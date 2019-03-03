package computer.lil.quilt.identity

import android.content.Context
import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.Sign
import computer.lil.quilt.BuildConfig
import java.lang.Exception
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import android.security.keystore.KeyProperties
import android.security.keystore.KeyGenParameterSpec
import android.os.Build
import java.security.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import android.util.Base64
import androidx.annotation.RequiresApi
import com.goterl.lazycode.lazysodium.utils.Key
import java.lang.ref.WeakReference
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

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

        private val FIXED_IV = ByteArray(12)

        fun createWithGeneratedKeys(context: Context): AndroidKeyStoreIdentityHandler {
            val handler = AndroidKeyStoreIdentityHandler(context)
            if (handler.generateIdentityKeyPair())
                return handler
            throw IdentityHandler.IdentityException("Failed generating keys.")
        }

        fun checkIdentityKeyPairExists(context: Context): Boolean {
            val pref= context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            return pref.contains(PREF_IDENTITY_PUBLIC_KEY) && pref.contains(
                PREF_IDENTITY_ENCRYPTED_PRIVATE_KEY
            )
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
                    createKeystoreKeys(context)
                    val editor = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE).edit()
                    editor.putString(
                        PREF_IDENTITY_PUBLIC_KEY,
                        Base64.encodeToString(keyPair.publicKey.asBytes, Base64.NO_WRAP)
                    )
                    editor.putString(PREF_IDENTITY_ALGORITHM, "ed25519")

                    //Encrypt private key
                    /*rsaEncrypt(keyPair.secretKey.asBytes).let {
                    editor.putString(PREF_IDENTITY_ENCRYPTED_PRIVATE_KEY, Base64.encodeToString(it, Base64.DEFAULT))
                }*/
                    aesEncrypt(keyPair.secretKey.asBytes).let {
                        editor.putString(PREF_IDENTITY_ENCRYPTED_PRIVATE_KEY, Base64.encodeToString(it, Base64.NO_WRAP))
                        editor.putString(PREF_IDENTITY_KEYSTORE_ALGO, AES_MODE)
                    }

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
                //return rsaDecrypt(Base64.decode(encryptedKey, Base64.DEFAULT))
                return aesDecrypt(Base64.decode(encryptedKey, Base64.NO_WRAP))
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

    @RequiresApi(Build.VERSION_CODES.M)
    @Throws(
        NoSuchProviderException::class,
        NoSuchAlgorithmException::class,
        InvalidAlgorithmParameterException::class
    )
    private fun createKeystoreKeys(context: Context) {
        /*val start = GregorianCalendar()
        val end = GregorianCalendar()
        end.add(Calendar.YEAR, 5)

        val kpGenerator = KeyPairGenerator.getInstance("RSA", PROVIDER_KEYSTORE)
        val spec: AlgorithmParameterSpec

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            spec = KeyPairGeneratorSpec.Builder(context)
                .setAlias(ALIAS_IDENTITY_KEYSTORE)
                .setSubject(X500Principal("CN=$ALIAS_IDENTITY_KEYSTORE"))
                .setSerialNumber(BigInteger.valueOf(3456789765432141))
                .setStartDate(start.getTime())
                .setEndDate(end.getTime())
                .build()
        } else {
            spec = KeyGenParameterSpec.Builder(ALIAS_IDENTITY_KEYSTORE,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(false)
                .build()
        }

        kpGenerator.initialize(spec)
        kpGenerator.generateKeyPair()*/


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER_KEYSTORE)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(ALIAS_IDENTITY_KEYSTORE, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(false)
                .build()
            )
            keyGenerator.generateKey()
        } else {

        }

    }

    @Throws(Exception::class)
    private fun rsaEncrypt(secret: ByteArray): ByteArray {
        val keyStore = KeyStore.getInstance(PROVIDER_KEYSTORE)
        keyStore.load(null)
        val privateKey = keyStore.getKey(ALIAS_IDENTITY_KEYSTORE, null) as PrivateKey
        val publicKey = keyStore.getCertificate(ALIAS_IDENTITY_KEYSTORE).publicKey;
        // Encrypt the text

        val inputCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES)
        inputCipher.init(Cipher.ENCRYPT_MODE, publicKey)

        val outputStream = ByteArrayOutputStream()
        val cipherOutputStream = CipherOutputStream(outputStream, inputCipher)
        cipherOutputStream.write(secret)
        cipherOutputStream.close()

        return outputStream.toByteArray()
    }

    @Throws(Exception::class)
    private fun rsaDecrypt(encrypted: ByteArray): ByteArray {
        val keyStore = KeyStore.getInstance(PROVIDER_KEYSTORE)
        keyStore.load(null)
        val privateKey = keyStore.getKey(ALIAS_IDENTITY_KEYSTORE, null) as PrivateKey
        val output = Cipher.getInstance(RSA_MODE)
        output.init(Cipher.DECRYPT_MODE, privateKey)
        val cipherInputStream = CipherInputStream(ByteArrayInputStream(encrypted), output)
        val values = mutableListOf<Byte>()
        var nextByte: Int = cipherInputStream.read()
        while (nextByte != -1) {
            values.add(nextByte.toByte())
            nextByte = cipherInputStream.read()
        }

        val bytes = ByteArray(values.size)
        for (i in bytes.indices) {
            bytes[i] = values[i]
        }
        return bytes
    }

    private fun aesEncrypt(message: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, getAesKey(), GCMParameterSpec(128, FIXED_IV))
        return cipher.doFinal(message)
    }

    private fun aesDecrypt(encrypted: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.DECRYPT_MODE, getAesKey(), GCMParameterSpec(128, FIXED_IV))
        return cipher.doFinal(encrypted)
    }
}