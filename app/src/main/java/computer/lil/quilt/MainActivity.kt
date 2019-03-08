package computer.lil.quilt

import android.os.Bundle
import android.util.Log
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.squareup.moshi.JsonAdapter
import computer.lil.quilt.database.SSBDatabase
import computer.lil.quilt.identity.AndroidKeyStoreIdentityHandler
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.model.RPCRequest
import computer.lil.quilt.network.PeerConnection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import okio.ByteString.Companion.decodeHex
import java.nio.charset.StandardCharsets
import kotlin.reflect.jvm.internal.impl.protobuf.ByteString
import com.squareup.moshi.Moshi
import computer.lil.quilt.model.RPCRequestJsonAdapter
import java.lang.reflect.Type


class MainActivity : AppCompatActivity() {
    var clientSubs = CompositeDisposable()

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

        btn_retry.setOnClickListener {
            clientSubs.add(client.connectToPeer("10.0.2.2", 8008)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Log.d("success", it.toString())
                    if (it) {
                        clientSubs.add(
                            client.listenToPeer()
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeBy (
                                    onNext = {
                                        Log.d("request number", it?.requestNumber.toString())
                                        Log.d("json", ByteString.copyFrom(it?.body).toStringUtf8())

                                        val rpcJsonAdapterFactory = object : JsonAdapter.Factory {
                                            @Nullable
                                            override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
                                                if (type !== RPCRequest::class.java || !annotations.isEmpty()) {
                                                    return null
                                                }

                                                return RPCRequestJsonAdapter(moshi)
                                            }
                                        }

                                        val moshi = Moshi.Builder().add(rpcJsonAdapterFactory).build()
                                        val jsonAdapter = moshi.adapter(RPCRequest::class.java)
                                        val json = jsonAdapter.fromJson(ByteString.copyFrom(it.body).toStringUtf8())
                                        Log.d("json true", json.toString())
                                    },
                                    onError = {
                                        it.printStackTrace()
                                    }
                                )
                        )
                    }
                })
        }
    }

    override fun onStop() {
        clientSubs.dispose()
        super.onStop()
    }
}
