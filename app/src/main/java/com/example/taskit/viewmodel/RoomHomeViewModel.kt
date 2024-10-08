package com.example.taskit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskit.db.AppRepository
import com.example.taskit.ui.ext.toDbBucket
import com.example.taskit.ui.ext.toUiBucket
import com.example.taskit.ui.ext.toUiTask
import com.example.taskit.db.model.Bucket as DbBucket
import com.example.taskit.ui.model.Task as UiTask
import com.example.taskit.ui.model.Bucket as UiBucket
import com.example.taskit.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomHomeViewModel(
   private val repo: AppRepository
): HomeViewModel, ViewModel(){

    override val buckets: StateFlow<List<UiBucket>> = repo.getBucketsFlow()
        .map { buckets -> buckets.map { it.toUiBucket() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    override fun buildTasksStateFlow(bucket: UiBucket): StateFlow<List<UiTask>> {
        return repo.getTasksFlow(bucket.id).map {tasks ->
            tasks.map { task ->
                task.toUiTask(bucket, true)
            }
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
}