package computer.lil.batchwork.handshake

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.Sign
import computer.lil.batchwork.identity.AndroidKeyStoreIdentityHandler
import computer.lil.batchwork.identity.BasicIdentityHandler
import computer.lil.batchwork.network.SSBClientHandshake
import computer.lil.batchwork.network.SSBServerHandshake
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
        val clientIdentityHandler = AndroidKeyStoreIdentityHandler(InstrumentationRegistry.getTargetContext())
        clientIdentityHandler.generateIdentityKeyPair()

        val serverIdentityHandler = BasicIdentityHandler()
        serverIdentityHandler.generateIdentityKeyPair()

        val clientHandshake = SSBClientHandshake(clientIdentityHandler, serverIdentityHandler.getIdentityPublicKey())
        val serverHandshake = SSBServerHandshake(serverIdentityHandler)

        assertTrue(serverHandshake.validateHello(clientHandshake.createHello()))
        assertTrue(clientHandshake.validateHelloResponse(serverHandshake.createHello()))
        assertTrue(serverHandshake.validateClientAuthentication(clientHandshake.createAuthenticateMessage()))
        assertTrue(clientHandshake.validateServerAcceptResponse(serverHandshake.createAccept()))
    }
}