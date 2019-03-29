package computer.lil.quilt.network

import android.util.Log
import com.squareup.moshi.Moshi
import computer.lil.quilt.database.Peer
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.model.InviteCode
import computer.lil.quilt.model.RPCMessage
import computer.lil.quilt.model.RPCRequest
import computer.lil.quilt.protocol.BoxStream
import computer.lil.quilt.protocol.ClientHandshake
import computer.lil.quilt.protocol.RPCProtocol
import io.reactivex.Emitter
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import okio.*
import okio.ByteString.Companion.decodeHex
import java.io.Closeable
import java.io.IOException
import java.net.Socket
import java.util.concurrent.Executors

class PeerConnection(
    identityHandler: IdentityHandler,
    networkId: ByteArray = "d4a1cb88a66f02f8db635ce26441cc5dac1b08420ceaac230839b755845a9ffb".decodeHex().toByteArray(),
    remoteKey: ByteArray,
    val moshi: Moshi) {
    private val clientHandshake = ClientHandshake(identityHandler, remoteKey, networkId)
    var socket: Socket? = null
    var source: BufferedSource? = null
    var sink: BufferedSink? = null
    var boxStream: BoxStream? = null
    var executor = Executors.newScheduledThreadPool(3)
    var clientToServerRequestNumber = 0
    private val requestQueue = mutableListOf<RPCRequest>()

    fun listenToPeer(peerList: List<Peer>): Observable<RPCMessage> {
        val handler: ObservableOnSubscribe<RPCMessage> = ObservableOnSubscribe {
                emitter: ObservableEmitter<RPCMessage> ->
            val future = executor.submit {
                socket?.run {
                    if(isConnected && clientHandshake.completed) {
                        writeRequests(peerList)
                        readFromPeer(emitter)
                    }
                }
                //emitter.onComplete()
            }
            emitter.setCancellable { future.cancel(false) }
        }

        return Observable.create(handler)
    }

    fun connectToPeer(host: String, port: Int): Observable<Boolean> {
        val handler: ObservableOnSubscribe<Boolean> = ObservableOnSubscribe {
                emitter: ObservableEmitter<Boolean> ->
            val future = executor.submit {
                emitter.onNext(start(host, port))
                emitter.onComplete()
            }
            emitter.setCancellable { future.cancel(false) }
        }

        return Observable.create(handler)
    }

    fun connectToPub(inviteCode: InviteCode): Observable<Boolean> {
        val handler: ObservableOnSubscribe<Boolean> = ObservableOnSubscribe {
                emitter: ObservableEmitter<Boolean> ->
            val future = executor.submit {
                emitter.onNext(start(inviteCode.host, inviteCode.port))
                emitter.onComplete()
            }
            emitter.setCancellable { future.cancel(false) }
        }

        return Observable.create(handler)
    }


    private fun start(host: String, port: Int): Boolean {
        return try {
            //if (socket == null || !socket!!.isConnected || !clientHandshake.completed) {
                Socket(host, port).run {
                    socket = this
                    source = source().buffer()
                    sink = sink().buffer()
                    clientToServerRequestNumber = 0
                    requestQueue.clear()
                    performHandshake()
                }
            /*} else {
                return true
            }*/
        } catch (e: IOException) {
            false
        }
    }

    private fun performHandshake(): Boolean {
        val buffer = Buffer()
        sink?.run {
            write(clientHandshake.createHelloMessage())
            flush()
            source?.run {
                var byteCount = read(buffer, 8192L)
                val serverHello = buffer.readByteArray(byteCount)
                if (byteCount != -1L && clientHandshake.verifyHelloMessage(serverHello))
                    write(clientHandshake.createAuthenticateMessage())
                    flush()

                    byteCount = read(buffer, 8192L)
                    val acceptResponse = buffer.readByteArray(byteCount)
                    if (byteCount != -1L && clientHandshake.verifyServerAcceptResponse(acceptResponse)) {
                        boxStream = clientHandshake.createBoxStream()
                        return true
                    }
            }
        }
        return false
    }

    fun writeRequests(peerList: List<Peer>) {
        val jsonAdapter = moshi.adapter(RPCRequest.RequestCreateHistoryStream::class.java)
        for (peer in peerList) {
            val createHistoryStream = RPCRequest.RequestCreateHistoryStream(
                args = listOf(
                    RPCRequest.RequestCreateHistoryStream.Arg(
                        id = peer.id
                    )
                )
            )

            val payload = jsonAdapter.toJson(createHistoryStream).toByteArray()
            val response = RPCMessage(
                true,
                false,
                RPCProtocol.Companion.RPCBodyType.JSON,
                payload.size,
                ++clientToServerRequestNumber,
                payload
            )
            Log.d("writing body", createHistoryStream.toString())
            writeToPeer(response)
        }
    }

    fun writeToPeer(message: RPCMessage) {
        try {
            sink?.run {
                val rpcEncode = RPCProtocol.encode(message)
                Log.d("writing", message.toString())
                val boxStreamEncode = boxStream!!.sendToServer(rpcEncode)
                write(boxStreamEncode)
                flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            socket?.let {
                closeQuietly(it)
            }
        }
    }

    private fun readFromPeer(emitter: Emitter<RPCMessage>) {
        try {
            source?.run {
                val rpcBuffer = Buffer()
                var rpcExpectedLength = 0

                boxStream?.readFromServer(this)?.let { decoded ->
                    if (rpcBuffer.size == 0L)
                        rpcExpectedLength = RPCProtocol.getBodyLength(decoded) + RPCProtocol.HEADER_SIZE

                    rpcBuffer.write(decoded)
                    while (rpcExpectedLength != 0 && rpcBuffer.size >= (rpcExpectedLength.toLong())) {
                        emitter.onNext(
                            RPCProtocol.decode(
                                rpcBuffer.readByteArray(rpcExpectedLength.toLong())
                            )
                        )
                        rpcExpectedLength =
                            if (rpcBuffer.size >= RPCProtocol.HEADER_SIZE)
                                RPCProtocol.getBodyLength(rpcBuffer.peek().readByteArray()) + RPCProtocol.HEADER_SIZE
                            else
                                0
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            socket?.let {
                closeQuietly(it)
            }
        }
    }

    private fun closeQuietly(closeable: Closeable) {
        try {
            Log.d("closing socket", "closing")
            closeable.close()
        } catch (ignored: IOException) { }
    }

}