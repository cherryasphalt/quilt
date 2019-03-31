package computer.lil.quilt.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.model.*
import okio.ByteString

class APIConfig(
    val networkId: ByteString,
    val identityHandler: IdentityHandler,
    val storage: SSBStorage? = null,
    val requestTypes: Set<Class<out RPCRequest>>,
    val moshi: Moshi
) {
    class Builder {
        private var networkId: ByteString = Constants.SSB_NETWORK_ID
        private var identityHandler: IdentityHandler? = null
        private val contentList: MutableSet<Pair<String, Class<out Content>>> = mutableSetOf()
        private val requestList: MutableSet<Class<out RPCRequest>> = mutableSetOf()
        private var storage: SSBStorage? = null

        fun withNetworkId(networkId: ByteString): Builder {
            this.networkId = networkId
            return this
        }

        fun withIdentity(identityHandler: IdentityHandler): Builder {
            this.identityHandler = identityHandler
            return this
        }

        fun addSocialContentTypes(): Builder {
            val socialSet = setOf(
                Pair("post", Content.Post::class.java),
                Pair("pub", Content.Pub::class.java),
                Pair("contact", Content.Contact::class.java),
                Pair("about", Content.About::class.java),
                Pair("channel", Content.Channel::class.java),
                Pair("vote", Content.Vote::class.java)
            )
            contentList.addAll(socialSet)
            return this
        }

        fun addSSBStorage(storage: SSBStorage): Builder {
            this.storage = storage
            return this
        }

        fun addContentType(typeName: String, type: Class<Content>): Builder {
            contentList.add(Pair(typeName, type))
            return this
        }

        fun addRequest(vararg types: Class<RPCRequest>): Builder {
            requestList.addAll(types)
            return this
        }

        fun build(): APIConfig {
            var contentAdapterFactory =
                PolymorphicJsonAdapterFactory.of(Content::class.java, "type")

            for (contentInfo in contentList)
                contentAdapterFactory = contentAdapterFactory.withSubtype(contentInfo.second, contentInfo.first)

            val moshi = Moshi.Builder()
                .add(Identifier.IdentifierJsonAdapter())
                .add(Adapters.DataTypeAdapter())
                .add(RPCJsonAdapterFactory())
                .add(contentAdapterFactory).build()

            identityHandler?.let { identity ->
                return APIConfig(networkId, identity, storage, requestList, moshi)
            }

            throw ConfigException("No identity set in configuration.")
        }
    }
}