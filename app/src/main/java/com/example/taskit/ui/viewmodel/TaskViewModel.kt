package com.example.taskit.ui.viewmodel

import com.example.taskit.ui.model.Bucket
import com.example.taskit.ui.model.Task
import kotlinx.coroutines.flow.StateFlow

interface TaskViewModel {
    val isWriting: StateFlow<Boolean>
    fun buildTasksStateFlow(bucket: Bucket?): StateFlow<List<Task>>
    fun addTask(task: Task)
    fun updateTaskContent(taskId: Int, content: String)
    fun updateTaskState(taskId: Int, isChecked: Boolean)
    fun deleteTask(taskId: Int)
    fun moveTask(taskId: Int, toPos: Int, bucketId: Int)
}