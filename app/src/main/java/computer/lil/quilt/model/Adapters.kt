package computer.lil.quilt.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.util.*

public class Adapters {
    public class DataTypeAdapter {
        @FromJson
        fun fromJson(reader: JsonReader): Date {
            return Date(reader.nextLong())
        }

        @ToJson
        fun toJson(writer: JsonWriter, value: Date) {
            writer.value(value.time)
        }
    }
}
