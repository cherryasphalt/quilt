package computer.lil.quilt.model

import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
class ExtendedMessage (
    val key: Identifier,
    val value: MessageModel,
    val timestamp: Date
) {
    constructor(
        value: MessageModel,
        timestamp: Date
    ): this(value.createMessageId(), value, timestamp)

    constructor(
        value: MessageModel
    ): this(value.createMessageId(), value, Date(System.currentTimeMillis()))
}