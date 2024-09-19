package com.example.taskit.viewmodel

import com.example.taskit.db.TaskDao
import com.example.taskit.ui.model.Bucket
import com.example.taskit.ui.model.Task
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

    override fun buildTasksStateFlow(bucket: Bucket?): StateFlow<List<Task>> {
        return if(bucket == null )
            MutableStateFlow(emptyList())
        else
            taskDao.getTasks(bucket.id)
                .map { tasks -> tasks.map { it.toUiTask(bucket) } }
                .stateIn(scope, SharingStarted.Lazily, emptyList())
    }

    override fun addTask(task: Task) {
        scope.launch {
            taskDao.insertTask(task.toDbTask())
        }
    }

    override fun updateTask(task: Task) {
        scope.launch {
            taskDao.updateTask(task.toDbTask())
        }
    }

    override fun deleteTask(task: Task) {
        scope.launch {
            taskDao.deleteTask(task.toDbTask())
        }
    }

    override fun moveTask(taskToMove: Task, toIndex: Int) {
        TODO("Not yet implemented")
        /*val bucket = taskToMove.bucket
        val tasksToUpdate = mutableListOf<Task>()
        val newTasks = bucket.tasks.toMutableList()

        fun moveDown(){

            for(i in taskToMove.index..toIndex){

                if(i == taskToMove.index){
                    val newTask = taskToMove.copy(index = toIndex)
                    tasksToUpdate += newTask
                    newTasks[toIndex] = newTask
                }
                else
                {
                    val oldTask = bucket.tasks[i]
                    val newTask = oldTask.copy(index = oldTask.index - 1)
                    tasksToUpdate += newTask
                    newTasks[i-1] = newTask
                }
            }
        }

        fun moveUp(){

            for(i in toIndex..taskToMove.index){

                if(i == taskToMove.index){
                    val newTask = taskToMove.copy(index = toIndex)
                    tasksToUpdate += newTask
                    newTasks[toIndex] = newTask
                }
                else{
                    val oldTask = bucket.tasks[i]
                    val newTask = oldTask.copy(index = oldTask.index + 1)
                    tasksToUpdate += newTask
                    newTasks[i+1] = newTask
                }
            }
        }

        if(taskToMove.index == toIndex)
            return
        if(taskToMove.index < toIndex)
            moveDown()
        else
            moveUp()

        if(tasksToUpdate.isNotEmpty()) {
            bucket.tasks = newTasks
            scope.launch {
                for (task in tasksToUpdate)
                    taskDao.updateTask(task.toDbTask())
            }
        }*/
    }

    override fun renameBucket() {
        TODO("Not yet implemented")
    }

    private fun DbTask.toUiTask(bucket: Bucket) = UiTask(
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