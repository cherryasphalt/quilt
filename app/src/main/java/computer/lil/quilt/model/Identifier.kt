package computer.lil.quilt.model

import androidx.room.TypeConverter
import com.squareup.moshi.*

class Identifier(
    val keyHash: String,
    val algorithm: AlgoType,
    val type: IdentityType
) {
    companion object {
        private const val IDENTIFER_REGEX = "([@%&])([a-zA-Z0-9+/]*={0,3})(\\.)(\\w)+"

        @TypeConverter
        fun fromString(from: String): Identifier? {
            val regex = Regex(IDENTIFER_REGEX)
            if (from.length < 4 || regex.matchEntire(from) == null)
                return null

            val type = IdentityType.fromChar(from[0])
            val algoRegex = Regex("([@%&])([a-zA-Z0-9+/]*={0,3})(\\.)")

            val split = algoRegex.split(from, 2)
            AlgoType.fromString(split[1])?.let { algo ->
                val keyHash = from.substring(1 until (from.length - 1 - algo.algo.length))
                return Identifier(keyHash, algo, type)
            }
            return null
        }

        @TypeConverter
        fun toString(identifier: Identifier): String {
            return identifier.toString()
        }
    }

    enum class IdentityType(val symbol: Char) {
        IDENTITY('@'),
        MESSAGE('%'),
        BLOB('&');

        companion object {
            fun fromChar(symbol: Char) = IdentityType.values().first { it.symbol == symbol }
        }
    }

    enum class AlgoType(val algo: String) {
        SHA256("sha256"),
        ED25519("ed25519");

        companion object {
            private val map = AlgoType.values().associateBy(AlgoType::algo)
            fun fromString(type: String) = map[type]
        }
    }

    override fun toString(): String {
        return "${type.symbol}$keyHash.$algorithm"
    }

    override fun equals(other: Any?): Boolean {
        return other != null
            && other is Identifier
            && keyHash == other.keyHash
            && algorithm == other.algorithm
            && type == other.type
    }

    override fun hashCode(): Int {
        var result = keyHash.hashCode()
        result = 31 * result + algorithm.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    class IdentifierJsonAdapter {
        @FromJson
        fun fromJson(from: String): Identifier {
            return fromString(from)!!
        }

        @ToJson
        fun toJson(value: Identifier): String {
            return value.toString()
        }

    }
}