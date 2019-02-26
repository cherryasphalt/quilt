package computer.lil.batchwork

import android.util.Log
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import computer.lil.batchwork.handshake.SSBClientHandshake
import io.reactivex.android.schedulers.AndroidSchedulers
import moe.codeest.rxsocketclient.RxSocketClient
import moe.codeest.rxsocketclient.SocketSubscriber
import moe.codeest.rxsocketclient.meta.SocketConfig
import moe.codeest.rxsocketclient.meta.ThreadStrategy
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.charset.StandardCharsets
import java.security.SecureRandom


@RunWith(AndroidJUnit4::class)
class ScuttlebuttInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("computer.lil.batchwork", appContext.packageName)
    }

    fun getHexString(array: ByteArray): String {
        val sb = StringBuilder()
        for (b in array) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }

    @Test
    fun connectScuttlebutt() {
        val sodium = SodiumAndroid()
        val lazySodium = LazySodiumAndroid(sodium, StandardCharsets.UTF_8)
        val keyPair = lazySodium.cryptoBoxKeypair()

        val clientHandshake = SSBClientHandshake(keyPair,
            "Z2rNu9oinC9LzYPcaaOjEELE7pImbQnKu2mbCzBmsN4=".toByteArray())

        val client = RxSocketClient.create(
            SocketConfig.Builder()
                .setIp("10.0.2.2")
                .setPort(8888)
                .setCharset(Charsets.UTF_8)
                .setThreadStrategy(ThreadStrategy.ASYNC)
                .setTimeout(30 * 1000)
                .build())

        val ref = client.connect()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object: SocketSubscriber() {
                override fun onResponse(data: ByteArray) {
                    Log.d("hello", "response")
                    if (clientHandshake.validateHelloResponse(data))
                        client.sendData(clientHandshake.createAuthenticateMessage())
                }

                override fun onDisconnected() {
                    Log.d("hello", "disconnected")
                }

                override fun onConnected() {
                    Log.d("hello", "connected")
                    Log.d("hello message length", clientHandshake.createHello().size.toString())
                    client.sendData(clientHandshake.createHello())
                }
            })
    }
}
