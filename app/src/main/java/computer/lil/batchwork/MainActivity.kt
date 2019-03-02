package computer.lil.batchwork

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.goterl.lazycode.lazysodium.interfaces.SecretBox
import com.goterl.lazycode.lazysodium.utils.Key
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import computer.lil.batchwork.database.SSBDatabase
import computer.lil.batchwork.identity.IdentityHandler
import computer.lil.batchwork.protocol.ClientHandshake
import computer.lil.batchwork.protocol.BoxStream
import computer.lil.batchwork.protocol.RPCProtocol
import computer.lil.batchwork.model.SSBClient
import computer.lil.batchwork.identity.AndroidKeyStoreIdentityHandler
import computer.lil.batchwork.protocol.Handshake
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import moe.codeest.rxsocketclient.RxSocketClient
import moe.codeest.rxsocketclient.SocketSubscriber
import moe.codeest.rxsocketclient.meta.SocketConfig
import moe.codeest.rxsocketclient.meta.ThreadStrategy

class MainActivity : AppCompatActivity() {
    var ref: Disposable? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = SSBDatabase.getInstance(this)

        val identityHandler: IdentityHandler = AndroidKeyStoreIdentityHandler(this)
        identityHandler.generateIdentityKeyPair()
        val clientHandshake = ClientHandshake(
            identityHandler,
            Key.fromHexString("676acdbbda229c2f4bcd83dc69a3a31042c4ee92266d09cabb699b0b3066b0de").asBytes
        )
        var boxStream: BoxStream? = null

        val client = RxSocketClient.create(
            SocketConfig.Builder()
                .setIp("10.0.2.2")
                .setPort(8008)
                .setCharset(Charsets.UTF_8)
                .setThreadStrategy(ThreadStrategy.ASYNC)
                .setTimeout(30 * 1000)
                .build())

        ref = client.connect()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object: SocketSubscriber() {
                override fun onResponse(data: ByteArray) {
                    Log.d("response", data.toString())
                    when (clientHandshake.state) {
                        Handshake.State.PHASE1 -> {
                            if (clientHandshake.verifyHelloMessage(data)) {
                                client.sendData(clientHandshake.createAuthenticateMessage())
                                clientHandshake.state = Handshake.State.PHASE2
                            }
                        }
                        Handshake.State.PHASE2 -> {
                            val success = clientHandshake.validateServerAcceptResponse(data)
                            Log.d("authentication", success.toString())
                            /*boxStream = BoxStream(
                                clientHandshake.remoteKey,
                                identityHandler.getIdentityPublicKey(),
                                clientHandshake.serverEphemeralKey!!.sliceArray(0 until SecretBox.NONCEBYTES),
                                clientHandshake.localEphemeralKeyPair.publicKey.asBytes.sliceArray(0 until SecretBox.NONCEBYTES)
                            )*/
                            clientHandshake.state = Handshake.State.PHASE3
                        }
                        Handshake.State.PHASE3 -> {
                            val protocol = RPCProtocol()
                            /*boxStream?.run {
                                Log.d("finished", lazySodium.toHexStr(this.readFromServer(protocol.decode(data).body)))
                            }*/

                            val moshi = Moshi.Builder().build()
                            val adapter: JsonAdapter<SSBClient.Request> = moshi.adapter(
                                SSBClient.Request::class.java)
                            val request = adapter.toJson(
                                SSBClient.Request(listOf("createHistoryStream"),
                                    SSBClient.RequestType.SOURCE,
                                    mapOf(
                                        "id" to "",
                                        "limit" to 1
                                    )
                                )
                            )
                            Log.d("request object", request)
                            boxStream?.run {
                                client.sendData(sendToServer (protocol.encode(request.toByteArray())))
                            }
                        }
                    }
                }

                override fun onDisconnected() {
                    Log.d("hello", "disconnected")
                }

                override fun onConnected() {
                    Log.d("hello", "connected")
                    client.sendData(clientHandshake.createHelloMessage())
                }
            })
    }

    override fun onStop() {
        ref?.dispose()
        super.onStop()
    }
}
