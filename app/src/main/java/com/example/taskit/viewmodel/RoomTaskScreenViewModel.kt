package com.example.taskit.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskit.db.AppRepository
import com.example.taskit.ui.model.Bucket as UiBucket
import com.example.taskit.ui.model.Task as UiTask
import com.example.taskit.db.model.Task as DbTask
import com.example.taskit.ui.viewmodel.TaskScreenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoomTaskScreenViewModel(
    private val repo: AppRepository,
) : TaskScreenViewModel, ViewModel() {
    private val _isWriting = MutableStateFlow(false)
    private val taskOnReorder = MutableStateFlow(-2)
    override val isWriting: StateFlow<Boolean>
        get() = _isWriting.asStateFlow()

    override fun buildTasksStateFlow(bucket: UiBucket): StateFlow<List<UiTask>> {
        Log.d("ViewModel", "buildTasksStateFlow")
        return combine(repo.getTasksFlow(bucket.id), taskOnReorder) { tasks, movingTaskId ->
            //The tasks retrieved from database are ordered by parentId and taskOrder
            //They need to be reordered by depth first traverse
            val tasksByParent = tasks.groupBy { it.parentId }
            val rootTasks = tasksByParent[-1] ?: emptyList()
            Log.d("Flow change","---------------------------")
            fun depthFirstTraverse(task: DbTask): List<UiTask> {
                val children = tasksByParent[task.id] ?: emptyList()
                //Hide the children of the task on reordering
                val isVisible = task.parentId != movingTaskId
                Log.d("Flow change", "id:${task.id}, parent:${task.parentId}, order:${task.taskOrder}, content:${task.content}")
                return listOf(task.toUiTask(bucket, isVisible)) + children.flatMap { childTask -> depthFirstTraverse(childTask) }
            }
            //transform each root task to a list of tasks, then flat map them all to a single list
            rootTasks.flatMap { rootTask -> depthFirstTraverse(rootTask) }
        }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    private fun DbTask.toUiTask(bucket: UiBucket, isVisible: Boolean) = UiTask(
        id = id,
        bucket = bucket,
        content = content,
        isChecked = isChecked,
        isChild = parentId >= 0,
        isVisible = isVisible,
    )
}