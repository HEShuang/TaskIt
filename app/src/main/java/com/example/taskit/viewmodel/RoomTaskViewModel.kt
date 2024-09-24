package com.example.taskit.viewmodel

import android.util.Log
import com.example.taskit.db.TaskDao
import com.example.taskit.ui.model.Bucket as UiBucket
import com.example.taskit.ui.model.Task as UiTask
import com.example.taskit.db.model.Task as DbTask
import com.example.taskit.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoomTaskViewModel(
    private val taskDao: TaskDao,
    private val scope: CoroutineScope,
): TaskViewModel {
    private val _isWriting = MutableStateFlow(false)

    override val isWriting: StateFlow<Boolean>
        get() = _isWriting.asStateFlow()

    override fun buildTasksStateFlow(bucket: UiBucket?): StateFlow<List<UiTask>> {
        return if(bucket == null )
            MutableStateFlow(emptyList())
        else
            taskDao.getTasksFlow(bucket.id)
                .map { tasks -> tasks.map { it.toUiTask(bucket) } }
                .onEach {dbTasks -> Log.d("MoveTask Flow change", "List from DB: ${dbTasks.joinToString { it.content }}") }
                .stateIn(scope, SharingStarted.Lazily, emptyList())
    }

    override fun addTask(task: UiTask) {
        launchWriteTask {
            val lastOrder = taskDao.getLastTaskOrder(task.bucket.id) ?: -1
            taskDao.insertTask(task.toDbTask(lastOrder + 1))
        }
    }

    override fun updateTaskContent(taskId: Int, content: String) {
        launchWriteTask {
            taskDao.updateTaskContent(taskId, content)
        }
    }

    override fun updateTaskState(taskId: Int, isChecked: Boolean) {
        launchWriteTask {
            taskDao.updateTaskState(taskId, isChecked)
        }
    }

    override fun deleteTask(taskId: Int) {
        launchWriteTask {
            taskDao.deleteTask(taskId)
        }
    }

    override fun moveTask(taskId: Int, toPos: Int, bucketId: Int) {
        launchWriteTask {
            //fetch all tasks of the bucket
            val dbTasks = taskDao.getTasks(bucketId)
            val task = dbTasks.firstOrNull { it.id == taskId } ?: return@launchWriteTask
            val fromOrder = task.taskOrder
            val toOrder = dbTasks[toPos].taskOrder
            Log.d("MoveTask", "Move ${task.content} to position $toPos")
            Log.d("MoveTask", "List from ViewModel Before: ${dbTasks.joinToString { it.content }}")
            Log.d("MoveTask", "From task ${task.content} ($fromOrder) to task ${dbTasks[toPos].content} ($toOrder)")
            val tasksToUpdate = mutableListOf<DbTask>()

            fun moveDown() {
                for(iTask in dbTasks){
                    val order = iTask.taskOrder
                    if(order == fromOrder)
                        tasksToUpdate += iTask.copy(taskOrder = toOrder )
                    else if(order in fromOrder + 1..toOrder)
                        tasksToUpdate += iTask.copy(taskOrder = order - 1)
                }
            }
            fun moveUp(){
                for(iTask in dbTasks){
                    val order = iTask.taskOrder
                    if(order == fromOrder)
                        tasksToUpdate += iTask.copy(taskOrder = toOrder)
                    else if(order in toOrder..<fromOrder)
                        tasksToUpdate += iTask.copy(taskOrder = order + 1)
                }
            }

            if (fromOrder < toOrder)
                moveDown()
            else if (fromOrder > toOrder)
                moveUp()

            if (tasksToUpdate.isNotEmpty()) {
                taskDao.updateTasks(tasksToUpdate)
                Log.d("MoveTask", "List from ViewModel After: ${taskDao.getTasks(bucketId).joinToString { it.content }}")
            }
            else{
                Log.d("MoveTask", "No change")
            }

        }
    }

    private fun launchWriteTask(block: suspend CoroutineScope.() -> Unit) {
        scope.launch {
            _isWriting.value = true
            block()
            _isWriting.value = false
        }
    }

    private fun DbTask.toUiTask(bucket: UiBucket) = UiTask(
        id = id,
        bucket = bucket,
        parentId = parentId,
        content = content,
        isChecked = isChecked,
    )
    private fun UiTask.toDbTask(order: Int) = DbTask(
        id = id,
        bucketId = bucket.id,
        parentId = parentId,
        taskOrder = order,
        content = content,
        isChecked = isChecked,
    )
}