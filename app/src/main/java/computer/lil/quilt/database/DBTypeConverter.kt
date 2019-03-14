package computer.lil.quilt.database

import android.net.Uri
import androidx.room.TypeConverter

class DBTypeConverter {
    companion object {
        @TypeConverter
        @JvmStatic
        fun toUri(uri: Uri): String {
            return uri.toString()
        }

        @TypeConverter
        @JvmStatic
        fun fromUri(from: String): Uri {
            return Uri.parse(from)
        }
    }
}