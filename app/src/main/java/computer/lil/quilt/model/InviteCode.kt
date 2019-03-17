package computer.lil.quilt.model


class InviteCode(
    val host: String,
    val port: Int,
    val pubKey: Identifier,
    val inviteKey: String
) {
    companion object {
        private const val INVITE_CODE_REGEX = "^((\\*)|((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)|((\\*\\.)?([a-zA-Z0-9-]+\\.){0,5}[a-zA-Z0-9-][a-zA-Z0-9-]+\\.[a-zA-Z]*))(:)([0-9]|[1-8][0-9]|9[0-9]|[1-8][0-9]{2}|9[0-8][0-9]|99[0-9]|[1-8][0-9]{3}|9[0-8][0-9]{2}|99[0-8][0-9]|999[0-9]|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])+(:)(([@%&])([a-zA-Z0-9+/]*={0,3})(\\.)(\\w)+)(~)(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)+\$"

        fun fromString(from: String): InviteCode? {
            val regex = Regex(INVITE_CODE_REGEX)
            if (from.length < 10 || regex.matchEntire(from) == null)
                return null

            val hostRegex = Regex("^((\\*)|((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)|((\\*\\.)?([a-zA-Z0-9-]+\\.){0,5}[a-zA-Z0-9-][a-zA-Z0-9-]+\\.[a-zA-Z]*))(?=:)")
            val host = hostRegex.find(from)?.value

            val portRegex = Regex("(?!:)([0-9]|[1-8][0-9]|9[0-9]|[1-8][0-9]{2}|9[0-8][0-9]|99[0-9]|[1-8][0-9]{3}|9[0-8][0-9]{2}|99[0-8][0-9]|999[0-9]|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])+(?=:)")
            val port = portRegex.find(from)?.value?.toInt()

            val pubKeyRegex = Regex("(?!:)(([@%&])([a-zA-Z0-9+/]*={0,3})(\\.)(\\w)+)(?=~)")
            val pubKey = pubKeyRegex.find(from)?.value?.let {
                Identifier.fromString(it)
            }

            val inviteKeyRegex = Regex("(?!~)(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)+\$")
            val inviteKey = inviteKeyRegex.find(from)?.value

            return InviteCode(host!!, port!!, pubKey!!, inviteKey!!)
        }
    }

    fun getInviteCode(): String {
        return "$host:$port:${pubKey.getIdentifierString()}~$inviteKey"
    }
}