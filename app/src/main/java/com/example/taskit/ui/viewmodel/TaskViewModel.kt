package com.example.taskit.ui.viewmodel

import com.example.taskit.ui.model.Bucket
import com.example.taskit.ui.model.Task
import kotlinx.coroutines.flow.StateFlow

interface TaskViewModel {
    val isWriting: StateFlow<Boolean>
    fun buildTasksStateFlow(bucket: Bucket): StateFlow<List<Task>>
    fun addTask(bucketId: Int)
    fun insertTask(taskAboveId: Int)
    fun updateTaskContent(taskId: Int, content: String)
    fun updateTaskState(taskId: Int, isChecked: Boolean)
    fun deleteTask(taskId: Int)
    fun reorderTask(fromTaskId: Int, toTaskId: Int)
    fun moveTaskToRoot(taskId: Int)
    fun moveTaskToChild(taskId: Int, taskAboveId: Int)
    fun onReorderStart(taskId: Int)
    fun onReorderEnd(taskId: Int)
}