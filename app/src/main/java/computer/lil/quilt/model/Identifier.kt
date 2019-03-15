package computer.lil.quilt.model

import java.sql.Types

class Identifier(
    val keyHash: String,
    val algorithm: String,
    val type: IdentityType
) {
    companion object {
        private const val IDENTIFER_REGEX = "([@%&])([a-zA-Z0-9+/]*={0,3})(\\.)(\\w)+"

        fun fromString(from: String): Identifier? {
            val regex = Regex(IDENTIFER_REGEX)
            if (from.length < 4 || regex.matchEntire(from) == null)
                return null

            val type = IdentityType.fromChar(from[0])
            val algoRegex = Regex("([@%&])([a-zA-Z0-9+/]*={0,3})(\\.)")

            val split = algoRegex.split(from, 2)
            val algo = split[1]
            val keyHash = from.substring(1 until (from.length - 1 - algo.length))

            return Identifier(keyHash, algo, type)
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
        SHA256("SHA256"),
        ED25519("ED25519")
    }

    fun getIdentifierString(): String {
        return "${type.symbol}$keyHash.$algorithm"
    }
}