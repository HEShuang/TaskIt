package com.example.taskit.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskit.db.BucketDao
import com.example.taskit.db.TaskDao
import com.example.taskit.db.model.Bucket as DbBucket
import com.example.taskit.ui.viewmodel.BucketViewModel
import com.example.taskit.ui.model.Bucket as UiBucket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomBucketViewModel(
    private val bucketDao: BucketDao,
): BucketViewModel, ViewModel() {

    override val buckets: StateFlow<List<UiBucket>> = bucketDao.getBucketsFlow()
        .onEach { Log.d("RoomBucketViewModel", "buckets: ${it.size}") }
        .map { buckets -> buckets.map { it.toUiBucket() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    override fun getBucket(bucketId: Int, onComplete: (bucket: UiBucket?) -> Unit){
        viewModelScope.launch {
            val bucket = withContext(Dispatchers.IO){
                bucketDao.getBucket(bucketId)?.toUiBucket()
            }
            withContext(Dispatchers.Main){
                onComplete(bucket)
            }
        }
    }

    override fun addBucket(nTasks: Int, onComplete: (bucket: UiBucket) -> Unit) {
        //Log.d("RoomBucketViewModel", "addBucket: $nTasks")
        viewModelScope.launch {
            val uiBucket = withContext(Dispatchers.IO) {
                val bucket = DbBucket()
                bucket.id = bucketDao.insertBucket(bucket).toInt()
                bucket.toUiBucket()
            }

            withContext(Dispatchers.Main) {
                onComplete(uiBucket)
            }
        }
    }

    override fun updateBucket(bucket: UiBucket) {
        viewModelScope.launch {
            bucketDao.updateBucket(bucket.toDbBucket())
        }
    }

    override fun deleteBucket(bucket: UiBucket) {
        viewModelScope.launch {
            bucketDao.deleteBucket(bucket.id)
        }
    }

    override fun deleteBuckets(bucketIds: List<Int>) {
        viewModelScope.launch {
            bucketDao.deleteBuckets(bucketIds)
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
}