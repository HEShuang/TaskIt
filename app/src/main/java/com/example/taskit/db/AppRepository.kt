package com.example.taskit.db

import android.util.Log
import androidx.room.withTransaction
import com.example.taskit.db.model.Bucket
import com.example.taskit.db.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppRepository(
    val db: AppDatabase
) {
    fun getBucketsFlow(): Flow<List<Bucket>> {
        return db.bucketDao.getBucketsFlow()
    }

    suspend fun getBucket(bucketId: Int): Bucket? {
        return db.bucketDao.getBucket(bucketId)
    }

    suspend fun insertBucket(bucket: Bucket): Long {
        return db.bucketDao.insertBucket(bucket)
    }

    suspend fun updateBucket(bucket: Bucket): Unit {
        db.bucketDao.updateBucket(bucket)
    }

    suspend fun deleteBucket(bucketId: Int): Unit {
        db.withTransaction {
            db.bucketDao.deleteBucket(bucketId)
            db.taskDao.deleteTasks(bucketId)
        }
    }

    suspend fun deleteBuckets(bucketIds: List<Int>): Unit {
        db.withTransaction {
            for (id in bucketIds)
                db.bucketDao.deleteBucket(id)
        }
    }

    fun getTasksFlow(bucketId: Int): Flow<List<Task>> {
        //The tasks retrieved from database are ordered by parentId and taskOrder
        //They need to be reordered by depth first traverse
        return db.taskDao.getTasksFlow(bucketId).map { tasks ->
            val tasksByParent = tasks.groupBy { it.parentId }
            val rootTasks = tasksByParent[-1] ?: emptyList()
            fun depthFirstTraverse(task: Task): List<Task> {
                val children = tasksByParent[task.id] ?: emptyList()
                return listOf(task) + children.flatMap { childTask -> depthFirstTraverse(childTask) }
            }
            //transform each root task to a list of tasks, then flat map them all to a single list
            rootTasks.flatMap { rootTask -> depthFirstTraverse(rootTask) }
        }
    }

    /**
     * Append a task as the last child of its parent.
     * @param task The input task's taskOrder will be ignored and recalculated.
     */
    suspend fun appendTask(task: Task): Long {
        val lastOrder = db.taskDao.getLastTaskOrder(task.bucketId, task.parentId) ?: -1
        return db.taskDao.insertTask(task.copy(taskOrder = lastOrder + 1))
    }

    /**
     * Insert a task under another task.
     * Depending on whether the task above has children, the task will be insert as its child or sibling.
     * @param task The input task's bucketId, parentId, and taskOrder will be ignored and recalculated.
     * @param taskAboveId The id of the task above.
     */
    suspend fun insertTask(task: Task, taskAboveId: Int) {
        val taskAbove = db.taskDao.getTask(taskAboveId) ?: return
        val taskAboveChildren = db.taskDao.getTasks(taskAbove.bucketId, taskAbove.id)
        lateinit var newTask: Task
        val tasksToUpdate = mutableListOf<Task>()

        //If taskAbove has children, insert new task as his first child
        //Increase taskOrder of original children
        if (taskAboveChildren.isNotEmpty()) {
            newTask = task.copy(
                bucketId = taskAbove.bucketId,
                parentId = taskAboveId,
                taskOrder = 0,
            )
            tasksToUpdate += taskAboveChildren.map { iTask -> iTask.copy(taskOrder = iTask.taskOrder + 1) }
        }
        //Otherwise, insert task after it and increase taskOrder of its siblings below
        else {
            newTask = task.copy(
                bucketId = taskAbove.bucketId,
                parentId = taskAbove.parentId,
                taskOrder = taskAbove.taskOrder + 1
            )
            val siblings = db.taskDao.getTasks(taskAbove.bucketId, taskAbove.parentId)
            for (iTask in siblings) {
                if (iTask.taskOrder > taskAbove.taskOrder) {
                    tasksToUpdate += iTask.copy(taskOrder = iTask.taskOrder + 1)
                }
            }
        }
        db.withTransaction {
            db.taskDao.insertTask(newTask)
            db.taskDao.updateTasks(tasksToUpdate)
        }
    }

    suspend fun updateTaskContent(taskId: Int, content: String) {
        db.taskDao.updateTaskContent(taskId, content)
    }

    suspend fun updateTaskState(taskId: Int, isChecked: Boolean) {
        db.taskDao.updateTaskState(taskId, isChecked)
    }

    suspend fun deleteTask(taskId: Int) {
        db.taskDao.deleteTask(taskId)
    }

    suspend fun reorderTask(fromTaskId: Int, toTaskId: Int) {
        val fromTask = db.taskDao.getTask(fromTaskId) ?: return
        val toTask = db.taskDao.getTask(toTaskId) ?: return

        Log.d("MoveTask", "from Task ${fromTask.content} to Task ${toTask.content}")

        val fromOrder = fromTask.taskOrder
        val toOrder = toTask.taskOrder
        val fromParentId = fromTask.parentId
        val toParentId = toTask.parentId

        if (fromParentId == toParentId && fromOrder == toOrder) {
            Log.d("MoveTask", "Do nothing: the same task")
            return
        }
        if (fromTaskId == toParentId) {
            Log.d("MoveTask", "Do nothing: move to its own child")
            return
        }

        val bucketId = fromTask.bucketId
        val tasksToUpdate = mutableListOf<Task>()

        //Reordering under the same parent
        if (fromParentId == toParentId) {
            tasksToUpdate += fromTask.copy(taskOrder = toOrder)
            val isMoveDown = fromOrder < toOrder
            val toParentChildren = db.taskDao.getTasks(bucketId, toParentId)
            for (iTask in toParentChildren) {
                //If moving down, decrease taskOrder of the tasks in between
                if (isMoveDown && iTask.taskOrder in fromOrder + 1..toOrder) {
                    tasksToUpdate += iTask.copy(taskOrder = iTask.taskOrder - 1)
                }
                //If moving up, increase taskOrder of the tasks in between
                else if (iTask.taskOrder in toOrder..<fromOrder)
                    tasksToUpdate += iTask.copy(taskOrder = iTask.taskOrder + 1)
            }
        }
        //Move to new parent
        else {
            //Determine moving down or up
            val compareFromOrder = if (fromParentId < 0) fromOrder else {
                db.taskDao.getTask(fromParentId)?.taskOrder ?: -1
            }
            val compareToOrder = if (toParentId < 0) toOrder else {
                db.taskDao.getTask(toParentId)?.taskOrder ?: -1
            }
            val fromTaskChildren = db.taskDao.getTasks(bucketId, fromTaskId)
            //If moving up, insert the task and its children before the destination task
            //Increase taskOrder of destination task and its siblings below
            if (compareFromOrder >= compareToOrder) {//Moving Up
                var newOrder = toOrder
                tasksToUpdate += fromTask.copy(parentId = toParentId, taskOrder = newOrder++)
                for (iTask in fromTaskChildren) {
                    tasksToUpdate += iTask.copy(parentId = toParentId, taskOrder = newOrder++)
                }
                val toParentChildren = db.taskDao.getTasks(bucketId, toParentId)
                for (iTask in toParentChildren) {
                    if (iTask.taskOrder >= toOrder) {
                        tasksToUpdate += iTask.copy(taskOrder = newOrder++)
                    }
                }

            } else {
                //If the destination task has children, insert the task and its children on top of destination task's children
                //Otherwise, insert the task and its children below the destination task
                val toTaskChildren = db.taskDao.getTasks(bucketId, toTaskId)
                if (toTaskChildren.isNotEmpty()) {
                    var newOrder = 0
                    tasksToUpdate += fromTask.copy(parentId = toTaskId, taskOrder = newOrder++)
                    for (iTask in fromTaskChildren) {
                        tasksToUpdate += iTask.copy(parentId = toTaskId, taskOrder = newOrder++)
                    }
                    //Increase taskOrder of the destination task's original children
                    for (iTask in toTaskChildren) {
                        tasksToUpdate += iTask.copy(taskOrder = newOrder++)
                    }
                } else {
                    var newOrder = toOrder + 1
                    tasksToUpdate += fromTask.copy(parentId = toParentId, taskOrder = newOrder++)
                    for (iTask in fromTaskChildren) {
                        tasksToUpdate += iTask.copy(parentId = toParentId, taskOrder = newOrder++)
                    }
                    //Increase taskOrder of the destination task's siblings below
                    val toParentChildren = db.taskDao.getTasks(bucketId, toParentId)
                    for (iTask in toParentChildren) {
                        if (iTask.taskOrder > toOrder) {
                            tasksToUpdate += iTask.copy(taskOrder = newOrder++)
                        }
                    }
                }
            }
        }
        if (tasksToUpdate.isNotEmpty())
            db.taskDao.updateTasks(tasksToUpdate)
    }

    /**
     * Change a subtask to a root task
     * Example: move C to root
     * Change D's parent to C
     * Increase the taskOrder of E and F
     * A           A
     * -B          -B
     * -C          C
     * -D          -D
     * E           E
     * F           F
     */
    suspend fun toRootTask(taskId: Int) {
        val task = db.taskDao.getTask(taskId) ?: return
        Log.d("MoveTask", "Move task to root: $task")
        if (task.parentId < 0) {
            Log.d("MoveTask", "Do nothing: it is already a root task")
            return
        }
        val parentTask = db.taskDao.getTask(task.parentId) ?: return

        val tasksToUpdate = mutableListOf<Task>()
        //The target task becomes the parent of its siblings below
        val children = db.taskDao.getTasks(task.bucketId, parentTask.id)
        for (iTask in children) {
            if (iTask.taskOrder > task.taskOrder) {
                tasksToUpdate += iTask.copy(parentId = task.id)
            }
        }

        //Move the task to root below its old parent
        tasksToUpdate += task.copy(parentId = -1, taskOrder = parentTask.taskOrder + 1)

        //Increase the taskOrder of the root tasks that below its old parent
        val rootTasks = db.taskDao.getTasks(task.bucketId, -1)
        for (iTask in rootTasks) {
            if (iTask.taskOrder > parentTask.taskOrder)
                tasksToUpdate += iTask.copy(taskOrder = iTask.taskOrder + 1)
        }
        db.taskDao.updateTasks(tasksToUpdate)
    }

    /**
     * Change a root task to subtask
     * Example: move C to child
     * Case 1 the task above B is a root task:
     * Task C and D become B's child
     * A           A
     * B           B
     * C           -C
     * -D          -D
     * E           E
     *
     * Case 2 the task above B is also a child task:
     * Task C and D become B's sibling,
     * A           A
     * -B          -B
     * C           -C
     * -D          -D
     * E           E
     */
    suspend fun toSubtask(taskId: Int, taskAboveId: Int) {
        val task = db.taskDao.getTask(taskId) ?: return
        Log.d("MoveTask", "move task to child: $task")
        if (task.parentId >= 0) {
            Log.d("MoveTask", "Do nothing: it is already a child")
            return
        }

        //If the task above is a root task, add the target task as its child
        //Otherwise, add the target task as its sibling
        val taskAbove = db.taskDao.getTask(taskAboveId) ?: return
        val taskAboveIsRoot = taskAbove.parentId < 0
        val newParentId = if (taskAboveIsRoot) taskAbove.id else taskAbove.parentId
        var newOrder = if (taskAboveIsRoot) 0 else taskAbove.taskOrder + 1

        //If the target task has children, append them to the new parent as well
        val children = db.taskDao.getTasks(task.bucketId, task.id)
        if (children.isNotEmpty()) {
            val tasksToUpdate =
                mutableListOf(task.copy(parentId = newParentId, taskOrder = newOrder))
            for (child in children)
                tasksToUpdate += child.copy(parentId = newParentId, taskOrder = ++newOrder)
            db.taskDao.updateTasks(tasksToUpdate)
        } else {
            db.taskDao.updateTask(task.copy(parentId = newParentId, taskOrder = newOrder))
        }
    }
}