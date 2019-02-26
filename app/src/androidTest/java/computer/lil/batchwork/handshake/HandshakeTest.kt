package computer.lil.batchwork.handshake

import android.util.Log
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.Sign
import org.junit.Assert
import org.junit.Assert.assertTrue
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
    fun testHandshake() {
        val lazySodium = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)
        val clientLongTermKey = lazySodium.cryptoSignSeedKeypair(SecureRandom().generateSeed(Sign.SEEDBYTES))
        val serverLongTermKey = lazySodium.cryptoSignSeedKeypair(SecureRandom().generateSeed(Sign.SEEDBYTES))

        val clientHandshake = SSBClientHandshake(clientLongTermKey, serverLongTermKey.publicKey.asBytes)
        val serverHandshake = SSBServerHandshake(serverLongTermKey)

        assertTrue(serverHandshake.validateHello(clientHandshake.createHello()))
        assertTrue(clientHandshake.validateHelloResponse(serverHandshake.createHello()))
        assertTrue(serverHandshake.validateClientAuthentication(clientHandshake.createAuthenticateMessage()))
        assertTrue(clientHandshake.validateServerAcceptResponse(serverHandshake.createAccept()))
    }
}