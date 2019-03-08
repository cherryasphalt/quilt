package computer.lil.quilt.database

import android.net.Uri
import androidx.room.TypeConverter

class DBTypeConverter {
    companion object {
        @TypeConverter
        @JvmStatic
        fun toHashAlgo(algo: String): HashAlgo {
            return HashAlgo.valueOf(algo)
        }

        @TypeConverter
        @JvmStatic
        fun fromHashAlgo(algo: HashAlgo): String {
            return algo.value
        }

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