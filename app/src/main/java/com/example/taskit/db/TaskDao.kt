package com.example.taskit.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.taskit.db.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Update
    suspend fun updateTasks(tasks: List<Task>)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE bucket_id = :bucketId ORDER BY task_order ASC")
    suspend fun getTasks(bucketId: Int): List<Task>

    @Query("SELECT * FROM tasks WHERE bucket_id = :bucketId ORDER BY task_order ASC")
    fun getTasksFlow(bucketId: Int): Flow<List<Task>>

}