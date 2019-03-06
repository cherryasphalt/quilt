package computer.lil.quilt

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import computer.lil.quilt.database.SSBDatabase
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.identity.AndroidKeyStoreIdentityHandler
import computer.lil.quilt.network.PeerConnection
import computer.lil.quilt.protocol.RPCProtocol
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import okio.ByteString.Companion.decodeHex
import java.nio.charset.StandardCharsets
import kotlin.reflect.jvm.internal.impl.protobuf.ByteString

class MainActivity : AppCompatActivity() {
    var clientSub: Disposable? = null
    private val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = SSBDatabase.getInstance(this)

        val identityHandler: IdentityHandler = AndroidKeyStoreIdentityHandler.getInstance(this)

        val client = PeerConnection(
            identityHandler,
            "d4a1cb88a66f02f8db635ce26441cc5dac1b08420ceaac230839b755845a9ffb".decodeHex().toByteArray(),
            "676acdbbda229c2f4bcd83dc69a3a31042c4ee92266d09cabb699b0b3066b0de".decodeHex().toByteArray()
        )

        clientSub = client.listenToPeer("10.0.2.2", 8008)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy (
                onNext = {
                    Log.d("got thing", it?.toString())
                    Log.d("json", ByteString.copyFrom(it?.body).toStringUtf8())
                    val response = ByteString.copyFromUtf8("{}")
                    /*client.writeToPeer(
                        RPCProtocol.RPCMessage(
                            true, false, RPCProtocol.Companion.RPCBodyType.JSON, response.size(),
                            -1, response.toByteArray()
                        )
                    )*/
                },
                onError = {
                    it.printStackTrace()
                }
            )
    }

    override fun onStop() {
        clientSub?.dispose()
        super.onStop()
    }
}
