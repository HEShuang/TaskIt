package com.example.taskit.viewmodel

import com.example.taskit.ui.model.Bucket
import com.example.taskit.ui.model.Task
import com.example.taskit.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.flow.StateFlow

class MemoryTaskViewModel: TaskViewModel {
    override fun buildTasksStateFlow(bucket: Bucket?): StateFlow<List<Task>> {
        TODO("Not yet implemented")
    }

    override fun addTask(task: Task) {
        TODO("Not yet implemented")
    }

    override fun updateTask(task: Task) {
        TODO("Not yet implemented")
    }

    override fun deleteTask(task: Task) {
        TODO("Not yet implemented")
    }

    override fun moveTask(fromIndex: Int, toIndex: Int, bucketId: Int) {
        TODO("Not yet implemented")
    }

    override fun renameBucket() {
        TODO("Not yet implemented")
    }
}