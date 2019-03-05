package computer.lil.quilt.handshake

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import computer.lil.quilt.identity.AndroidKeyStoreIdentityHandler
import computer.lil.quilt.identity.BasicIdentityHandler
import computer.lil.quilt.protocol.ClientHandshake
import computer.lil.quilt.protocol.ServerHandshake
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class ScuttlebuttInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getTargetContext()
        Assert.assertEquals("computer.lil.batchwork", appContext.packageName)
    }

    @Test
    fun testHandshake() {
        val appContext = InstrumentationRegistry.getTargetContext()
        val clientIdentityHandler =
            if (!AndroidKeyStoreIdentityHandler.checkIdentityKeyPairExists(appContext))
                AndroidKeyStoreIdentityHandler.createWithGeneratedKeys(appContext)
            else
                AndroidKeyStoreIdentityHandler(appContext)
        val serverIdentityHandler = BasicIdentityHandler.createWithGeneratedKeys()

        val clientHandshake = ClientHandshake(clientIdentityHandler, serverIdentityHandler.getIdentityPublicKey())
        val serverHandshake = ServerHandshake(serverIdentityHandler)

        assertTrue(serverHandshake.verifyHelloMessage(clientHandshake.createHelloMessage()))
        assertTrue(clientHandshake.verifyHelloMessage(serverHandshake.createHelloMessage()))
        assertTrue(serverHandshake.verifyClientAuthentication(clientHandshake.createAuthenticateMessage()))
        assertTrue(clientHandshake.verifyServerAcceptResponse(serverHandshake.createAcceptMessage()))
    }
}