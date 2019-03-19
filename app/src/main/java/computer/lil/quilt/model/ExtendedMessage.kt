package computer.lil.quilt.model

import java.util.*

data class ExtendedMessage (
    val key: Identifier,
    val value: MessageModel,
    val timestamp: Date
)