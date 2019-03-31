package computer.lil.quilt.api

import computer.lil.quilt.model.Identifier
import computer.lil.quilt.model.RPCRequest
import computer.lil.quilt.network.ConnectionPool

class ScuttlebuttAPI(val config: APIConfig = Constants.DEFAULT_SSB_CONFIG) {
    val pool: ConnectionPool = ConnectionPool(config.identityHandler, config.networkId)

    fun syncRemote() {

    }

    fun closeConnections() {

    }

    fun performRequestAllPeers(request: RPCRequest) {

    }

    fun performRequest(request: RPCRequest, peerId: Identifier) {

    }
}