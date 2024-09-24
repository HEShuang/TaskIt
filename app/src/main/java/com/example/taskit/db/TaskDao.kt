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

    @Query("UPDATE tasks SET content = :content WHERE id = :taskId")
    suspend fun updateTaskContent(taskId: Int, content: String)

    @Query("UPDATE tasks SET is_checked = :isChecked WHERE id = :taskId")
    suspend fun updateTaskState(taskId: Int, isChecked: Boolean)

    @Update
    suspend fun updateTasks(tasks: List<Task>)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Int)

    @Query("SELECT * FROM tasks WHERE bucket_id = :bucketId ORDER BY task_order ASC")
    suspend fun getTasks(bucketId: Int): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTask(taskId: Int): Task?

    @Query("SELECT task_order FROM tasks WHERE bucket_id = :bucketId ORDER BY task_order DESC LIMIT 1")
    suspend fun getLastTaskOrder(bucketId: Int): Int?

    @Query("SELECT * FROM tasks WHERE bucket_id = :bucketId ORDER BY task_order ASC")
    fun getTasksFlow(bucketId: Int): Flow<List<Task>>

}