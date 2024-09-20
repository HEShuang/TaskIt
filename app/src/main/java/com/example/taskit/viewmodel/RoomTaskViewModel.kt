package com.example.taskit.viewmodel

import android.util.Log
import com.example.taskit.db.TaskDao
import com.example.taskit.db.model.Task
import com.example.taskit.ui.model.Bucket as UiBucket
import com.example.taskit.ui.model.Task as UiTask
import com.example.taskit.db.model.Task as DbTask
import com.example.taskit.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoomTaskViewModel(
    private val taskDao: TaskDao,
    private val scope: CoroutineScope,
): TaskViewModel {

    override fun buildTasksStateFlow(bucket: UiBucket?): StateFlow<List<UiTask>> {
        return if(bucket == null )
            MutableStateFlow(emptyList())
        else
            taskDao.getTasksFlow(bucket.id)
                .map { tasks -> tasks.map { it.toUiTask(bucket) } }
                .stateIn(scope, SharingStarted.Lazily, emptyList())
    }

    override fun addTask(task: UiTask) {
        scope.launch {
            taskDao.insertTask(task.toDbTask())
        }
    }

    override fun updateTask(task: UiTask) {
        scope.launch {
            taskDao.updateTask(task.toDbTask())
        }
    }

    override fun deleteTask(task: UiTask) {
        scope.launch {
            taskDao.deleteTask(task.toDbTask())
        }
    }

    override fun moveTask(fromIndex: Int, toIndex: Int, bucketId: Int) {
        scope.launch {
            //fetch all tasks of the bucket
            val tasks = taskDao.getTasks(bucketId)
            val tasksToUpdate = mutableListOf<DbTask>()

            fun moveDown() {
                for(task in tasks){
                    val index = task.taskOrder
                    if(index == fromIndex)
                        tasksToUpdate += task.copy(taskOrder = toIndex)
                    else  if(index in fromIndex + 1..toIndex)
                        tasksToUpdate += task.copy(taskOrder = index - 1)
                }
            }

            fun moveUp() {
                for(task in tasks){
                    val index = task.taskOrder
                    if(index == fromIndex)
                        tasksToUpdate += task.copy(taskOrder = toIndex)
                    else if(index in toIndex..<fromIndex){
                        tasksToUpdate += task.copy(taskOrder = index + 1)
                    }
                }
            }

            if (fromIndex < toIndex)
                moveDown()
            else if (fromIndex > toIndex)
                moveUp()

            if (tasksToUpdate.isNotEmpty()) {
                taskDao.updateTasks(tasksToUpdate)
            }
        }
    }

    override fun renameBucket() {
        TODO("Not yet implemented")
    }

    private fun DbTask.toUiTask(bucket: UiBucket) = UiTask(
        id = id,
        bucket = bucket,
        parentId = parentId,
        index = taskOrder,
        content = content,
        isChecked = isChecked,
    )
    private fun UiTask.toDbTask() = DbTask(
        id = id,
        bucketId = bucket.id,
        parentId = parentId,
        taskOrder = index,
        content = content,
        isChecked = isChecked,
    )
}