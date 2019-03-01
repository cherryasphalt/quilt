package computer.lil.batchwork.util

import android.content.Context
import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.Sign
import computer.lil.batchwork.BuildConfig
import java.lang.Exception
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import android.security.keystore.KeyProperties
import android.security.keystore.KeyGenParameterSpec
import android.security.KeyPairGeneratorSpec
import android.os.Build
import java.math.BigInteger
import java.security.*
import java.security.spec.AlgorithmParameterSpec
import java.util.*
import javax.security.auth.x500.X500Principal
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import android.util.Base64

class Credentials() {
    class CredentialException(message:String): Exception(message)

    companion object {
        private const val SHARED_PREF_NAME = BuildConfig.APPLICATION_ID + ".PREFERENCE"

        private const val PREF_IDENTITY_PUBLIC_KEY = "PREF_IDENTITY_PUBLIC_KEY"
        private const val PREF_IDENTITY_ENCRYPTED_PRIVATE_KEY = "PREF_IDENTITY_ENCRYPTED_PRIVATE_KEY"
        private const val PREF_IDENTITY_ALGORITHM = "PREF_IDENTITY_ALGORITHM"

        private const val ALIAS_IDENTITY_KEYSTORE = "ALIAS_IDENTITY_KEYSTORE"
        private const val PROVIDER_KEYSTORE = "AndroidKeyStore"
        private const val RSA_MODE = "RSA/ECB/PKCS1Padding"

        /*
            Generates an identity key pair and stores it encrypted in the shared preferences.
            Returns true if successful.
         */
        fun generateIdentityKeyPair(context: Context): Boolean {
            if (!checkIdentityKeyPairExists(context)) {
                val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)
                ls.cryptoSignSeedKeypair(SecureRandom().generateSeed(Sign.SEEDBYTES))?.let { keyPair ->
                    val editor = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE).edit()
                    editor.putString(PREF_IDENTITY_PUBLIC_KEY, keyPair.publicKey.asPlainString)
                    editor.putString(PREF_IDENTITY_ALGORITHM, "ed25519")

                    //Encrypt private key
                    val encryptedPrivateKey = rsaEncrypt(keyPair.secretKey.asBytes).let {
                        Base64.encodeToString(it, Base64.DEFAULT);
                    }
                    editor.putString(PREF_IDENTITY_ENCRYPTED_PRIVATE_KEY, encryptedPrivateKey)

                    editor.apply()
                    return true
                }
            }
            return false
        }


        fun signUsingIdentity(context: Context, message: String, charset: Charset = Charsets.UTF_8): ByteArray {
            return Companion.signUsingIdentity(context, message.toByteArray(charset))
        }

        fun signUsingIdentity(context: Context, message: ByteArray): ByteArray {
            return ByteArray(0)
        }

        fun getIdentityString(context: Context): String {
            val pref= context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            pref.getString(PREF_IDENTITY_PUBLIC_KEY, null)?.let { publicKeyString ->
                pref.getString(PREF_IDENTITY_ALGORITHM, null)?.let { algorithmName ->
                    return "@$publicKeyString.$algorithmName"
                }
            }
            throw CredentialException("Identity not stored in preferences.")
        }

        fun checkIdentityKeyPairExists(context: Context): Boolean {
            val pref= context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            return pref.contains(PREF_IDENTITY_PUBLIC_KEY) && pref.contains(PREF_IDENTITY_ENCRYPTED_PRIVATE_KEY)
        }

        @Throws(
            NoSuchProviderException::class,
            NoSuchAlgorithmException::class,
            InvalidAlgorithmParameterException::class
        )
        private fun createKeystoreKeys(context: Context) {
            val start = GregorianCalendar()
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
                spec = KeyGenParameterSpec.Builder(ALIAS_IDENTITY_KEYSTORE, KeyProperties.PURPOSE_SIGN)
                    .setCertificateSubject(X500Principal("CN=$ALIAS_IDENTITY_KEYSTORE"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .setCertificateSerialNumber(BigInteger.valueOf(3456789765432141))
                    .setCertificateNotBefore(start.getTime())
                    .setCertificateNotAfter(end.getTime())
                    .build()
            }

            kpGenerator.initialize(spec)
            kpGenerator.generateKeyPair()
        }

        @Throws(Exception::class)
        private fun rsaEncrypt(secret: ByteArray): ByteArray {
            val keyStore = KeyStore.getInstance(PROVIDER_KEYSTORE);
            val privateKeyEntry = keyStore.getEntry(ALIAS_IDENTITY_KEYSTORE, null) as KeyStore.PrivateKeyEntry
            // Encrypt the text
            val inputCipher = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL")
            inputCipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.certificate.publicKey)

            val outputStream = ByteArrayOutputStream()
            val cipherOutputStream = CipherOutputStream(outputStream, inputCipher)
            cipherOutputStream.write(secret)
            cipherOutputStream.close()

            return outputStream.toByteArray()
        }

        @Throws(Exception::class)
        private fun rsaDecrypt(encrypted: ByteArray): ByteArray {
            val keyStore = KeyStore.getInstance(PROVIDER_KEYSTORE);
            val privateKeyEntry = keyStore.getEntry(ALIAS_IDENTITY_KEYSTORE, null) as KeyStore.PrivateKeyEntry
            val output = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL")
            output.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)
            val cipherInputStream = CipherInputStream(
                ByteArrayInputStream(encrypted), output
            )
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
    }
}