package computer.lil.quilt.network

import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.protocol.ClientHandshake
import java.net.Socket

class SSBClient(
    identityHandler: IdentityHandler,
    val networkId: ByteArray,
    remoteKey: ByteArray,
    val host: String,
    val port: Int) {

    val clientHandshake = ClientHandshake(identityHandler, remoteKey, networkId)
    var socket: Socket? = null

    fun connectToPeer() {
        socket = Socket(host, port)
        val inputStream = socket?.getInputStream()
        val outputStream = socket?.getOutputStream()
        //socket.
    }
}