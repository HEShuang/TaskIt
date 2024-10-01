package com.example.taskit.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.taskit.db.model.Bucket
import com.example.taskit.db.model.Task
import com.example.taskit.db.model.User


@Database(
    entities = [Bucket::class, Task::class, User::class],
    version = 1
)
abstract class AppDatabase: RoomDatabase() {
    abstract val bucketDao: BucketDao
    abstract val taskDao: TaskDao
    abstract val userDao: UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tasks.db"
                ).fallbackToDestructiveMigration()
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}