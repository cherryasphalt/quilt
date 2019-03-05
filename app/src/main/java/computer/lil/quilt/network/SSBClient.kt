package computer.lil.quilt.network

import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.protocol.BoxStream
import computer.lil.quilt.protocol.ClientHandshake
import computer.lil.quilt.protocol.RPCProtocol
import io.reactivex.subjects.PublishSubject
import java.net.Socket
import okio.*
import java.io.*

class SSBClient(identityHandler: IdentityHandler, networkId: ByteArray, remoteKey: ByteArray) {

    private val clientHandshake = ClientHandshake(identityHandler, remoteKey, networkId)
    var socket: Socket? = null
    var source: BufferedSource? = null
    var sink: BufferedSink? = null
    var boxStream: BoxStream? = null

    private val openSockets = mutableSetOf<Socket>()
    val subject: PublishSubject<RPCProtocol.RPCMessage> = PublishSubject.create()

    fun connectToPeer(host: String, port: Int): Boolean {
        return try {
            Socket(host, port).run {
                source = source().buffer()
                sink = sink().buffer()
            }
            true
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

    fun readFromPeer() {
        try {
            source?.run {
                val buffer = Buffer()
                val rpcBuffer = Buffer()
                var rpcExpectedBodyLength = 0

                var byteCount = read(buffer, 8192L)
                while (byteCount != -1L) {
                    val readBytes = buffer.readByteArray(byteCount)
                    val decoded = boxStream?.readFromServer(readBytes)

                    if (rpcBuffer.size == 0L)
                        rpcExpectedBodyLength = RPCProtocol.getBodyLength(decoded!!)

                    rpcBuffer.write(decoded!!)
                    while (rpcExpectedBodyLength != 0 && rpcBuffer.size >= rpcExpectedBodyLength.toLong() + RPCProtocol.HEADER_SIZE) {
                        subject.onNext(
                            RPCProtocol.decode(
                                rpcBuffer.readByteArray(rpcExpectedBodyLength.toLong() + RPCProtocol.HEADER_SIZE)
                            )
                        )

                        rpcExpectedBodyLength = if (rpcBuffer.size > 0L)
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