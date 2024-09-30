package com.example.taskit.viewmodel

import android.util.Log
import com.example.taskit.db.TaskDao
import com.example.taskit.ui.model.Task
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoomTaskViewModel(
    private val taskDao: TaskDao,
    private val scope: CoroutineScope,
): TaskViewModel {
    private val _isWriting = MutableStateFlow(false)
    private val taskOnMove = MutableStateFlow(-2)

    override val isWriting: StateFlow<Boolean>
        get() = _isWriting.asStateFlow()

    override fun buildTasksStateFlow(bucket: UiBucket): StateFlow<List<UiTask>> {
        return combine(taskDao.getTasksFlow(bucket.id), taskOnMove) { tasks, movingTaskId ->
                //The tasks retrieved from database are ordered by parentId and taskOrder
                //They need to be reordered by depth first traverse
                val tasksByParent = tasks.groupBy { it.parentId }
                val rootTasks = tasksByParent[-1] ?: emptyList()
                Log.d("Flow change","---------------------------")
                fun depthFirstTraverse(task: DbTask): List<UiTask> {
                    val children = tasksByParent[task.id] ?: emptyList()
                    val isVisible = task.parentId != movingTaskId
                    Log.d("Flow change", "id:${task.id}, parent:${task.parentId}, order:${task.taskOrder}, content:${task.content}")
                    return listOf(task.toUiTask(bucket, isVisible)) + children.flatMap { childTask -> depthFirstTraverse(childTask) }
                }
                //transform each root task to a list of tasks, then flat map them all to a single list
                rootTasks.flatMap { rootTask -> depthFirstTraverse(rootTask) }
            }
            .stateIn(scope, SharingStarted.Lazily, emptyList())
    }

    override fun addTask(task: UiTask) {
        launchWriteTask {
            val lastOrder = taskDao.getLastTaskOrder(task.bucket.id, -1) ?: -1
            taskDao.insertTask(task.toDbTask(-1,lastOrder + 1))
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

    override fun reorderTask(taskId: Int, toTaskId: Int) {
        launchWriteTask {
            val fromTask = taskDao.getTask(taskId) ?: return@launchWriteTask
            val toTask = taskDao.getTask(toTaskId) ?: return@launchWriteTask

            val fromOrder = fromTask.taskOrder
            val toOrder = toTask.taskOrder
            val fromParentId = fromTask.parentId
            val toParentId = toTask.parentId
            if(fromParentId == toParentId && fromOrder == toOrder) return@launchWriteTask

            val bucketId = fromTask.bucketId
            val tasksToUpdate = mutableListOf<DbTask>()

            //Reordering under the same parent
            if(fromParentId == toParentId) {
                tasksToUpdate += fromTask.copy(taskOrder = toOrder)
                val isMoveDown = fromOrder < toOrder
                val toParentChildren = taskDao.getTasks(bucketId, toParentId)
                for(iTask in toParentChildren){
                    //If moving down, the tasks in between will decrease their order
                    if (isMoveDown && iTask.taskOrder in fromOrder + 1..toOrder){
                        tasksToUpdate += iTask.copy(taskOrder = iTask.taskOrder - 1)
                    }
                    //If moving up, the tasks in between will increase their order
                    else if(iTask.taskOrder in toOrder..<fromOrder)
                        tasksToUpdate += iTask.copy(taskOrder = iTask.taskOrder + 1)
                }
            }
            //Move to new parent
            else{
                //Determine moving down or up
                val compareFromOrder = if(fromParentId < 0 ) fromOrder else {
                    taskDao.getTask(fromParentId)?.taskOrder ?: -1
                }
                val compareToOrder = if(toParentId < 0) toOrder else {
                    taskDao.getTask(toParentId)?.taskOrder ?: -1
                }
                //If moving up, insert the task before the destination task
                //Increase taskOrder of destination task and its siblings below
                if(compareFromOrder >= compareToOrder) {//Moving Up
                    tasksToUpdate += fromTask.copy(parentId = toParentId, taskOrder = toOrder)

                    val toParentChildren = taskDao.getTasks(bucketId, toParentId)
                    for(iTask in toParentChildren){
                        if(iTask.taskOrder >= toOrder){
                            tasksToUpdate += iTask.copy(taskOrder = iTask.taskOrder + 1)
                        }
                    }

                }
                //Moving down
                else{
                    //If the destination task has children, insert the task as it's first child
                    //Increase taskOrder of the original children
                    val destTaskChildren = taskDao.getTasks(bucketId, toTaskId)
                    if(destTaskChildren.isNotEmpty()){
                        tasksToUpdate += fromTask.copy(parentId = toTaskId, taskOrder = 0)
                        for(iTask in destTaskChildren){
                            tasksToUpdate += iTask.copy(taskOrder = iTask.taskOrder + 1)
                        }
                    }
                    //Otherwise, insert the task below the destination task
                    //Increase taskOrder of the siblings below
                    else {
                        tasksToUpdate += fromTask.copy(parentId = toParentId, taskOrder = toOrder + 1)
                        val toParentChildren = taskDao.getTasks(bucketId, toParentId)
                        for(iTask in toParentChildren){
                            if(iTask.taskOrder > toOrder){
                                tasksToUpdate += iTask.copy(taskOrder = iTask.taskOrder + 1)
                            }
                        }
                    }
                }
            }
            if(tasksToUpdate.isNotEmpty())
                taskDao.updateTasks(tasksToUpdate)
        }
    }

    override fun moveTaskToRoot(taskId: Int) {
        launchWriteTask {
            val task = taskDao.getTask(taskId) ?: return@launchWriteTask
            val parentTask = taskDao.getTask(task.parentId) ?: return@launchWriteTask

            //Example: move C to root
            //Change D's parent to C
            //Increase the taskOrder of E and F
            // A           A
            // -B          -B
            // -C          C
            // -D          -D
            // E           E
            // F           F
            val tasksToUpdate = mutableListOf<DbTask>()

            //Change the parent of its siblings below
            val children = taskDao.getTasks(task.bucketId, parentTask.id)
            for(iTask in children){
                if(iTask.taskOrder > task.taskOrder){
                    tasksToUpdate += iTask.copy(parentId = task.id)
                }
            }

            //Insert the task below its old parent
            tasksToUpdate += task.copy(parentId = -1, taskOrder = parentTask.taskOrder + 1)

            //Move down the root tasks that below its old parent
            val rootTasks = taskDao.getTasks(task.bucketId, - 1)
            for(iTask in rootTasks){
                if(iTask.taskOrder > parentTask.taskOrder)
                    tasksToUpdate += iTask.copy(taskOrder = iTask.taskOrder + 1)
            }
            taskDao.updateTasks(tasksToUpdate)

        }
    }

    override fun moveTaskToChild(taskId: Int, taskAboveId: Int) {
        launchWriteTask {
            val task = taskDao.getTask(taskId) ?: return@launchWriteTask
            val taskAbove = taskDao.getTask(taskAboveId) ?: return@launchWriteTask

            var newParentId = taskAbove.id
            var newOrder = 0
            if(taskAbove.parentId >= 0){ //If the task above is a child task
                newParentId = taskAbove.parentId
                newOrder = taskAbove.taskOrder + 1
            }

            //If the target task has children, append them to the new parent as well
            val children = taskDao.getTasks(task.bucketId, task.id)
            if(children.isNotEmpty()){
                val tasksToUpdate = mutableListOf(task.copy(parentId = newParentId, taskOrder = newOrder))
                for(child in children)
                    tasksToUpdate += child.copy(parentId = newParentId, taskOrder = ++newOrder)
                taskDao.updateTasks(tasksToUpdate)
            }
            else {
                taskDao.updateTask(task.copy(parentId = newParentId, taskOrder = newOrder))
            }

        }
    }

    override fun onReorderStart(taskId: Int) {
        taskOnMove.value = taskId
    }

    override fun onReorderEnd(taskId: Int) {
        if (taskOnMove.value == taskId) {
            taskOnMove.value = -2
        }
    }

    private fun launchWriteTask(block: suspend CoroutineScope.() -> Unit) {
        scope.launch {
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
    private fun UiTask.toDbTask(parentId: Int, taskOrder: Int) = DbTask(
        id = id,
        bucketId = bucket.id,
        parentId = parentId,
        taskOrder = taskOrder,
        content = content,
        isChecked = isChecked,
    )
}