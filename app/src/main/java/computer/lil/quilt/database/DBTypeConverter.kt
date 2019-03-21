package computer.lil.quilt.database

import android.net.Uri
import androidx.room.TypeConverter
import java.util.*

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

        @TypeConverter
        @JvmStatic
        fun toDate(timestamp: Long): Date {
            return Date(timestamp)
        }

        @TypeConverter
        @JvmStatic
        fun fromDate(date: Date): Long {
            return date.time
        }
    }
}