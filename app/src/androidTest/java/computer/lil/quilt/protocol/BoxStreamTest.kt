package computer.lil.quilt.protocol

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import computer.lil.quilt.identity.BasicIdentityHandler
import okio.Buffer
import okio.ByteString
import org.junit.Assert
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
    fun testBoxStream() {
        val clientIdentityHandler = BasicIdentityHandler.createWithGeneratedKeys()
        val serverIdentityHandler = BasicIdentityHandler.createWithGeneratedKeys()

        val clientHandshake = ClientHandshake(clientIdentityHandler, serverIdentityHandler.getIdentityPublicKey())
        val serverHandshake = ServerHandshake(serverIdentityHandler)

        Assert.assertTrue(serverHandshake.verifyHelloMessage(clientHandshake.createHelloMessage()))
        Assert.assertTrue(clientHandshake.verifyHelloMessage(serverHandshake.createHelloMessage()))
        Assert.assertTrue(serverHandshake.verifyClientAuthentication(clientHandshake.createAuthenticateMessage()))
        Assert.assertTrue(clientHandshake.verifyServerAcceptResponse(serverHandshake.createAcceptMessage()))

        val boxStreamClient = clientHandshake.createBoxStream()
        val boxStreamServer = serverHandshake.createBoxStream()

        val message = "hello"
        val message2 = "world"
        val receivedMessage = boxStreamServer.readFromServer(Buffer().write(boxStreamClient.sendToServer(ByteString.of(*message.toByteArray()))))
        val receivedMessage2 = boxStreamServer.readFromServer(Buffer().write(boxStreamClient.sendToServer(ByteString.of(*message2.toByteArray()))))

        val message3 = "how are you"
        val message4 = "good"
        val receivedMessage3 = boxStreamClient.readFromServer(Buffer().write(boxStreamServer.sendToServer(ByteString.of(*message3.toByteArray()))))
        val receivedMessage4 = boxStreamClient.readFromServer(Buffer().write(boxStreamServer.sendToServer(ByteString.of(*message4.toByteArray()))))

        Assert.assertEquals(message, receivedMessage?.utf8())
        Assert.assertEquals(message2, receivedMessage2?.utf8())
        Assert.assertEquals(message3, receivedMessage3?.utf8())
        Assert.assertEquals(message4, receivedMessage4?.utf8())
    }
}