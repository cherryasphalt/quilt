package computer.lil.quilt.protocol

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import computer.lil.quilt.model.Adapters
import computer.lil.quilt.model.Content
import computer.lil.quilt.model.Identifier
import computer.lil.quilt.model.RPCJsonAdapterFactory
import okio.ByteString.Companion.decodeHex

class Constants {
    companion object {
        @JvmField
        val SSB_NETWORK_ID = "d4a1cb88a66f02f8db635ce26441cc5dac1b08420ceaac230839b755845a9ffb".decodeHex()

        @JvmStatic
        fun getMoshiInstance(): Moshi =
            Moshi.Builder()
                .add(Identifier.IdentifierJsonAdapter())
                .add(Adapters.DataTypeAdapter())
                .add(RPCJsonAdapterFactory())
                .add(
                    PolymorphicJsonAdapterFactory.of(Content::class.java, "type")
                        .withSubtype(Content.Post::class.java, "post")
                        .withSubtype(Content.Pub::class.java, "pub")
                        .withSubtype(Content.Contact::class.java, "contact")
                        .withSubtype(Content.About::class.java, "about")
                        .withSubtype(Content.Channel::class.java, "channel")
                        .withSubtype(Content.Vote::class.java, "vote")
                ).build()
    }
}