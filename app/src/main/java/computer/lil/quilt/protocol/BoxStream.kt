package computer.lil.quilt.protocol

import com.goterl.lazycode.lazysodium.interfaces.SecretBox
import computer.lil.quilt.protocol.Crypto.Companion.increment
import computer.lil.quilt.protocol.Crypto.Companion.secretBoxOpen
import computer.lil.quilt.protocol.Crypto.Companion.secretBoxSeal
import computer.lil.quilt.protocol.Crypto.Companion.toByteString
import okio.Buffer
import okio.BufferedSource
import okio.ByteString
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import kotlin.math.min

class BoxStream(
    private val clientToServerKey: ByteString,
    private val serverToClientKey: ByteString,
    private val clientToServerNonce: Buffer,
    private val serverToClientNonce: Buffer
) {
    companion object {
        const val HEADER_SIZE = 34
        const val MAX_MESSAGE_SIZE = 4096
    }

    fun sendToClient(message: ByteString): ByteString {
        return encryptMessage(message, serverToClientKey, serverToClientNonce)
    }

    fun readFromClient(source: BufferedSource): ByteString {
        return decryptMessage(source, clientToServerKey, clientToServerNonce)
    }

    fun sendToServer(message: ByteString): ByteString {
        return encryptMessage(message, clientToServerKey, clientToServerNonce)
    }

    fun readFromServer(source: BufferedSource): ByteString? {
        return decryptSingle(source, serverToClientKey, serverToClientNonce)
    }

    fun createGoodbye(key: ByteString, nonce: Buffer): ByteString {
        return encryptMessage(ByteArray(18).toByteString(), key, nonce)
    }

    fun decryptMessage(source: BufferedSource, key: ByteString, nonce: Buffer): ByteString {
        val messages = Buffer()

        var decryptedMessage = decryptSingle(source, key, nonce)
        while (decryptedMessage != null) {
            messages.write(decryptedMessage)
            decryptedMessage = decryptSingle(source, key, nonce)
        }
        return messages.snapshot()
    }

    private fun decryptSingle(source: BufferedSource, key: ByteString, nonce: Buffer): ByteString? {
        val headerNonce = nonce.snapshot()
        val bodyNonce = nonce.snapshot().increment()

        val peekSource = source.peek()

        val encryptedHeader = Buffer()
        val bytesRead = peekSource.read(encryptedHeader, HEADER_SIZE.toLong())
        if (bytesRead == HEADER_SIZE.toLong()) {
            secretBoxOpen(encryptedHeader.readByteString(), key, headerNonce)?.let { header ->
                val messageLength = header.substring(0, 2).asByteBuffer().order(ByteOrder.BIG_ENDIAN).short.toLong()
                val bodyTag = header.substring(2, header.size)

                val encryptedBody = Buffer()
                return if (peekSource.read(encryptedBody, messageLength) == messageLength) {
                    val encryptedBodyWithHeader = ByteString.of(*bodyTag.toByteArray(), *encryptedBody.readByteArray())
                    val decryptedBody = Crypto.secretBoxOpen(encryptedBodyWithHeader, key, bodyNonce)

                    peekSource.close()
                    source.skip(HEADER_SIZE + messageLength)
                    nonce.increment()
                    nonce.increment()
                    decryptedBody
                } else {
                    null
                }
            }
        } else if (bytesRead == -1L) {
            return null
        }
        throw ProtocolException("Stream decryption error.")
    }

    fun encryptMessage(message: ByteString, key: ByteString, nonce: Buffer): ByteString {
        val messageCount = ceil(message.size.toFloat() / MAX_MESSAGE_SIZE.toFloat()).toInt()
        val encryptedMessages = Buffer()

        for (i in 0 until messageCount) {
            val messageStart = i * MAX_MESSAGE_SIZE
            val messageSize = min(message.size, MAX_MESSAGE_SIZE)
            val messageSegment = message.substring(messageStart, (messageStart + messageSize))
            val encryptedMessage = encryptSingle(messageSegment, key, nonce)
            encryptedMessages.write(encryptedMessage)
        }

        return encryptedMessages.snapshot()
    }

    private fun encryptSingle(messageSegment: ByteString, key: ByteString, nonce: Buffer): ByteString {
        val headerNonce = nonce.snapshot()
        val bodyNonce = nonce.increment().snapshot()
        nonce.increment()

        val encryptedBody = secretBoxSeal(messageSegment, key, bodyNonce)

        val headerValue = ByteString.of(
            *ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(encryptedBody.size - SecretBox.MACBYTES).array().sliceArray(2 until 4),
            *encryptedBody.substring(0, SecretBox.MACBYTES).toByteArray()
        )

        val encryptedHeader = secretBoxSeal(headerValue, key, headerNonce)

        return ByteString.of(*encryptedHeader.toByteArray(), *encryptedBody.substring(SecretBox.MACBYTES, encryptedBody.size).toByteArray())
    }
}