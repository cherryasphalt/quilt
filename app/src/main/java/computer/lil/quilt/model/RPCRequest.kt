package computer.lil.quilt.model

import com.squareup.moshi.*

@JsonClass(generateAdapter = false)
open class RPCRequest(
    open val name: List<String>,
    open val type: RequestType
) {
    companion object {
        const val REQUEST_CREATE_HISTORY_STREAM = "createHistoryStream"
        const val REQUEST_BLOBS = "blobs"
        const val REQUEST_HAS = "has"
        const val REQUEST_GET = "get"
        const val REQUEST_GET_SLICE = "getSlice"
        const val REQUEST_CREATE_WANTS = "createWants"
    }

    @JsonClass(generateAdapter = true)
    data class RequestCreateHistoryStream(
        override val name: List<String> = listOf(RPCRequest.REQUEST_CREATE_HISTORY_STREAM),
        override val type: RequestType,
        val args: List<Arg>
    ): RPCRequest(name, type) {
        @JsonClass(generateAdapter = true)
        data class Arg(
            val id: String,
            val seq: Int = 1,
            val limit: Int = 100,
            val live: Boolean = false,
            val old: Boolean = true,
            val keys: Boolean = true
        )
    }

    @JsonClass(generateAdapter = true)
    data class RequestBlobsGet(
        override val name: List<String> = listOf(RPCRequest.REQUEST_BLOBS, RPCRequest.REQUEST_GET),
        override val type: RequestType,
        val args: List<Arg>
    ): RPCRequest(name, type) {
        @JsonClass(generateAdapter = true)
        data class Arg(
            val hash: String,
            val size: Long,
            val max: Long
        )
    }

    @JsonClass(generateAdapter = true)
    data class RequestBlobsGetSimple(
        override val name: List<String> = listOf(RPCRequest.REQUEST_BLOBS, RPCRequest.REQUEST_GET),
        override val type: RequestType,
        val args: List<String>
    ): RPCRequest(name, type)

    @JsonClass(generateAdapter = true)
    data class RequestBlobsGetSlice(
        override val name: List<String> = listOf(RPCRequest.REQUEST_BLOBS, RPCRequest.REQUEST_GET_SLICE),
        override val type: RequestType,
        val hash: String,
        val start: Long,
        val end: Long,
        val size: Long,
        val max: Long
    ): RPCRequest(name, type)

    @JsonClass(generateAdapter = true)
    data class RequestBlobsHas(
        override val name: List<String> = listOf(RPCRequest.REQUEST_BLOBS, RPCRequest.REQUEST_HAS),
        override val type: RequestType,
        val args: List<String>
    ): RPCRequest(name, type)


    @JsonClass(generateAdapter = true)
    data class RequestBlobsCreateWants(
        override val name: List<String> = listOf(RPCRequest.REQUEST_BLOBS, RPCRequest.REQUEST_CREATE_WANTS),
        override val type: RequestType,
        val args: List<String>
    ): RPCRequest(name, type)
}

enum class RequestType {
    @Json(name="async") ASYNC(),
    @Json(name="source") SOURCE()
}

class RPCRequestJsonAdapter(val moshi: Moshi): JsonAdapter<RPCRequest>() {

    override fun fromJson(reader: JsonReader): RPCRequest? {
        val nameFieldOptions = JsonReader.Options.of("name")
        val argsFieldOptions = JsonReader.Options.of("args")

        reader.peekJson()?.run {
            beginObject()
            while (hasNext()) {
                if (selectName(nameFieldOptions) == 0) {
                    beginArray()
                        while (hasNext()) {
                            when (nextString()) {
                                RPCRequest.REQUEST_CREATE_HISTORY_STREAM -> return moshi.adapter(RPCRequest.RequestCreateHistoryStream::class.java).fromJson(reader)
                                RPCRequest.REQUEST_BLOBS -> {
                                    when (nextString()) {
                                        RPCRequest.REQUEST_GET_SLICE -> return moshi.adapter(RPCRequest.RequestBlobsGet::class.java).fromJson(reader)
                                        RPCRequest.REQUEST_HAS -> return moshi.adapter(RPCRequest.RequestBlobsHas::class.java).fromJson(reader)
                                        RPCRequest.REQUEST_CREATE_WANTS -> return moshi.adapter(RPCRequest.RequestBlobsCreateWants::class.java).fromJson(reader)
                                        RPCRequest.REQUEST_GET -> {
                                            reader.peekJson()?.run {
                                                beginObject()
                                                while (hasNext()) {
                                                    if (selectName(argsFieldOptions) == 0) {
                                                        beginArray()
                                                        try {
                                                            nextString()
                                                            return moshi.adapter(RPCRequest.RequestBlobsGetSimple::class.java).fromJson(reader)
                                                        } catch (e: JsonDataException) {
                                                            break
                                                        }
                                                    }
                                                    skipName()
                                                    skipValue()
                                                }
                                                endObject()
                                            }
                                            return moshi.adapter(RPCRequest.RequestBlobsGet::class.java).fromJson(reader)
                                        }
                                    }
                                }
                            }
                        }
                    endArray()
                    break
                }
                skipName();
                skipValue();
            }
            endObject()
        }
        throw JsonDataException("Unknown request.")
    }

    override fun toJson(writer: JsonWriter, request: RPCRequest?) {
        request?.let {
            if (!it.name.isEmpty()) {
                when (it.name[0]) {
                    RPCRequest.REQUEST_CREATE_HISTORY_STREAM -> moshi.adapter(RPCRequest.RequestCreateHistoryStream::class.java).toJson(writer, request as RPCRequest.RequestCreateHistoryStream)
                    RPCRequest.REQUEST_BLOBS -> {
                        if (it.name.size == 2) {
                            when (it.name[1]) {
                                RPCRequest.REQUEST_GET_SLICE -> moshi.adapter(RPCRequest.RequestBlobsGetSlice::class.java).toJson(writer, request as RPCRequest.RequestBlobsGetSlice)
                                RPCRequest.REQUEST_HAS -> moshi.adapter(RPCRequest.RequestBlobsHas::class.java).toJson(writer, request as RPCRequest.RequestBlobsHas)
                                RPCRequest.REQUEST_CREATE_WANTS -> moshi.adapter(RPCRequest.RequestBlobsCreateWants::class.java).toJson(writer, request as RPCRequest.RequestBlobsCreateWants)
                                RPCRequest.REQUEST_GET -> {
                                    if (request is RPCRequest.RequestBlobsGetSimple)
                                        moshi.adapter(RPCRequest.RequestBlobsGetSimple::class.java).toJson(writer, request)
                                    else
                                        moshi.adapter(RPCRequest.RequestBlobsGet::class.java).toJson(writer, request as RPCRequest.RequestBlobsGet)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
