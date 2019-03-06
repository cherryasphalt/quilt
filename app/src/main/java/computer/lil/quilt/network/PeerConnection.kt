package computer.lil.quilt.network

import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.protocol.BoxStream
import computer.lil.quilt.protocol.ClientHandshake
import computer.lil.quilt.protocol.RPCProtocol
import io.reactivex.Emitter
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.subjects.PublishSubject
import okio.*
import java.io.Closeable
import java.io.IOException
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PeerConnection(identityHandler: IdentityHandler, networkId: ByteArray, remoteKey: ByteArray) {

    private val clientHandshake = ClientHandshake(identityHandler, remoteKey, networkId)
    var socket: Socket? = null
    var source: BufferedSource? = null
    var sink: BufferedSink? = null
    var boxStream: BoxStream? = null

    val subject: PublishSubject<RPCProtocol.RPCMessage> = PublishSubject.create()

    var executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun listenToPeer(host: String, port: Int): Observable<RPCProtocol.RPCMessage> {
        val handler: ObservableOnSubscribe<RPCProtocol.RPCMessage> = ObservableOnSubscribe {
                emitter: ObservableEmitter<RPCProtocol.RPCMessage> ->
            val future = executor.submit {
                if (connectToPeer(host, port)) {
                    readFromPeer(emitter)
                }
                emitter.onComplete()
            }
            emitter.setCancellable { future.cancel(false) }
        }

        return Observable.create(handler)
    }

    fun connectToPeer(host: String, port: Int): Boolean {
        return try {
            Socket(host, port).run {
                source = source().buffer()
                sink = sink().buffer()
                performHandshake()
            }
        } catch (e: IOException) {
            false
        }
    }

    fun performHandshake(): Boolean {
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

    fun writeToPeer(message: RPCProtocol.RPCMessage) {
        try {
            sink?.run {
                val rpcEncode = RPCProtocol.encode(message)
                val boxStreamEncode = boxStream!!.sendToServer(rpcEncode)
                write(boxStreamEncode)
                flush()
            }
        } catch (e: IOException) {
            socket?.let {
                closeQuietly(it)
            }
        }
    }

    fun readFromPeer(emitter: Emitter<RPCProtocol.RPCMessage>) {
        try {
            source?.run {
                val buffer = Buffer()
                val rpcBuffer = Buffer()
                var rpcExpectedLength = 0

                var byteCount = read(buffer, 8192L)
                while (byteCount != -1L) {
                    val readBytes = buffer.readByteArray(byteCount)
                    val decoded = boxStream?.readFromServer(readBytes)

                    if (rpcBuffer.size == 0L)
                        rpcExpectedLength = RPCProtocol.getBodyLength(decoded!!) + RPCProtocol.HEADER_SIZE

                    rpcBuffer.write(decoded!!)
                    while (rpcExpectedLength != 0 && rpcBuffer.size >= rpcExpectedLength.toLong()) {
                        emitter.onNext(
                            RPCProtocol.decode(
                                rpcBuffer.readByteArray(rpcExpectedLength.toLong())
                            )
                        )

                        rpcExpectedLength = if (rpcBuffer.size > 0L)
                            RPCProtocol.getBodyLength(rpcBuffer.peek().readByteArray())
                        else
                            0
                    }

                    byteCount = read(buffer, 8192L)
                }
            }
        } catch (e: IOException) {
            socket?.let {
                closeQuietly(it)
            }
        }
    }

    private fun closeQuietly(closeable: Closeable) {
        try {
            closeable.close()
        } catch (ignored: IOException) { }
    }

}