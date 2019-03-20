package computer.lil.quilt.model

import computer.lil.quilt.protocol.RPCProtocol

data class RPCMessage(
    val stream: Boolean = true,
    val enderror: Boolean = false,
    val bodyType: RPCProtocol.Companion.RPCBodyType = RPCProtocol.Companion.RPCBodyType.JSON,
    val bodyLength: Int,
    val requestNumber: Int,
    val body: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RPCMessage

        if (stream != other.stream) return false
        if (enderror != other.enderror) return false
        if (bodyType != other.bodyType) return false
        if (bodyLength != other.bodyLength) return false
        if (requestNumber != other.requestNumber) return false
        if (!body.contentEquals(other.body)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stream.hashCode()
        result = 31 * result + enderror.hashCode()
        result = 31 * result + bodyType.hashCode()
        result = 31 * result + bodyLength
        result = 31 * result + requestNumber
        result = 31 * result + body.contentHashCode()
        return result
    }
}