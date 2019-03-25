package computer.lil.quilt.network

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import computer.lil.quilt.data.repo.MessageRepository
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.model.ExtendedMessage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import okio.ByteString
import okio.ByteString.Companion.decodeHex

class SyncManager(context: Context, val identityHandler: IdentityHandler, val moshi: Moshi) {
    var clientSubs = CompositeDisposable()
    val messageRepository = MessageRepository(context)
    private val connection = PeerConnection(
        identityHandler,
        remoteKey = "676acdbbda229c2f4bcd83dc69a3a31042c4ee92266d09cabb699b0b3066b0de".decodeHex().toByteArray(),
        moshi = moshi
    )

    fun startSync() {
        clientSubs.add(connection.connectToPeer("10.0.2.2", 8008)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Log.d("success", it.toString())
                val requestQueue = RequestQueue(moshi)
                if (it) {
                    clientSubs.add(
                        connection.listenToPeer()
                            .subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.io())
                            .subscribeBy(
                                onNext = {
                                    Log.d("server response", it.toString())
                                    if (it.requestNumber > 0) {
                                        requestQueue.add(it)
                                        requestQueue.processRequest(connection)
                                    } else {
                                        val messageJson = ByteString.of(*it.body).utf8()
                                        Log.d("not request", messageJson)
                                        moshi.adapter(ExtendedMessage::class.java).fromJson(messageJson)?.let {
                                            messageRepository.saveMessage(it)
                                        }
                                    }
                                },
                                onError = {
                                    it.printStackTrace()
                                }
                            )
                    )
                }
            }
        )
    }

    fun cancelSync() {
        clientSubs.dispose()
    }
}