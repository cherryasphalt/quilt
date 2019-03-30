package computer.lil.quilt.protocol

import computer.lil.quilt.model.RPCMessage
import okio.BufferedSource
import okio.ByteString
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and
import kotlin.experimental.or
import computer.lil.quilt.protocol.Crypto.Companion.toByteString

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

        fun getBodyLength(header: ByteString): Int {
            if (header.size < HEADER_SIZE)
                throw ProtocolException("Header wrong size.")
            return header.substring(1, 5).asByteBuffer().order(ByteOrder.BIG_ENDIAN).int
        }

        fun decodeBodyLength(encoded: BufferedSource): Int {
            val header = ByteArray(HEADER_SIZE)
            val readSize = encoded.read(header)

            return if (readSize == HEADER_SIZE) getBodyLength(header.toByteString())
            else -1
        }

        fun decode(encoded: ByteString): RPCMessage {
            val header = encoded.substring(0, HEADER_SIZE)

            val flags = header[0]
            val stream = (flags and STREAM) != 0x00.toByte()
            val enderror = (flags and ENDERROR) != 0x00.toByte()

            val bodyType = when {
                flags and JSON_FLAG != 0x00.toByte() -> Companion.RPCBodyType.JSON
                flags and UTF8_FLAG != 0x00.toByte() -> Companion.RPCBodyType.UTF8
                else -> Companion.RPCBodyType.BINARY
            }

            val bodyLength = header.substring(1, 5).asByteBuffer().order(ByteOrder.BIG_ENDIAN).int
            val requestNumber = header.substring(5, 9).asByteBuffer().order(ByteOrder.BIG_ENDIAN).int
            val body = encoded.substring(HEADER_SIZE, (bodyLength + HEADER_SIZE))

            return RPCMessage(stream, enderror, bodyType, bodyLength, requestNumber, body)
        }


        fun goodbye(requestNumber: Int): ByteString {
            return encode(RPCMessage(false, true, Companion.RPCBodyType.BINARY, 9, requestNumber, ByteArray(9).toByteString()))
        }

        fun encode(rpcMessage: RPCMessage): ByteString {
            return encode(rpcMessage.body, rpcMessage.stream, rpcMessage.enderror, rpcMessage.bodyType, rpcMessage.requestNumber)
        }

        fun encode(
            body: ByteString,
            stream: Boolean = true,
            enderror: Boolean = false,
            bodyType: RPCBodyType = Companion.RPCBodyType.JSON,
            requestNumber: Int = 0
        ): ByteString {
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

            return ByteString.of(headerFlags, *bodyLength, *requestNumberArray, *body.toByteArray())
        }
    }
}
