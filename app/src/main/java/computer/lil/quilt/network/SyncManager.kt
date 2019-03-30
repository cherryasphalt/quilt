package computer.lil.quilt.network

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.squareup.moshi.Moshi
import computer.lil.quilt.data.repo.MessageRepository
import computer.lil.quilt.data.repo.PeerRepository
import computer.lil.quilt.database.Peer
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.model.ExtendedMessage
import computer.lil.quilt.protocol.Constants
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import okio.ByteString.Companion.decodeHex

class SyncManager(context: Context, val identityHandler: IdentityHandler) {
    var clientSubs = CompositeDisposable()
    val messageRepository = MessageRepository(context)
    val peerRepository = PeerRepository(context)
    val moshi = Constants.getMoshiInstance()

    private val connection = PeerConnection(
        identityHandler,
        remoteKey = "676acdbbda229c2f4bcd83dc69a3a31042c4ee92266d09cabb699b0b3066b0de".decodeHex()
    )

    fun startSync(lifecycleOwner: LifecycleOwner, context: Context) {
        val peers = peerRepository.getPeersFollowing()
        val peerList = mutableListOf<Peer>()
        peers.observe(lifecycleOwner,
            Observer<List<Peer>> {
                peerList.addAll(it)

                clientSubs.add(connection.connectToPeer("10.0.2.2", 8008)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        Log.d("success", it.toString())
                        val requestQueue = RequestQueue()
                        if (it) {
                            clientSubs.add(
                                connection.listenToPeer(peerList)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(Schedulers.newThread())
                                    .subscribeBy(
                                        onNext = {
                                            Log.d("server response", it.toString())
                                            if (it.enderror) {
                                                Log.d("enderror", moshi.adapter(Boolean::class.java).fromJson(it.body.utf8()).toString())
                                            } else if (it.requestNumber > 0) {
                                                requestQueue.add(it)
                                                requestQueue.processRequest(connection, context)
                                            } else {
                                                val messageJson = it.body.utf8()
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
            })
    }

    fun cancelSync() {
        clientSubs.dispose()
    }
}