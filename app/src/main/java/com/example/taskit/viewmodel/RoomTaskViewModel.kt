package com.example.taskit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskit.db.AppRepository
import com.example.taskit.ui.ext.toUiTask
import com.example.taskit.ui.model.Bucket as UiBucket
import com.example.taskit.ui.model.Task as UiTask
import com.example.taskit.db.model.Task as DbTask
import com.example.taskit.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoomTaskViewModel(
    private val repo: AppRepository,
) : TaskViewModel, ViewModel() {
    private val _isWriting = MutableStateFlow(false)
    private val taskOnReorder = MutableStateFlow(-2)
    override val isWriting: StateFlow<Boolean>
        get() = _isWriting.asStateFlow()

    override fun buildTasksStateFlow(bucket: UiBucket): StateFlow<List<UiTask>> {
        return combine(repo.getTasksFlow(bucket.id), taskOnReorder) { tasks, taskOnReorder ->
            tasks.map { task ->
                task.toUiTask(
                    bucket,
                    task.parentId != taskOnReorder
                ) //Hide the children of the task on reordering
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    override fun addTask(bucketId: Int) {
        launchWriteTask {
            repo.appendTask(DbTask(bucketId = bucketId))
        }
    }

    override fun insertTask(taskAboveId: Int) {
        launchWriteTask {
            repo.insertTask(DbTask(), taskAboveId)
        }
    }

    override fun updateTaskContent(taskId: Int, content: String) {
        launchWriteTask {
            repo.updateTaskContent(taskId, content)
        }
    }

    override fun updateTaskState(taskId: Int, isChecked: Boolean) {
        launchWriteTask {
            repo.updateTaskState(taskId, isChecked)
        }
    }

    override fun deleteTask(taskId: Int) {
        launchWriteTask {
            repo.deleteTask(taskId)
        }
    }

    override fun reorderTask(fromTaskId: Int, toTaskId: Int) {
        launchWriteTask {
            repo.reorderTask(fromTaskId, toTaskId)
        }
    }

    override fun moveTaskToRoot(taskId: Int) {
        launchWriteTask {
            repo.toRootTask(taskId)
        }
    }

    override fun moveTaskToChild(taskId: Int, taskAboveId: Int) {
        launchWriteTask {
            repo.toSubtask(taskId, taskAboveId)
        }
    }

    override fun onReorderStart(taskId: Int) {
        taskOnReorder.value = taskId
    }

    override fun onReorderEnd(taskId: Int) {
        if (taskOnReorder.value == taskId) {
            taskOnReorder.value = -2
        }
    }

    private fun launchWriteTask(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            _isWriting.value = true
            block()
            _isWriting.value = false
        }
    }
}