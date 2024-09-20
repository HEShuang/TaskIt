package com.example.taskit.ui.viewmodel

import com.example.taskit.ui.model.Bucket
import com.example.taskit.ui.model.Task
import kotlinx.coroutines.flow.StateFlow

interface TaskViewModel {
    fun buildTasksStateFlow(bucket: Bucket?): StateFlow<List<Task>>
    fun addTask(task: Task)
    fun updateTask(task: Task)
    fun deleteTask(task: Task)
    fun moveTask(fromIndex: Int, toIndex: Int, bucketId: Int)
    fun renameBucket()
}