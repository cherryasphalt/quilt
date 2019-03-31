package computer.lil.quilt.network

import android.content.Context
import android.util.Log
import computer.lil.quilt.data.repo.MessageRepository
import computer.lil.quilt.model.RPCMessage
import computer.lil.quilt.model.RPCRequest
import computer.lil.quilt.api.Constants
import computer.lil.quilt.protocol.Crypto.Companion.toByteString
import computer.lil.quilt.protocol.ProtocolException
import computer.lil.quilt.protocol.RPCProtocol

class RequestQueue() {
    private val queue = mutableListOf<Pair<Int, RPCRequest>>()
    private val requestNumberSet = mutableSetOf<Int>()

    fun add(request: RPCMessage) {
        if (checkValidRequest(request)) {
            val moshi = Constants.getMoshiInstance()
            val jsonAdapter = moshi.adapter(RPCRequest::class.java)
            val bodyString = request.body.utf8()
            Log.d("request body string", bodyString)

            jsonAdapter.fromJson(bodyString)?.let {
                requestNumberSet.add(request.requestNumber)
                queue.add(Pair(request.requestNumber, it))
            }
        }
        else
            throw ProtocolException("Invalid request received.")
    }

    fun processRequest(connection: PeerConnection, context: Context) {
        if (queue.isNotEmpty()) {
            val messageRepo = MessageRepository(context)

            val requestPair = queue.removeAt(0)
            val request = requestPair.second
            when (request.name[0]) {
                RPCRequest.REQUEST_CREATE_HISTORY_STREAM -> {
                    val args = (request as RPCRequest.RequestCreateHistoryStream).args
                    if (!args.isEmpty()) {
                        for (message in messageRepo.getMessagesFromId(args[0].id, args[0].seq)) {

                        }
                    }
                    endStream(requestPair.first, connection)
                }
                RPCRequest.REQUEST_CREATE_USER_STREAM -> {
                    //something
                }
                RPCRequest.REQUEST_BLOBS -> {
                    when (request.name[1]) {
                        RPCRequest.REQUEST_GET -> {
                            //writeToPe
                        }
                        RPCRequest.REQUEST_GET_SLICE -> {
                            //writeToPe
                        }
                        RPCRequest.REQUEST_HAS -> {
                            //refuseRequest(requestPair.first, connection)
                        }
                        RPCRequest.REQUEST_CHANGES -> {
                            //refuseRequest(requestPair.first, connection)
                        }
                        RPCRequest.REQUEST_CREATE_WANTS -> {
                            //refuseRequest(requestPair.first, connection)
                        }
                    }
                }
            }
            Log.d("processRequest", "${requestPair.first}: ${requestPair.second}")
        }
    }

    private fun endStream(requestNumber: Int, connection: PeerConnection) {
        val payload = "true".toByteArray().toByteString()
        val response = RPCMessage(
            true,
            true,
            RPCProtocol.Companion.RPCBodyType.JSON,
            payload.size,
            -requestNumber,
            payload
        )
        connection.writeToPeer(response)
    }

    private fun checkValidRequest(request: RPCMessage): Boolean {
        return request.requestNumber > 0
            && !requestNumberSet.contains(request.requestNumber)
    }
}
