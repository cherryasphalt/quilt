package computer.lil.quilt.network

import android.util.Log
import com.squareup.moshi.Moshi
import computer.lil.quilt.model.RPCJsonAdapterFactory
import computer.lil.quilt.model.RPCMessage
import computer.lil.quilt.model.RPCRequest
import computer.lil.quilt.protocol.ProtocolException
import computer.lil.quilt.protocol.RPCProtocol
import kotlin.reflect.jvm.internal.impl.protobuf.ByteString

class RequestQueue(val moshi: Moshi) {
    private val queue = mutableListOf<Pair<Int, RPCRequest>>()
    private val requestNumberSet = mutableSetOf<Int>()

    fun add(request: RPCMessage) {
        if (checkValidRequest(request)) {
            val moshi = Moshi.Builder().add(RPCJsonAdapterFactory()).build()
            val jsonAdapter = moshi.adapter(RPCRequest::class.java)
            val bodyString = ByteString.copyFrom(request.body).toStringUtf8()
            Log.d("request body string", bodyString)

            jsonAdapter.fromJson(bodyString)?.let {
                requestNumberSet.add(request.requestNumber)
                queue.add(Pair(request.requestNumber, it))
            }
        }
        else
            throw ProtocolException("Invalid request received.")
    }

    fun processRequest(connection: PeerConnection) {
        if (queue.isNotEmpty()) {
            val requestPair = queue.removeAt(0)
            val request = requestPair.second
            when (request.name[0]) {
                RPCRequest.REQUEST_CREATE_HISTORY_STREAM -> {
                    //get history from db
                    //endStream(requestPair.first, connection)
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
                            refuseRequest(requestPair.first, connection)
                        }
                        RPCRequest.REQUEST_CHANGES -> {
                            refuseRequest(requestPair.first, connection)
                        }
                        RPCRequest.REQUEST_CREATE_WANTS -> {
                            refuseRequest(requestPair.first, connection)
                        }
                    }
                }
            }
            Log.d("processRequest", "${requestPair.first}: ${requestPair.second}")
        }
    }

    private fun refuseRequest(requestNumber: Int, connection: PeerConnection) {
        //Retrieve wants
        val payload = "{}".toByteArray()
        val response = RPCMessage(
            true,
            false,
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
