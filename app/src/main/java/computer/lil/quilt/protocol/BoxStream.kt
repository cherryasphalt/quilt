package computer.lil.quilt.protocol

import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.goterl.lazycode.lazysodium.interfaces.SecretBox
import okio.*
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
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

    private val ls = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)

    private fun ByteArray.increment(): ByteArray {
        for (i in size - 1 downTo 0) {
            if (this[i] == 0xFF.toByte()) {
                this[i] = 0x00.toByte()
            } else {
                ++this[i]
                break
            }
        }
        return this
    }

    private fun ByteString.increment(): ByteString {
        return ByteString.of(*this.toByteArray().increment())
    }

    private fun Buffer.increment(): Buffer {
        val incremented = this.snapshot().increment()
        this.skip(this.size)
        this.write(incremented)
        return this
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
        return encryptMessage(ByteString.of(*ByteArray(18)), key, nonce)
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
            val header = ByteArray(HEADER_SIZE - SecretBox.MACBYTES)

            if (ls.cryptoSecretBoxOpenEasy(header, encryptedHeader.readByteArray(), bytesRead, headerNonce.toByteArray(), key.toByteArray())) {
                val messageLength =
                    ByteBuffer.wrap(header.sliceArray(0 until 2)).order(ByteOrder.BIG_ENDIAN).short.toLong()
                val bodyTag = header.sliceArray(2 until header.size)

                val encryptedBody = Buffer()
                if (peekSource.read(encryptedBody, messageLength) == messageLength) {
                    val encryptedBodyWithHeader = byteArrayOf(*bodyTag, *encryptedBody.readByteArray())
                    val decryptedBody = ByteArray(messageLength.toInt())
                    ls.cryptoSecretBoxOpenEasy(
                        decryptedBody,
                        encryptedBodyWithHeader,
                        encryptedBodyWithHeader.size.toLong(),
                        bodyNonce.toByteArray(),
                        key.toByteArray()
                    )

                    peekSource.close()
                    source.skip(HEADER_SIZE + messageLength)
                    nonce.increment()
                    nonce.increment()
                    return ByteString.of(*decryptedBody)
                } else {
                    return null
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

        val encryptedBody = ByteArray(messageSegment.size + SecretBox.MACBYTES)
        ls.cryptoSecretBoxEasy(encryptedBody, messageSegment.toByteArray(), messageSegment.size.toLong(), bodyNonce.toByteArray(), key.toByteArray())

        val headerValue = byteArrayOf(
            *ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(encryptedBody.size - SecretBox.MACBYTES).array().sliceArray(2 until 4),
            *encryptedBody.sliceArray(0 until SecretBox.MACBYTES)
        )

        val encryptedHeader = ByteArray(headerValue.size + SecretBox.MACBYTES)
        ls.cryptoSecretBoxEasy(encryptedHeader, headerValue, headerValue.size.toLong(), headerNonce.toByteArray(), key.toByteArray())

        return ByteString.of(*encryptedHeader, *encryptedBody.sliceArray(SecretBox.MACBYTES until encryptedBody.size))
    }
}