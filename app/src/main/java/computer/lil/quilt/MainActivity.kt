package computer.lil.quilt

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import computer.lil.quilt.database.SSBDatabase
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.identity.AndroidKeyStoreIdentityHandler
import computer.lil.quilt.network.SSBClient
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.*
import okio.ByteString.Companion.decodeHex
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {
    var ref: Disposable? = null
    private val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = SSBDatabase.getInstance(this)

        val identityHandler: IdentityHandler = AndroidKeyStoreIdentityHandler.getInstance(this)

        val client = SSBClient(
            identityHandler,
            "d4a1cb88a66f02f8db635ce26441cc5dac1b08420ceaac230839b755845a9ffb".decodeHex().toByteArray(),
            "676acdbbda229c2f4bcd83dc69a3a31042c4ee92266d09cabb699b0b3066b0de".decodeHex().toByteArray()
        )

        val ioScope = CoroutineScope(Dispatchers.IO)
        val mainScope = CoroutineScope(Dispatchers.Main)
        mainScope.launch {
            val ioData = async(Dispatchers.IO) {
                client.connectToPeer("10.0.2.2", 8008)
                client.performHandshake()
            }

            val success = ioData.await()
            Log.d("success", success.toString())

            client.subject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Log.d("got thing", it?.toString())
                }

            launch(Dispatchers.IO) {
                client.readFromPeer()
            }
        }
    }

    override fun onStop() {
        ref?.dispose()
        super.onStop()
    }
}
