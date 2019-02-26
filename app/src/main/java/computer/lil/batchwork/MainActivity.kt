package computer.lil.batchwork

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.Sign
import com.goterl.lazycode.lazysodium.utils.Key
import computer.lil.batchwork.database.SSBDatabase
import computer.lil.batchwork.handshake.SSBClientHandshake
import io.reactivex.android.schedulers.AndroidSchedulers
import moe.codeest.rxsocketclient.RxSocketClient
import moe.codeest.rxsocketclient.SocketSubscriber
import moe.codeest.rxsocketclient.meta.SocketConfig
import moe.codeest.rxsocketclient.meta.ThreadStrategy
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = Room.databaseBuilder(
            applicationContext,
            SSBDatabase::class.java, "ssb-database"
        ).build()

        val lazySodium = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)
        val longTermKeyPair = lazySodium.cryptoSignSeedKeypair(SecureRandom().generateSeed(Sign.SEEDBYTES))
        val clientHandshake = SSBClientHandshake(longTermKeyPair, Key.fromHexString("676acdbbda229c2f4bcd83dc69a3a31042c4ee92266d09cabb699b0b3066b0de").asBytes)

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
            .doOnError {
                it.printStackTrace()
            }
            .subscribe(object: SocketSubscriber() {
                override fun onResponse(data: ByteArray) {
                    Log.d("hello", "response")
                    when (clientHandshake.state) {
                        SSBClientHandshake.State.STEP1 -> {
                            if (clientHandshake.validateHelloResponse(data)) {
                                client.sendData(clientHandshake.createAuthenticateMessage())
                                clientHandshake.state = SSBClientHandshake.State.STEP2
                            }
                        }
                        SSBClientHandshake.State.STEP2 -> {
                            val success = clientHandshake.validateServerAcceptResponse(data)
                            Log.d("authentication", success.toString())
                            clientHandshake.state = SSBClientHandshake.State.STEP3
                        }
                        SSBClientHandshake.State.STEP3 -> {
                            Log.d("finished", data.toString())
                        }
                    }
                }

                override fun onDisconnected() {
                    Log.d("hello", "disconnected")
                }

                override fun onConnected() {
                    Log.d("hello", "connected")
                    client.sendData(clientHandshake.createHello())
                }
            })
    }
}
