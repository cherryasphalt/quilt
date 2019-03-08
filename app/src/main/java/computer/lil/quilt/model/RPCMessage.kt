package computer.lil.quilt.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import computer.lil.quilt.protocol.RPCProtocol

data class RPCMessage(
    val stream: Boolean,
    val enderror: Boolean,
    val bodyType: RPCProtocol.Companion.RPCBodyType,
    val bodyLength: Int,
    val requestNumber: Int,
    val body: ByteArray
)