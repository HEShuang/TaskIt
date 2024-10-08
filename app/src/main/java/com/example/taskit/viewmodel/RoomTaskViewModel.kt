package com.example.taskit.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoomTaskViewModel(
    private val taskDao: TaskDao,
): TaskViewModel, ViewModel() {
    private val _isWriting = MutableStateFlow(false)
    private val taskOnMove = MutableStateFlow(-2)

    override val isWriting: StateFlow<Boolean>
        get() = _isWriting.asStateFlow()

    override fun buildTasksStateFlow(bucket: UiBucket): StateFlow<List<UiTask>> {
        Log.d("ViewModel", "buildTasksStateFlow")
        return combine(taskDao.getTasksFlow(bucket.id), taskOnMove) { tasks, movingTaskId ->
                //The tasks retrieved from database are ordered by parentId and taskOrder
                //They need to be reordered by depth first traverse
                val tasksByParent = tasks.groupBy { it.parentId }
                val rootTasks = tasksByParent[-1] ?: emptyList()
                Log.d("Flow change","---------------------------")
                fun depthFirstTraverse(task: DbTask): List<UiTask> {
                    val children = tasksByParent[task.id] ?: emptyList()
                    //Hide the children of the task on move
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
            val lastOrder = taskDao.getLastTaskOrder(bucketId, -1) ?: -1
            taskDao.insertTask(DbTask(bucketId = bucketId, parentId = -1, taskOrder = lastOrder + 1))
        }
    }

    override fun insertTask(taskAboveId: Int) {
        launchWriteTask {
            val taskAbove = taskDao.getTask(taskAboveId) ?: return@launchWriteTask
            val taskAboveChildren = taskDao.getTasks(taskAbove.bucketId, taskAbove.id)

            //If taskAbove has children, insert new task as his first child
            //Increase taskOrder of original children
            if(taskAboveChildren.isNotEmpty()){
                val newTask = DbTask(
                    bucketId = taskAbove.bucketId,
                    parentId = taskAboveId,
                )
                taskDao.upsertTasks(taskAboveChildren.map { task -> task.copy(taskOrder = task.taskOrder + 1) }, newTask)
            }
            //Otherwise, insert task after it and increase taskOrder of its siblings below
            else{
                val newTask = DbTask(
                    bucketId = taskAbove.bucketId,
                    parentId = taskAbove.parentId,
                    taskOrder = taskAbove.taskOrder + 1
                )
                val tasksToUpdate = mutableListOf<DbTask>()
                val siblings = taskDao.getTasks(taskAbove.bucketId, taskAbove.parentId)
                for(iTask in siblings){
                    if(iTask.taskOrder > taskAbove.taskOrder){
                        tasksToUpdate += iTask.copy(taskOrder = iTask.taskOrder + 1)
                    }
                }
                taskDao.upsertTasks(tasksToUpdate, newTask)
            }

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

    override fun reorderTask(fromTaskId: Int, toTaskId: Int) {
        launchWriteTask {
            val fromTask = taskDao.getTask(fromTaskId) ?: return@launchWriteTask
            val toTask = taskDao.getTask(toTaskId) ?: return@launchWriteTask

            Log.d("MoveTask", "from Task ${fromTask.content} to Task ${toTask.content}")

            val fromOrder = fromTask.taskOrder
            val toOrder = toTask.taskOrder
            val fromParentId = fromTask.parentId
            val toParentId = toTask.parentId

            if(fromParentId == toParentId && fromOrder == toOrder) {
                Log.d("MoveTask", "Do nothing: the same task")
                return@launchWriteTask
            }
            if(fromTaskId == toParentId){
                Log.d("MoveTask", "Do nothing: move to its own child")
                return@launchWriteTask
            }

            val bucketId = fromTask.bucketId
            val tasksToUpdate = mutableListOf<DbTask>()

            //Reordering under the same parent
            if(fromParentId == toParentId) {
                tasksToUpdate += fromTask.copy(taskOrder = toOrder)
                val isMoveDown = fromOrder < toOrder
                val toParentChildren = taskDao.getTasks(bucketId, toParentId)
                for(iTask in toParentChildren){
                    //If moving down, decrease taskOrder of the tasks in between
                    if (isMoveDown && iTask.taskOrder in fromOrder + 1..toOrder){
                        tasksToUpdate += iTask.copy(taskOrder = iTask.taskOrder - 1)
                    }
                    //If moving up, increase taskOrder of the tasks in between
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
                val fromTaskChildren = taskDao.getTasks(bucketId, fromTaskId)
                //If moving up, insert the task and its children before the destination task
                //Increase taskOrder of destination task and its siblings below
                if(compareFromOrder >= compareToOrder) {//Moving Up
                    var newOrder = toOrder
                    tasksToUpdate += fromTask.copy(parentId = toParentId, taskOrder = newOrder++)
                    for(iTask in fromTaskChildren){
                        tasksToUpdate += iTask.copy(parentId = toParentId, taskOrder = newOrder++)
                    }
                    val toParentChildren = taskDao.getTasks(bucketId, toParentId)
                    for(iTask in toParentChildren){
                        if(iTask.taskOrder >= toOrder){
                            tasksToUpdate += iTask.copy(taskOrder = newOrder++)
                        }
                    }

                }
                else{
                    //If the destination task has children, insert the task and its children on top of destination task's children
                    //Otherwise, insert the task and its children below the destination task
                    val toTaskChildren = taskDao.getTasks(bucketId, toTaskId)
                    if(toTaskChildren.isNotEmpty()){
                        var newOrder = 0
                        tasksToUpdate += fromTask.copy(parentId = toTaskId, taskOrder = newOrder++)
                        for(iTask in fromTaskChildren){
                            tasksToUpdate += iTask.copy(parentId = toTaskId, taskOrder = newOrder++)
                        }
                        //Increase taskOrder of the destination task's original children
                        for(iTask in toTaskChildren){
                            tasksToUpdate += iTask.copy(taskOrder = newOrder++)
                        }
                    }
                    else {
                        var newOrder = toOrder + 1
                        tasksToUpdate += fromTask.copy(parentId = toParentId, taskOrder = newOrder++)
                        for(iTask in fromTaskChildren){
                            tasksToUpdate += iTask.copy(parentId = toParentId, taskOrder = newOrder++)
                        }
                        //Increase taskOrder of the destination task's siblings below
                        val toParentChildren = taskDao.getTasks(bucketId, toParentId)
                        for(iTask in toParentChildren){
                            if(iTask.taskOrder > toOrder){
                                tasksToUpdate += iTask.copy(taskOrder = newOrder++)
                            }
                        }
                    }
                }
            }
            if(tasksToUpdate.isNotEmpty())
                taskDao.updateTasks(tasksToUpdate)
        }
    }

    //Example: move C to root
    //Change D's parent to C
    //Increase the taskOrder of E and F
    // A           A
    // -B          -B
    // -C          C
    // -D          -D
    // E           E
    // F           F
    override fun moveTaskToRoot(taskId: Int) {
        launchWriteTask {
            val task = taskDao.getTask(taskId) ?: return@launchWriteTask
            Log.d("MoveTask", "Move task to root: $task")
            if(task.parentId < 0){
                Log.d("MoveTask", "Do nothing: it is already a root task")
                return@launchWriteTask
            }
            val parentTask = taskDao.getTask(task.parentId) ?: return@launchWriteTask

            val tasksToUpdate = mutableListOf<DbTask>()
            //The target task becomes the parent of its siblings below
            val children = taskDao.getTasks(task.bucketId, parentTask.id)
            for(iTask in children){
                if(iTask.taskOrder > task.taskOrder){
                    tasksToUpdate += iTask.copy(parentId = task.id)
                }
            }

            //Move the task to root below its old parent
            tasksToUpdate += task.copy(parentId = -1, taskOrder = parentTask.taskOrder + 1)

            //Increase the taskOrder of the root tasks that below its old parent
            val rootTasks = taskDao.getTasks(task.bucketId, - 1)
            for(iTask in rootTasks){
                if(iTask.taskOrder > parentTask.taskOrder)
                    tasksToUpdate += iTask.copy(taskOrder = iTask.taskOrder + 1)
            }
            taskDao.updateTasks(tasksToUpdate)

        }
    }

    //Example: move C to child
    //Case 1 the task above B is a root task:
    //Task C and D become B's child
    // A           A
    // B           B
    // C           -C
    // -D          -D
    // E           E
    //Case 2 the task above B is also a child task:
    //Task C and D become B's sibling,
    // A           A
    // -B          -B
    // C           -C
    // -D          -D
    // E           E
    override fun moveTaskToChild(taskId: Int, taskAboveId: Int) {
        launchWriteTask {
            val task = taskDao.getTask(taskId) ?: return@launchWriteTask
            Log.d("MoveTask", "move task to child: $task")
            if(task.parentId >= 0) {
                Log.d("MoveTask", "Do nothing: it is already a child")
                return@launchWriteTask
            }

            //If the task above is a root task, add the target task as its child
            //Otherwise, add the target task as its sibling
            val taskAbove = taskDao.getTask(taskAboveId) ?: return@launchWriteTask
            val taskAboveIsRoot = taskAbove.parentId < 0
            val newParentId = if (taskAboveIsRoot) taskAbove.id else taskAbove.parentId
            var newOrder = if (taskAboveIsRoot) 0 else taskAbove.taskOrder + 1

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
    private fun UiTask.toDbTask(parentId: Int, taskOrder: Int) = DbTask(
        id = id,
        bucketId = bucket.id,
        parentId = parentId,
        taskOrder = taskOrder,
        content = content,
        isChecked = isChecked,
    )
}