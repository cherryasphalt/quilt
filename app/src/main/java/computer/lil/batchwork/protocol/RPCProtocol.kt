package computer.lil.batchwork.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and
import kotlin.experimental.or

class RPCProtocol {
    companion object {
        const val HEADER_SIZE = 9

        const val STREAM: Byte = 0b00001000
        const val ENDERROR: Byte = 0b00000100
        const val UTF8_FLAG: Byte = 0b00000001
        const val JSON_FLAG: Byte = 0b00000010

        enum class RPCBodyType {
            UTF8, JSON, BINARY
        }
    }

    data class RPCMessage(
        val stream: Boolean,
        val enderror: Boolean,
        val bodyType: RPCBodyType,
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

    fun goodbye(requestNumber: Int): ByteArray {
        return encode(RPCMessage(false, true, Companion.RPCBodyType.BINARY, 9, requestNumber, ByteArray(9)))
    }

    fun encode(rpcMessage: RPCMessage): ByteArray {
        return encode(rpcMessage.body, rpcMessage.stream, rpcMessage.enderror, rpcMessage.bodyType, rpcMessage.requestNumber)
    }

    fun encode(
        body: ByteArray,
        stream: Boolean = true,
        enderror: Boolean = false,
        bodyType: RPCBodyType = Companion.RPCBodyType.JSON,
        requestNumber: Int = 0
    ): ByteArray {
        var headerFlags = 0x00.toByte()
        headerFlags = headerFlags or JSON_FLAG
        if (stream) headerFlags = headerFlags or STREAM
        if (enderror) headerFlags = headerFlags or ENDERROR
        headerFlags = when (bodyType) {
            Companion.RPCBodyType.JSON -> headerFlags or JSON_FLAG
            Companion.RPCBodyType.UTF8 -> headerFlags or UTF8_FLAG
            Companion.RPCBodyType.BINARY -> headerFlags
        }

        val bodyLength = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(body.size).array()
        val requestNumberArray = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(requestNumber).array()

        return byteArrayOf(headerFlags, *bodyLength, *requestNumberArray, *body)
    }

    fun decode(encoded: ByteArray): RPCMessage {
        val header = encoded.sliceArray(0 until HEADER_SIZE)

        val flags = header[0]
        val stream = (flags and STREAM) != 0x00.toByte()
        val enderror = (flags and ENDERROR) != 0x00.toByte()

        val bodyType = when {
            flags and JSON_FLAG != 0x00.toByte() -> Companion.RPCBodyType.JSON
            flags and UTF8_FLAG != 0x00.toByte() -> Companion.RPCBodyType.UTF8
            else -> Companion.RPCBodyType.BINARY
        }

        val bodyLength = ByteBuffer.wrap(header.sliceArray(1 until 5)).order(ByteOrder.BIG_ENDIAN).int
        val requestNumber = ByteBuffer.wrap(header.sliceArray(5 until 9)).order(ByteOrder.BIG_ENDIAN).int
        val body = encoded.sliceArray(HEADER_SIZE until (bodyLength + HEADER_SIZE))

        return RPCMessage(stream, enderror, bodyType, bodyLength, requestNumber, body)
    }

}
