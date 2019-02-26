package computer.lil.batchwork.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = arrayOf(Peer::class, Message::class), version = 1)
abstract class SSBDatabase : RoomDatabase() {
    abstract fun userDao(): MessageDao
}