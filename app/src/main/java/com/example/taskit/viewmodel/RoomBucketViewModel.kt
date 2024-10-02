package com.example.taskit.viewmodel

import android.util.Log
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
    private val scope: CoroutineScope,
    ): BucketViewModel {

    override val buckets: StateFlow<List<UiBucket>> = bucketDao.getAllBuckets()
        .onEach { Log.d("RoomBucketViewModel", "buckets: ${it.size}") }
        .map { buckets -> buckets.map { it.toUiBucket() } }
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    override fun getBucket(bucketId: Int, onComplete: (bucket: UiBucket?) -> Unit){
        scope.launch {
            val bucket = withContext(Dispatchers.IO){
                bucketDao.getBucketById(bucketId)?.toUiBucket()
            }
            withContext(Dispatchers.Main){
                onComplete(bucket)
            }
        }
    }

    override fun addBucket(nTasks: Int, onComplete: (bucket: UiBucket) -> Unit) {
        //Log.d("RoomBucketViewModel", "addBucket: $nTasks")
        scope.launch {
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
        scope.launch {
            bucketDao.updateBucket(bucket.toDbBucket())
        }
    }

    override fun deleteBucket(bucket: UiBucket) {
        scope.launch {
            bucketDao.deleteBucket(bucket.toDbBucket())
        }
    }

    override fun deleteBuckets(bucketIds: List<Int>) {
        scope.launch {
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