package com.example.taskit.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskit.db.AppRepository
import com.example.taskit.db.model.Task as DbTask
import com.example.taskit.db.model.Bucket as DbBucket
import com.example.taskit.ui.model.Task as UiTask
import com.example.taskit.ui.model.Bucket as UiBucket
import com.example.taskit.ui.viewmodel.HomeScreenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomHomeScreenViewModel(
   private val repo: AppRepository
): HomeScreenViewModel, ViewModel(){

    override val buckets: StateFlow<List<UiBucket>> = repo.getBucketsFlow()
        .map { buckets -> buckets.map { it.toUiBucket() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    override fun buildTasksStateFlow(bucket: UiBucket): StateFlow<List<UiTask>> {
         return repo.getTasksFlow(bucket.id).map {tasks ->
            //The tasks retrieved from database are ordered by parentId and taskOrder
            //They need to be reordered by depth first traverse
            val tasksByParent = tasks.groupBy { it.parentId }
            val rootTasks = tasksByParent[-1] ?: emptyList()
            Log.d("Flow change","---------------------------")
            fun depthFirstTraverse(task: DbTask): List<UiTask> {
                val children = tasksByParent[task.id] ?: emptyList()
                return listOf(task.toUiTask(bucket, true)) + children.flatMap { childTask -> depthFirstTraverse(childTask) }
            }
            //transform each root task to a list of tasks, then flat map them all to a single list
            rootTasks.flatMap { rootTask -> depthFirstTraverse(rootTask) }
         }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }
    override fun getBucket(bucketId: Int, onComplete: (bucket: UiBucket?) -> Unit) {
        viewModelScope.launch {
            val bucket = withContext(Dispatchers.IO){
                repo.getBucket(bucketId)?.toUiBucket()
            }
            withContext(Dispatchers.Main){
                onComplete(bucket)
            }
        }
    }

    override fun addBucket(nTasks: Int, onComplete: (bucket: UiBucket) -> Unit) {
        viewModelScope.launch {
            val uiBucket = withContext(Dispatchers.IO) {
                val bucket = DbBucket()
                bucket.id = repo.insertBucket(bucket).toInt()
                bucket.toUiBucket()
            }

            withContext(Dispatchers.Main) {
                onComplete(uiBucket)
            }
        }
    }

    override fun updateBucket(bucket: UiBucket) {
        viewModelScope.launch {
            repo.updateBucket(bucket.toDbBucket())
        }
    }

    override fun deleteBucket(bucket: UiBucket) {
        viewModelScope.launch {
            repo.deleteBucket(bucket.id)
        }
    }

    override fun deleteBuckets(bucketIds: List<Int>) {
        viewModelScope.launch {
            repo.deleteBuckets(bucketIds)
        }
    }

    private fun DbBucket.toUiBucket() = UiBucket(
        id = id,
        name = name,
    )

    private fun UiBucket.toDbBucket() = DbBucket(
        id = id,
        name = name,
    )

    private fun DbTask.toUiTask(bucket: UiBucket, isVisible: Boolean) = UiTask(
        id = id,
        bucket = bucket,
        content = content,
        isChecked = isChecked,
        isChild = parentId >= 0,
        isVisible = isVisible,
    )
}