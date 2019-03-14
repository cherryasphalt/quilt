package computer.lil.quilt.model

import computer.lil.quilt.protocol.RPCProtocol

data class RPCMessage(
    val stream: Boolean = true,
    val enderror: Boolean = false,
    val bodyType: RPCProtocol.Companion.RPCBodyType = RPCProtocol.Companion.RPCBodyType.JSON,
    val bodyLength: Int,
    val requestNumber: Int,
    val body: ByteArray
)