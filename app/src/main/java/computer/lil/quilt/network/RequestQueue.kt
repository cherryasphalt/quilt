package computer.lil.quilt.network

import computer.lil.quilt.model.RPCMessage
import computer.lil.quilt.model.RPCRequest
import computer.lil.quilt.protocol.ProtocolException

class RequestQueue {
    private val queue = mutableListOf<Pair<Int, RPCRequest>>()
    private val requestNumberSet = mutableSetOf<Int>()

    fun add(request: RPCMessage) {
        if (checkValidRequest(request)) {
            requestNumberSet.add(request.requestNumber)
            //val rpcRequest = rpcRe
            //queue.add(Pair(request.requestNumber, request.body))
        }
        else
            throw ProtocolException("Invalid request received.")
    }

    private fun checkValidRequest(request: RPCMessage): Boolean {
        return request.requestNumber > 0
            && !requestNumberSet.contains(request.requestNumber)
    }
}
