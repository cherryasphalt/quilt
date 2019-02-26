package computer.lil.batchwork.network

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.SecretBox
import com.goterl.lazycode.lazysodium.interfaces.Sign
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

@RunWith(AndroidJUnit4::class)
class ScuttlebuttInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getTargetContext()
        Assert.assertEquals("computer.lil.batchwork", appContext.packageName)
    }

    @Test
    fun testBoxStream() {
        val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)
        val clientLongTermKey = ls.cryptoSignSeedKeypair(SecureRandom().generateSeed(Sign.SEEDBYTES))
        val serverLongTermKey = ls.cryptoSignSeedKeypair(SecureRandom().generateSeed(Sign.SEEDBYTES))
        val clientEphemeralKeyPair = ls.cryptoKxKeypair()
        val serverEphemeralKeyPair = ls.cryptoKxKeypair()

        val boxStreamClient = BoxStream(
            serverLongTermKey.publicKey,
            clientLongTermKey.publicKey,
            serverEphemeralKeyPair.publicKey.asBytes.sliceArray(0 until SecretBox.NONCEBYTES),
            clientEphemeralKeyPair.publicKey.asBytes.sliceArray(0 until SecretBox.NONCEBYTES)
        )

        val boxStreamServer = BoxStream(
            clientLongTermKey.publicKey,
            serverLongTermKey.publicKey,
            clientEphemeralKeyPair.publicKey.asBytes.sliceArray(0 until SecretBox.NONCEBYTES),
            serverEphemeralKeyPair.publicKey.asBytes.sliceArray(0 until SecretBox.NONCEBYTES)
        )

        Assert.assertEquals(ls.toHexStr(boxStreamClient.clientToServerNonce), ls.toHexStr(serverEphemeralKeyPair.publicKey.asBytes.sliceArray(0 until SecretBox.NONCEBYTES)))

        val message = "hello"
        val message2 = "world"
        val receivedMessage = boxStreamServer.readFromServer(boxStreamClient.sendToServer(message.toByteArray()))
        val receivedMessage2 = boxStreamServer.readFromServer(boxStreamClient.sendToServer(message2.toByteArray()))

        val message3 = "how are you"
        val message4 = "good"
        val receivedMessage3 = boxStreamClient.readFromServer(boxStreamServer.sendToServer(message3.toByteArray()))
        val receivedMessage4 = boxStreamClient.readFromServer(boxStreamServer.sendToServer(message4.toByteArray()))

        Assert.assertEquals(message, receivedMessage.toString(Charsets.UTF_8))
        Assert.assertEquals(message2, receivedMessage2.toString(Charsets.UTF_8))
        Assert.assertEquals(message3, receivedMessage3.toString(Charsets.UTF_8))
        Assert.assertEquals(message4, receivedMessage4.toString(Charsets.UTF_8))

        Assert.assertNotEquals(ls.toHexStr(boxStreamClient.clientToServerNonce), ls.toHexStr(serverEphemeralKeyPair.publicKey.asBytes.sliceArray(0 until SecretBox.NONCEBYTES)))
    }
}